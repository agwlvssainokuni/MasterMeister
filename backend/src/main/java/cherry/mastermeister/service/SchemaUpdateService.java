/*
 * Copyright 2025 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cherry.mastermeister.service;

import cherry.mastermeister.entity.SchemaUpdateLogEntity;
import cherry.mastermeister.enums.SchemaUpdateOperation;
import cherry.mastermeister.model.SchemaMetadata;
import cherry.mastermeister.model.SchemaUpdateLog;
import cherry.mastermeister.repository.SchemaUpdateLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class SchemaUpdateService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SchemaReaderService schemaReaderService;
    private final SchemaUpdateLogRepository schemaUpdateLogRepository;
    private final AuditLogService auditLogService;

    public SchemaUpdateService(
            SchemaReaderService schemaReaderService,
            SchemaUpdateLogRepository schemaUpdateLogRepository,
            AuditLogService auditLogService
    ) {
        this.schemaReaderService = schemaReaderService;
        this.schemaUpdateLogRepository = schemaUpdateLogRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public SchemaMetadata executeSchemaRead(Long connectionId) {
        return executeWithLogging(connectionId, SchemaUpdateOperation.READ_SCHEMA,
                () -> schemaReaderService.readSchema(connectionId));
    }

    @Transactional
    public SchemaMetadata executeSchemaRefresh(Long connectionId) {
        return executeWithLogging(connectionId, SchemaUpdateOperation.REFRESH_SCHEMA,
                () -> schemaReaderService.readAndRefreshSchema(connectionId));
    }

    @Transactional
    public Optional<SchemaMetadata> getStoredSchema(Long connectionId) {
        // This doesn't need logging as it's just a read operation
        return schemaReaderService.getStoredSchemaMetadata(connectionId);
    }

    @Transactional(readOnly = true)
    public List<SchemaUpdateLog> getConnectionOperationHistory(Long connectionId) {
        logger.debug("Retrieving operation history for connection ID: {}", connectionId);

        return schemaUpdateLogRepository.findByConnectionIdOrderByCreatedAtDesc(connectionId)
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<SchemaUpdateLog> getConnectionOperationHistory(Long connectionId, Pageable pageable) {
        logger.debug("Retrieving paginated operation history for connection ID: {}", connectionId);

        return schemaUpdateLogRepository.findByConnectionIdOrderByCreatedAtDesc(connectionId, pageable)
                .map(this::toModel);
    }

    @Transactional(readOnly = true)
    public List<SchemaUpdateLog> getFailedOperations(Long connectionId) {
        logger.debug("Retrieving failed operations for connection ID: {}", connectionId);

        return schemaUpdateLogRepository.findFailedOperationsByConnection(connectionId)
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getSuccessfulOperationsCount(Long connectionId) {
        return schemaUpdateLogRepository.countSuccessfulOperationsByConnection(connectionId);
    }

    @Transactional(readOnly = true)
    public Page<SchemaUpdateLog> getUserOperationHistory(String userEmail, Pageable pageable) {
        logger.debug("Retrieving operation history for user: {}", userEmail);

        return schemaUpdateLogRepository.findByUserEmailOrderByCreatedAtDesc(userEmail, pageable)
                .map(this::toModel);
    }

    private <T> T executeWithLogging(Long connectionId, SchemaUpdateOperation operation, Supplier<T> schemaOperation) {
        String userEmail = getCurrentUserEmail();
        LocalDateTime startTime = LocalDateTime.now();
        long startTimeMs = System.currentTimeMillis();

        SchemaUpdateLogEntity logEntity = new SchemaUpdateLogEntity();
        logEntity.setConnectionId(connectionId);
        logEntity.setOperation(operation);
        logEntity.setUserEmail(userEmail);
        logEntity.setCreatedAt(startTime);

        try {
            logger.info("Executing {} for connection ID: {} by user: {}", operation, connectionId, userEmail);

            T result = schemaOperation.get();
            long executionTime = System.currentTimeMillis() - startTimeMs;

            // Log success
            logEntity.setExecutionTimeMs(executionTime);
            logEntity.setSuccess(true);

            if (result instanceof SchemaMetadata metadata) {
                logEntity.setTablesCount(metadata.tables().size());
                logEntity.setColumnsCount(metadata.tables().stream()
                        .mapToInt(table -> table.columns().size())
                        .sum());
                logEntity.setDetails(String.format("Successfully processed %d schemas, %d tables, %d columns",
                        metadata.schemas().size(), metadata.tables().size(),
                        metadata.tables().stream().mapToInt(table -> table.columns().size()).sum()));
            }

            schemaUpdateLogRepository.save(logEntity);
            logger.info("Successfully executed {} for connection ID: {} in {}ms",
                    operation, connectionId, executionTime);

            // Log admin action
            auditLogService.logSchemaOperation(userEmail, operation.name(), connectionId, true, 
                    logEntity.getDetails(), null);

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTimeMs;

            // Log failure
            logEntity.setExecutionTimeMs(executionTime);
            logEntity.setSuccess(false);
            logEntity.setErrorMessage(e.getMessage());
            logEntity.setDetails("Operation failed: " + e.getClass().getSimpleName());

            schemaUpdateLogRepository.save(logEntity);
            logger.error("Failed to execute {} for connection ID: {} in {}ms",
                    operation, connectionId, executionTime, e);

            // Log admin action failure
            auditLogService.logSchemaOperation(userEmail, operation.name(), connectionId, false, 
                    logEntity.getDetails(), e.getMessage());

            throw e;
        }
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        return "system";
    }

    private SchemaUpdateLog toModel(SchemaUpdateLogEntity entity) {
        return new SchemaUpdateLog(
                entity.getId(),
                entity.getConnectionId(),
                entity.getOperation(),
                entity.getUserEmail(),
                entity.getExecutionTimeMs(),
                entity.getSuccess(),
                entity.getErrorMessage(),
                entity.getTablesCount(),
                entity.getColumnsCount(),
                entity.getDetails(),
                entity.getCreatedAt()
        );
    }
}
