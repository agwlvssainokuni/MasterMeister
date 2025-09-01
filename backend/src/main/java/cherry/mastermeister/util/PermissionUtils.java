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

package cherry.mastermeister.util;

import cherry.mastermeister.enums.PermissionType;
import cherry.mastermeister.exception.PermissionDeniedException;
import cherry.mastermeister.model.PermissionCheckResult;
import cherry.mastermeister.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Component
public class PermissionUtils {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PermissionService permissionService;

    public PermissionUtils(
            PermissionService permissionService
    ) {
        this.permissionService = permissionService;
    }

    /**
     * Check permission and throw exception if denied
     */
    public void requirePermission(
            Long connectionId,
            PermissionType permissionType,
            String schemaName,
            String tableName,
            String columnName
    ) {
        PermissionCheckResult result = permissionService.checkPermission(
                connectionId, permissionType,
                schemaName, tableName, columnName
        );

        if (!result.granted()) {
            throw new PermissionDeniedException("Access denied", result);
        }
    }

    /**
     * Check connection-level permission
     */
    public void requireConnectionPermission(
            Long connectionId,
            PermissionType permissionType
    ) {
        requirePermission(
                connectionId, permissionType,
                null, null, null
        );
    }

    /**
     * Check schema-level permission
     */
    public void requireSchemaPermission(
            Long connectionId, PermissionType permissionType,
            String schemaName
    ) {
        requirePermission(
                connectionId, permissionType,
                schemaName, null, null
        );
    }

    /**
     * Check table-level permission
     */
    public void requireTablePermission(
            Long connectionId, PermissionType permissionType,
            String schemaName, String tableName
    ) {
        requirePermission(
                connectionId, permissionType,
                schemaName, tableName, null
        );
    }

    /**
     * Check column-level permission
     */
    public void requireColumnPermission(
            Long connectionId, PermissionType permissionType,
            String schemaName, String tableName, String columnName
    ) {
        requirePermission(
                connectionId, permissionType,
                schemaName, tableName, columnName
        );
    }

    /**
     * Check if user has permission without throwing exception
     */
    public boolean hasPermission(
            Long connectionId, PermissionType permissionType,
            String schemaName, String tableName, String columnName
    ) {
        PermissionCheckResult result = permissionService.checkPermission(
                connectionId, permissionType, schemaName, tableName, columnName);
        return result.granted();
    }

    /**
     * Check connection permission
     */
    public boolean hasConnectionPermission(Long connectionId, PermissionType permissionType) {
        return hasPermission(connectionId, permissionType, null, null, null);
    }

    /**
     * Check schema permission
     */
    public boolean hasSchemaPermission(Long connectionId, PermissionType permissionType, String schemaName) {
        return hasPermission(connectionId, permissionType, schemaName, null, null);
    }

    /**
     * Check table permission
     */
    public boolean hasTablePermission(Long connectionId, PermissionType permissionType,
                                      String schemaName, String tableName) {
        return hasPermission(connectionId, permissionType, schemaName, tableName, null);
    }

    /**
     * Check column permission
     */
    public boolean hasColumnPermission(Long connectionId, PermissionType permissionType,
                                       String schemaName, String tableName, String columnName) {
        return hasPermission(connectionId, permissionType, schemaName, tableName, columnName);
    }

    /**
     * Execute operation only if permission is granted
     */
    public <T> T executeIfPermitted(Long connectionId, PermissionType permissionType,
                                    String schemaName, String tableName, String columnName,
                                    Supplier<T> operation) {
        requirePermission(connectionId, permissionType, schemaName, tableName, columnName);
        return operation.get();
    }

    /**
     * Execute operation only if permission is granted, return null otherwise
     */
    public <T> T executeIfPermittedOrNull(Long connectionId, PermissionType permissionType,
                                          String schemaName, String tableName, String columnName,
                                          Supplier<T> operation) {
        if (hasPermission(connectionId, permissionType, schemaName, tableName, columnName)) {
            return operation.get();
        }
        return null;
    }

    /**
     * Filter list based on permission check
     */
    public <T> List<T> filterByPermission(List<T> items, Long connectionId, PermissionType permissionType,
                                          PermissionItemExtractor<T> extractor) {
        return items.stream()
                .filter(item -> {
                    try {
                        String schemaName = extractor.getSchemaName(item);
                        String tableName = extractor.getTableName(item);
                        String columnName = extractor.getColumnName(item);
                        return hasPermission(connectionId, permissionType, schemaName, tableName, columnName);
                    } catch (Exception e) {
                        logger.debug("Error checking permission for item, excluding: {}", e.getMessage());
                        return false;
                    }
                })
                .toList();
    }

    /**
     * Filter list by table permission
     */
    public <T> List<T> filterByTablePermission(List<T> tables, Long connectionId, PermissionType permissionType,
                                               TableExtractor<T> extractor) {
        return tables.stream()
                .filter(table -> {
                    try {
                        String schemaName = extractor.getSchemaName(table);
                        String tableName = extractor.getTableName(table);
                        return hasTablePermission(connectionId, permissionType, schemaName, tableName);
                    } catch (Exception e) {
                        logger.debug("Error checking table permission, excluding: {}", e.getMessage());
                        return false;
                    }
                })
                .toList();
    }

    /**
     * Check if current user is admin
     */
    public boolean isCurrentUserAdmin() {
        return permissionService.isCurrentUserAdmin();
    }

    /**
     * Execute operation only if user is admin
     */
    public <T> T executeAsAdmin(Supplier<T> operation) {
        if (!isCurrentUserAdmin()) {
            throw new PermissionDeniedException("Administrator access required", null);
        }
        return operation.get();
    }

    /**
     * Interface for extracting permission-relevant fields from objects
     */
    public interface PermissionItemExtractor<T> {
        default String getSchemaName(T item) {
            return null;
        }

        default String getTableName(T item) {
            return null;
        }

        default String getColumnName(T item) {
            return null;
        }
    }

    /**
     * Interface for extracting table information from objects
     */
    public interface TableExtractor<T> {
        String getSchemaName(T table);

        String getTableName(T table);
    }
}
