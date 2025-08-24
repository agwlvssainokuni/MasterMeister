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

import cherry.mastermeister.entity.AuditLogEntity;
import cherry.mastermeister.entity.UserEntity;
import cherry.mastermeister.repository.AuditLogRepository;
import cherry.mastermeister.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository
    ) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Log successful login - Uses REQUIRES_NEW to ensure logging regardless of subsequent operations
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginSuccess(String username) {
        AuditLogEntity auditLog = createBaseAuditLogEntity(username, "LOGIN", "AUTH");
        auditLog.setSuccess(true);
        auditLog.setDetails("User successfully logged in");
        auditLogRepository.save(auditLog);
    }

    /**
     * Log failed login - Uses REQUIRES_NEW to ensure security events are always recorded
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginFailure(String username, String errorMessage) {
        AuditLogEntity auditLog = createBaseAuditLogEntity(username, "LOGIN", "AUTH");
        auditLog.setSuccess(false);
        auditLog.setErrorMessage(errorMessage);
        auditLog.setDetails("Login attempt failed");
        auditLogRepository.save(auditLog);
    }

    /**
     * Log logout - Uses REQUIRES_NEW to ensure logout is always recorded
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLogout(String username) {
        AuditLogEntity auditLog = createBaseAuditLogEntity(username, "LOGOUT", "AUTH");
        auditLog.setSuccess(true);
        auditLog.setDetails("User successfully logged out");
        auditLogRepository.save(auditLog);
    }

    /**
     * Log token refresh success - Uses REQUIRES_NEW for security audit trail
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTokenRefresh(String username) {
        AuditLogEntity auditLog = createBaseAuditLogEntity(username, "TOKEN_REFRESH", "AUTH");
        auditLog.setSuccess(true);
        auditLog.setDetails("Access token refreshed successfully");
        auditLogRepository.save(auditLog);
    }

    /**
     * Log token refresh failure - Uses REQUIRES_NEW to ensure security events are recorded
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTokenRefreshFailure(String username, String errorMessage) {
        AuditLogEntity auditLog = createBaseAuditLogEntity(username, "TOKEN_REFRESH", "AUTH");
        auditLog.setSuccess(false);
        auditLog.setErrorMessage(errorMessage);
        auditLog.setDetails("Token refresh attempt failed");
        auditLogRepository.save(auditLog);
    }

    /**
     * Log admin actions - Uses REQUIRES_NEW to ensure admin actions are always audited
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAdminAction(String username, String action, String target, String details) {
        AuditLogEntity auditLog = createBaseAuditLogEntity(username, action, target);
        auditLog.setSuccess(true);
        auditLog.setDetails(details);
        auditLogRepository.save(auditLog);
    }

    /**
     * Log data access operation (for large datasets) - Read operation
     * Uses REQUIRED propagation as read operations typically don't have critical rollback concerns
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void logDataAccess(Long connectionId, String schemaName, String tableName,
                              int recordCount, long executionTimeMs) {
        String target = String.format("connection:%d table:%s.%s",
                connectionId, schemaName != null ? schemaName : "", tableName);

        String details = String.format("{\"connectionId\":%d,\"schema\":\"%s\",\"table\":\"%s\"," +
                        "\"recordCount\":%d,\"executionTimeMs\":%d}",
                connectionId, schemaName != null ? schemaName : "", tableName, recordCount, executionTimeMs);

        String username = getCurrentUsername();
        AuditLogEntity auditLog = createBaseAuditLogEntity(username, "DATA_ACCESS", target);
        auditLog.setSuccess(true);
        auditLog.setDetails(details);
        auditLogRepository.save(auditLog);
    }

    /**
     * Log schema operations - Uses REQUIRES_NEW for failures to ensure audit trail
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSchemaOperation(String username, String operation, Long connectionId, boolean success,
                                   String details, String errorMessage) {
        String target = "SCHEMA_CONNECTION_" + connectionId;
        AuditLogEntity auditLog = createBaseAuditLogEntity(username, operation, target);
        auditLog.setSuccess(success);
        auditLog.setDetails(details);
        if (errorMessage != null) {
            auditLog.setErrorMessage(errorMessage);
        }
        auditLogRepository.save(auditLog);
    }

    /**
     * Log data modification operations (CREATE, UPDATE, DELETE) - Success case
     * Uses REQUIRED propagation to participate in main transaction
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void logDataModification(Long connectionId, String schemaName, String tableName,
                                    String operation, int recordCount, long executionTimeMs) {
        String username = getCurrentUsername();
        String target = String.format("connection:%d, table:%s.%s", connectionId, schemaName, tableName);

        AuditLogEntity auditLog = createBaseAuditLogEntity(username, "DATA_" + operation, target);
        auditLog.setSuccess(true);
        auditLog.setDetails(String.format("Records %s: %d, Execution time: %dms",
                operation.toLowerCase(), recordCount, executionTimeMs));

        auditLogRepository.save(auditLog);
    }

    /**
     * Log data modification operations with detailed information - Success case
     * Uses REQUIRED propagation to participate in main transaction
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void logDataModificationDetailed(Long connectionId, String schemaName, String tableName,
                                           String operation, int recordCount, long executionTimeMs,
                                           String query, String additionalDetails) {
        String username = getCurrentUsername();
        String target = String.format("connection:%d, table:%s.%s", connectionId, schemaName, tableName);

        String details = String.format("{\"operation\":\"%s\",\"recordCount\":%d,\"executionTimeMs\":%d," +
                        "\"query\":\"%s\",\"additionalDetails\":\"%s\"}",
                operation, recordCount, executionTimeMs, 
                query.replace("\"", "\\\""), additionalDetails != null ? additionalDetails : "");

        AuditLogEntity auditLog = createBaseAuditLogEntity(username, "DATA_" + operation + "_DETAILED", target);
        auditLog.setSuccess(true);
        auditLog.setDetails(details);

        auditLogRepository.save(auditLog);
    }

    /**
     * Log data modification failure - Failure case
     * Uses REQUIRES_NEW propagation to ensure failure logs are always recorded
     * even when main transaction is rolled back
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDataModificationFailure(Long connectionId, String schemaName, String tableName,
                                          String operation, String errorMessage, long executionTimeMs,
                                          String query) {
        String username = getCurrentUsername();
        String target = String.format("connection:%d, table:%s.%s", connectionId, schemaName, tableName);

        String details = String.format("{\"operation\":\"%s\",\"executionTimeMs\":%d," +
                        "\"query\":\"%s\",\"error\":\"%s\"}",
                operation, executionTimeMs, 
                query != null ? query.replace("\"", "\\\"") : "", errorMessage.replace("\"", "\\\""));

        AuditLogEntity auditLog = createBaseAuditLogEntity(username, "DATA_" + operation + "_FAILED", target);
        auditLog.setSuccess(false);
        auditLog.setErrorMessage(errorMessage);
        auditLog.setDetails(details);

        auditLogRepository.save(auditLog);
    }

    /**
     * Log permission check events - Critical for security audit
     * Uses REQUIRES_NEW propagation to ensure all permission checks are recorded
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPermissionCheck(Long connectionId, String schemaName, String tableName, 
                                  String columnName, String permissionType, boolean granted,
                                  String reason) {
        String username = getCurrentUsername();
        String target = String.format("connection:%d", connectionId);
        if (schemaName != null) target += ", schema:" + schemaName;
        if (tableName != null) target += ", table:" + tableName;
        if (columnName != null) target += ", column:" + columnName;

        String details = String.format("{\"permissionType\":\"%s\",\"granted\":%b,\"reason\":\"%s\"}",
                permissionType, granted, reason != null ? reason : "");

        AuditLogEntity auditLog = createBaseAuditLogEntity(username, "PERMISSION_CHECK", target);
        auditLog.setSuccess(granted);
        if (!granted) {
            auditLog.setErrorMessage("Permission denied: " + reason);
        }
        auditLog.setDetails(details);

        auditLogRepository.save(auditLog);
    }

    /**
     * Log bulk operation events - Mixed success/failure case
     * Uses REQUIRES_NEW when there are failures to ensure logging is preserved
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBulkOperation(Long connectionId, String schemaName, String tableName,
                                String operation, int totalRecords, int successCount, 
                                int failureCount, long executionTimeMs, String summary) {
        String username = getCurrentUsername();
        String target = String.format("connection:%d, table:%s.%s", connectionId, schemaName, tableName);

        String details = String.format("{\"operation\":\"%s\",\"totalRecords\":%d," +
                        "\"successCount\":%d,\"failureCount\":%d,\"executionTimeMs\":%d,\"summary\":\"%s\"}",
                operation, totalRecords, successCount, failureCount, executionTimeMs, 
                summary != null ? summary.replace("\"", "\\\"") : "");

        AuditLogEntity auditLog = createBaseAuditLogEntity(username, "DATA_BULK_" + operation, target);
        auditLog.setSuccess(failureCount == 0);
        if (failureCount > 0) {
            auditLog.setErrorMessage(String.format("%d out of %d records failed", failureCount, totalRecords));
        }
        auditLog.setDetails(details);

        auditLogRepository.save(auditLog);
    }

    private AuditLogEntity createBaseAuditLogEntity(String username, String action, String target) {
        HttpServletRequest request = getCurrentRequest();
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setTarget(target);
        if (request != null) {
            auditLog.setIpAddress(extractClientIpAddress(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
        }
        return auditLog;
    }

    private HttpServletRequest getCurrentRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest).orElse(null);
    }

    private String extractClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-Forの最初のIPアドレスを取得
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Get current username from security context
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            String userUuid = authentication.getName(); // This is the UUID from JWT sub claim
            return userRepository.findByUserUuid(userUuid)
                    .map(UserEntity::getEmail)
                    .orElse("unknown-user");
        }
        return "system";
    }
}
