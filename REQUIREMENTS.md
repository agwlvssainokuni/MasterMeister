# Master Data Maintenance Application - Requirements Specification

## Project Overview
This project aims to develop an application for maintaining master data stored in an RDBMS. The application will be a Single Page Application (SPA) web application with Spring Boot 3.5 backend and React 19 frontend.

## Technical Stack
- **Java**: Version 21 (latest LTS, migrate when next LTS is released)
- **Node.js**: Version 22 (latest LTS, migrate when next LTS is released)  
- **Backend**: Spring Boot 3.5
- **Frontend**: React 19
- **Database Access**: JPA for internal DB access
- **Connection Pooling**: Use DB connection pool instead of DriverManager.getConnection() for target RDBMS
- **Date/Time Handling**: Use java.time API for target RDBMS date/time data

## Database Architecture
- **Internal DB**: Separate database for application operational data
- **Target RDBMS**: The database containing master data to be maintained
- **Connection Pool**: Configure pool parameters as part of DB connection settings

## Application Workflow

### 1. User Registration
- Users submit registration applications
- Confirmation email sent to users
- Registration applications displayed in admin dashboard
- Administrators approve or reject applications
- Approval/rejection result emails sent to users
- Approved users can log in

### 2. Target RDBMS Setup
- Administrators input RDBMS connection information in admin dashboard
- Schema update operation connects to target RDBMS and reads table/view structure (physical names, comments, types, constraints) into internal DB
- Administrators configure access permissions based on target RDBMS structure
- **Table-level permissions**: Allow (show) or Deny (hide)
- **Column-level permissions**: 
  - No access
  - Read-only (R)
  - Read/Update (RU) for existing records
  - Full access (CRUD) when all columns have update permission
- Access permission settings can be exported/imported in YAML format

### 3. User Authentication
- Users (including administrators) log in to access functionality

### 4. Master Maintenance Features
- **Table/View Listing**: Display accessible tables/views on top screen
- **Record Listing**: Show paginated record list when table/view selected
- **Filtering**: Intuitive UI for filter conditions and sorting (only columns with read+ permission)
- **SQL Input**: Manual WHERE and ORDER BY clause input (permission-independent)
- **Record Editing**: Modify values for columns with update permission on the record list screen
- **Transaction Management**: Apply changes button triggers backend API call as single transaction
- **Record Operations**: Create/delete records when full permissions available
- **Unified API**: Single API endpoint handles all create/update/delete operations as one transaction

### 5. Query Builder Features
- **Tab Interface**: SELECT, FROM, JOIN, WHERE, GROUP BY, HAVING, ORDER BY, LIMIT OFFSET tabs
- **Table Selection**: FROM/JOIN tabs specify tables and optional aliases
- **Column Selection**: Other tabs show accessible columns for selected tables/aliases
- **Aggregate Functions**: Available in SELECT, HAVING, ORDER BY tabs
- **SQL Generation**: Generate SQL based on UI specifications
- **Integration**: Save generated SQL or execute directly
- **Reverse Engineering**: Parse SQL to populate query builder tabs when transitioning from other features

### 6. Query Save Features
- **Named Storage**: Save reusable SQL with custom names (manual input or query builder)
- **Visibility Scopes**: Public or private query storage
- **Query Execution**: Users can execute saved queries
- **Modification Rights**: Query creators can modify their saved queries

### 7. Query Execution Features
- **SQL Input/Editing**: Enter and modify SQL in execution screen
- **Restriction**: Read-only SQL execution only
- **Parameter Support**: Use ":param" format for parameterized queries with NamedParameterJdbcTemplate
- **Parameter Input**: UI for setting parameter values with automatic parameter detection
- **Pagination Option**: Configurable pagination for query execution
- **History Logging**: Record SQL, parameters, result count, execution time, execution count (for saved queries), execution datetime, and executor
- **Result Display**: Tabular format with pagination as needed
- **Saved Query Restriction**: No modification allowed when executing saved queries

### 8. Query History Features
- **History Listing**: Paginated display of query execution history
- **Query Type Indication**: Distinguish between saved queries and direct input
- **Filtering Options**:
  - Execution datetime (from/to range)
  - Executor (all users or self only)  
  - SQL text search
- **Navigation**: Transition to query execution, query save, or query builder features from history

## Key Technical Requirements
- Use connection pooling for target RDBMS access
- Implement proper transaction management
- Support parameterized queries with NamedParameterJdbcTemplate
- Provide comprehensive access control at table and column levels
- Enable YAML-based configuration import/export
- Implement proper audit logging for all operations
