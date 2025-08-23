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
- Two-step email-first registration flow:
  1. Users submit email addresses to initiate registration
  2. Registration confirmation email with time-limited link sent to new users only
  3. Users complete registration by setting password through email link
  4. Completed registrations displayed in admin dashboard
- Administrator approval workflow:
  - Administrators approve or reject completed registrations
  - Approval/rejection result emails sent to users
  - Only approved users can log in
- Security features:
  - Email enumeration prevention (consistent API responses regardless of user existence)
  - Configurable registration token expiry (default: 3 hours)
  - Separate token management system for registration process
- Configuration:
  - Token expiry period configurable via `mm.app.user-registration.token-expiry-hours`
  - Registration path and token parameter customizable

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

## Technical Stack Specifications

### Build Tools and Frameworks
- **Build Tool**: Gradle 9.0 (latest version)
- **Internal Database**: H2 Database for application operational data
- **Frontend Build Tool**: Vite for React development and building

### Supported Target RDBMS
The application must support the following database systems as maintenance targets:
- MySQL
- MariaDB  
- PostgreSQL
- H2 Database

### Development Team and Timeline
- **Team Size**: Single developer (initial development)
- **Development Approach**: MVP-first with incremental feature delivery
- **Priority**: Workflow upstream features (user management → DB setup → access control → data display)

### Deployment Requirements

#### Primary Deployment Target
- **12 Factor App Compliance**: Application must follow 12-factor app principles
- **Executable WAR**: Generate self-contained executable WAR files
- **Configuration**: All configuration via environment variables
- **Logging**: Structured logging suitable for containerized environments

#### Additional Deployment Support
- **Container Support**: Docker containerization from executable WAR
- **Traditional Deployment**: Tomcat WAR deployment capability (future enhancement)

### Performance and Scale Requirements
- **Concurrent Users**: Designed for approximately 10 simultaneous users
- **Large Dataset Threshold**: Configurable threshold for audit logging (default: 100+ records)
- **Response Time**: Reasonable performance for typical master data operations

### Project Structure Requirements
```
MasterMeister/
├── backend/        # Spring Boot application
├── frontend/       # React application with Vite
└── devenv/         # Development environment (Docker Compose)
```

### Development Environment Specifications

#### Development Services (Docker Compose in devenv/)
- **Mail Server**: MailPit for development email testing
- **Development Databases**: 
  - MySQL container for development/testing
  - MariaDB container for development/testing  
  - PostgreSQL container for development/testing
  - H2 (embedded, no container needed)

#### Email Integration
- **Development**: MailPit container for email capture and testing
- **Production**: Configurable SMTP settings via environment variables

## Enhanced Audit Logging Requirements

### Mandatory Audit Events
- **Authentication Events**:
  - User login (timestamp, username, IP address)
  - User logout (timestamp, username, session duration)
  - Failed login attempts (timestamp, attempted username, IP address)

- **Administrative Operations**:
  - Account registration applications
  - Account approval/rejection by administrators
  - Target RDBMS connection configuration changes
  - Schema import operations and results
  - Access permission configuration changes (table/column level)

- **Data Access Events**:
  - Large dataset queries (configurable threshold, default 100+ records)
  - Query execution with result count and execution time
  - Data modification operations (insert/update/delete)

### Audit Log Requirements
- **Format**: Structured logging (JSON preferred)
- **Storage**: Persistent storage in internal database
- **Fields**: Timestamp (ISO 8601), user ID, action type, target resource, IP address, result status
- **Retention**: Configurable retention period
- **Access**: Admin-only access to audit logs

### Security and Compliance
- **No Special Requirements**: Standard web application security practices
- **Data Protection**: Basic protection for sensitive data in logs
- **Access Control**: Audit logs accessible only to administrators
