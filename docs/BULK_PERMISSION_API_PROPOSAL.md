# Bulk Permission API Implementation Proposal

## Overview
This document proposes a backend API implementation for the bulk permission feature to reduce the setup barrier for permission management.

## Current Status
- ✅ Frontend UI implemented with Quick Setup tab
- ✅ Type definitions and service layer ready
- ❌ Backend API not yet implemented
- ❌ Frontend displays "not available" message

## Proposed API Endpoint

### Bulk Grant Permissions
```
POST /api/admin/permissions/{connectionId}/bulk-grant
```

#### Request Body
```json
{
  "scope": "ALL_TABLES" | "SCHEMA" | "TABLE_LIST",
  "permissionType": "READ" | "WRITE" | "DELETE", 
  "userEmails": ["user1@example.com", "user2@example.com"],
  "schemaNames": ["schema1", "schema2"],
  "tableNames": ["table1", "table2"],
  "includeSystemTables": false,
  "description": "Bulk permission setup for development team"
}
```

#### Response Body
```json
{
  "ok": true,
  "data": {
    "processedUsers": 5,
    "processedTables": 15,
    "createdPermissions": 75,
    "skippedExisting": 3,
    "errors": []
  }
}
```

## Implementation Requirements

### 1. Controller Addition
Add to `PermissionExportController.java`:

```java
@PostMapping("/{connectionId}/bulk-grant")
@Operation(summary = "Bulk grant permissions", description = "Grant permissions to multiple users across multiple tables")
public ApiResponse<BulkPermissionResult> bulkGrantPermissions(
        @PathVariable Long connectionId,
        @RequestBody @Valid BulkPermissionRequest request
) {
    logger.info("Bulk granting {} permissions for connection ID: {}", 
                request.getPermissionType(), connectionId);

    BulkPermissionResult result = permissionBulkService.grantBulkPermissions(connectionId, request);
    
    logger.info("Bulk grant completed: {} users, {} tables, {} permissions created", 
                result.getProcessedUsers(), result.getProcessedTables(), result.getCreatedPermissions());
    
    return ApiResponse.success(result);
}
```

### 2. Service Layer
Create `PermissionBulkService.java`:

```java
@Service
@Transactional
public class PermissionBulkService {
    
    public BulkPermissionResult grantBulkPermissions(Long connectionId, BulkPermissionRequest request) {
        // 1. Validate connection exists and is active
        // 2. Get target users (all active if userEmails empty)
        // 3. Get target tables based on scope
        // 4. Create permissions with audit logging
        // 5. Handle duplicates based on skipExisting flag
        // 6. Return comprehensive result
    }
    
    private List<User> getTargetUsers(List<String> userEmails) {
        if (userEmails.isEmpty()) {
            return userRepository.findByStatus(UserStatus.APPROVED);
        }
        return userRepository.findByEmailIn(userEmails);
    }
    
    private List<TableMetadata> getTargetTables(Long connectionId, BulkPermissionRequest request) {
        switch (request.getScope()) {
            case ALL_TABLES:
                return schemaService.getAllTables(connectionId, request.isIncludeSystemTables());
            case SCHEMA:
                return schemaService.getTablesBySchemas(connectionId, request.getSchemaNames());
            case TABLE_LIST:
                return schemaService.getTablesByNames(connectionId, request.getTableNames());
        }
    }
}
```

### 3. DTO Classes
Create request/response DTOs:

```java
public record BulkPermissionRequest(
    BulkPermissionScope scope,
    PermissionType permissionType,
    List<String> userEmails,
    List<String> schemaNames,
    List<String> tableNames,
    boolean includeSystemTables,
    String description
) {}

public record BulkPermissionResult(
    int processedUsers,
    int processedTables, 
    int createdPermissions,
    int skippedExisting,
    List<String> errors
) {}

public enum BulkPermissionScope {
    ALL_TABLES, SCHEMA, TABLE_LIST
}
```

## Security Considerations

### 1. Authorization
- Require ADMIN role for bulk operations
- Add additional confirmation for WRITE/DELETE permissions
- Log all bulk operations with user details

### 2. Rate Limiting
- Implement reasonable limits on bulk operations
- Consider async processing for large bulk operations

### 3. Audit Trail
- Comprehensive logging of all created permissions
- Include bulk operation metadata in audit logs
- Track who performed bulk operations

## Testing Strategy

### 1. Unit Tests
- Test permission creation logic
- Test scope resolution (ALL_TABLES, SCHEMA, TABLE_LIST)
- Test duplicate handling
- Test error scenarios

### 2. Integration Tests  
- Test full bulk grant flow
- Test with different database types
- Test permission validation after bulk grant

### 3. Performance Tests
- Test bulk operations with large user/table counts
- Verify transaction performance
- Test concurrent bulk operations

## Implementation Priority

### High Priority (Immediate)
1. Basic ALL_TABLES READ permission bulk grant
2. User selection (all active users vs. specific emails)
3. Audit logging integration

### Medium Priority (Next iteration)
4. SCHEMA and TABLE_LIST scope support
5. Advanced error handling and rollback
6. Performance optimizations

### Low Priority (Future)
7. Async bulk processing for large operations
8. Email notifications for bulk operations
9. Bulk permission templates

## Frontend Integration

The frontend is already prepared to consume this API:
- `permissionService.ts` has placeholder bulk grant method
- UI shows "not available" message until API is implemented
- All error handling and success flows are ready

## Migration Path

1. Implement basic bulk grant API
2. Update frontend service to enable bulk grant feature
3. Test with development environment
4. Deploy incrementally with feature flags
5. Monitor usage and performance metrics

This implementation will significantly reduce the initial setup barrier while maintaining security and audit requirements.