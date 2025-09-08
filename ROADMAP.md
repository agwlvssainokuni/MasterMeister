# Master Data Maintenance Application - Development Roadmap

## Project Overview
Development roadmap for a Master Data Maintenance Single Page Application (SPA) with Spring Boot backend and React frontend. Designed for single-developer implementation with early MVP delivery.

## Current Status
- ✅ Requirements specification completed
- ✅ Project documentation established
- ✅ Development planning phase
- ✅ **Phase 1 COMPLETED** - Project structure setup (August 18, 2025)
- ✅ **Phase 2 COMPLETED** - Authentication & User Management (August 21, 2025)
- ✅ **Phase 3 COMPLETED** - Database Configuration System (August 24, 2025)
- 🔄 **Phase 4 NEAR COMPLETION** - Data Access & Display (September 8, 2025)
  - ✅ **Phase 4.1 & 4.2 COMPLETED** - Secure Data Access & Backend CRUD APIs
  - ✅ **Phase 4.3 COMPLETED** - Frontend Data Display with Advanced UI/UX
  - 🔄 **Phase 4.4 IN PROGRESS** - MVP Integration & Testing (E2E testing execution)

---

## Technical Stack Decisions

### Backend
- **Framework**: Spring Boot 3.5 with Java 21
- **Build Tool**: Gradle 9.0 (latest)
- **Internal DB**: H2 Database
- **Target RDBMS Support**: MySQL, MariaDB, PostgreSQL, H2
- **Deployment**: 12 Factor App compliant executable WAR

### Frontend
- **Framework**: React 19 with Node.js 22
- **Build Tool**: Vite
- **Packaging**: Static assets served by Spring Boot

### Development Environment
- **Structure**: `backend/`, `frontend/`, `devenv/`
- **Mail Server**: MailPit (containerized)
- **Development DBs**: MySQL, MariaDB, PostgreSQL, H2 (containerized)
- **Orchestration**: Docker Compose

---

## MVP Scope Definition

### Core MVP Features (Minimum Viable Product)
1. **User Management**: Registration → Admin Approval → Login
2. **RDBMS Configuration**: Connection setup and schema import
3. **Access Control**: Table/column-level permissions
4. **Data Display**: Table listing and basic record viewing

### MVP Success Criteria
- Single user can register, get approved, and login
- Admin can configure one target RDBMS connection
- Schema structure is imported and permissions can be set
- Users can view accessible tables and records with basic filtering

---

## Development Phases

### Phase 1: Project Foundation (Weeks 1-2)

#### 1.1 Project Structure Setup ✅ COMPLETED
- [x] Create multi-module project structure (`backend/`, `frontend/`, `devenv/`)
- [x] Initialize Spring Boot 3.5.4 with Gradle 9.0
- [x] Configure Java 21 and Spring Boot Gradle plugin
- [x] Initialize React 19 with Vite in `frontend/`
- [x] Setup development Docker Compose in `devenv/`

#### 1.2 Development Environment ✅ COMPLETED
- [x] Configure MailPit container for email testing
- [x] Setup MySQL, MariaDB, PostgreSQL containers
- [x] Configure Spring profiles (default/dev/stg/prd) with flexible logging output

#### 1.3 Basic Application Structure ✅ COMPLETED
- [x] Create Spring Boot main application with H2 configuration
- [x] Setup basic package structure for JPA entities
- [x] Configure copyright headers and license information
- [x] Create flexible logging configuration with logback-spring.xml

#### 1.4 Remaining Tasks ✅ COMPLETED
- [x] Create development database initialization scripts
- [x] Setup basic JPA entities for internal database
- [x] Configure Vite build integration with Spring Boot
- [x] Create basic REST API structure and CORS configuration

#### 1.5 Code Quality & Infrastructure ✅ COMPLETED
- [x] Remove lombok dependency and implement standard Java POJO
- [x] Add commons-lang3 for Builder classes (equals, hashCode, toString)
- [x] Implement TraceAspect for method execution logging
- [x] Add AOP support with configurable trace settings
- [x] Setup API unified response format with testing

**Milestone 1**: ✅ COMPLETED - Full project foundation with quality infrastructure ready

---

### Phase 2: Authentication & User Management (Weeks 3-4)

#### 2.1 Core Authentication ✅ COMPLETED
- [x] Implement User entity and UserRepository
- [x] Create Spring Security configuration with JWT
- [x] Build user registration API with email validation
- [x] Integrate MailPit for confirmation emails
- [x] Infrastructure Enhancements: Admin initialization + Production readiness
  - [x] AdminUserInitializer with configurable settings via application.properties
  - [x] Health endpoint test improvements (removed unnecessary @WithMockUser)
  - [x] Security service isolation verification for public endpoints
  - [x] Swagger/OpenAPI 3 integration with JWT Bearer authentication
  - [x] Docker containerization with Eclipse Temurin 21-jre
  - [x] Dependency management organization with Spring Boot BOM
  - [x] Environment-specific H2 file databases with profile-based naming
  - [x] Profile-specific logging configuration for Hibernate SQL

#### 2.2 Admin Approval Workflow (Backend) ✅ COMPLETED
- [x] Backend API Implementation
  - [x] AdminController with ADMIN role authorization
  - [x] GET /api/admin/users/pending - pending user list
  - [x] POST /api/admin/users/{id}/approve - user approval
  - [x] POST /api/admin/users/{id}/reject - user rejection
  - [x] UserService with approval/rejection logic
  - [x] Email notifications for approval/rejection results
  - [x] Model package restructuring (UserStatus, UserRole, TemplateType enums)
  - [x] Comprehensive test coverage (AdminControllerTest, UserServiceTest)

#### 2.3 Admin Approval Workflow (Frontend) ✅ COMPLETED
- [x] Create admin dashboard UI for user approval
  - [x] AdminRoute component with ADMIN role authorization
  - [x] AdminDashboard with tabbed navigation (Users/Database/Permissions)
  - [x] Responsive admin-specific CSS classes and styling
- [x] Implement pending users list display
  - [x] PendingUsersList component with API integration
  - [x] Table display with user details (username, email, registration date)
  - [x] Loading states, error handling, and empty state UI
- [x] Add approve/reject button functionality
  - [x] Direct API integration for approve/reject actions
  - [x] Optimistic UI updates (user removal after action)
  - [x] Action loading states and error handling
- [x] Build confirmation dialogs for admin actions
  - [x] ConfirmDialog component with modal-based UX
  - [x] ESC key and overlay click dismissal
  - [x] Danger styling for reject actions
  - [x] Loading state integration
- [x] Enhanced UX Features
  - [x] Toast notification system (NotificationProvider/Context/Hook)
  - [x] Success/error notifications for admin actions
  - [x] Comprehensive responsive design improvements
  - [x] ESLint compliance and code quality

#### 2.4 Login/Logout System ✅ COMPLETED
- [x] Create login/logout endpoints
  - [x] JWT Token Rotation implementation with RefreshToken management
  - [x] POST /api/auth/logout endpoint with token revocation
  - [x] Enhanced refresh endpoint with Token Rotation security
  - [x] Access token: 5 minutes, Refresh token: 24 hours (configurable)
  - [x] LogoutRequest DTO and TokenPair model
- [x] Frontend infrastructure setup
  - [x] i18n internationalization system (react-i18next with Japanese/English resources)
  - [x] CSS design system: Variables, Form, Button, Table, Pagination, Tabs, Modal, Alert, Navigation, Card, Loading, AuthLayout
  - [x] Accessibility and responsive design implementation
- [x] Create login/logout form components
  - [x] LoginForm/LogoutButton/LoginPage components with callback pattern
  - [x] JWT role integration (Backend: ROLE_ prefix removal, Frontend: role claim parsing)
  - [x] Complete authentication flow verification (login → dashboard → logout)

#### 2.5 Basic Security & Logging ✅ COMPLETED
- [x] Create admin operation audit logging (AuditLog entity)
- [x] Setup basic error handling and validation (API response format)
- [x] Implement authentication logging (login/logout)
  - [x] AuditLogService for comprehensive event logging
  - [x] Login/logout/token refresh audit trails
  - [x] IP address and User-Agent tracking

#### 2.6 User Registration System Refactor ✅ COMPLETED
- [x] Email-first registration flow implementation
  - [x] Two-step registration process: email submission → password completion
  - [x] RegisterEmailPage with email input and confirmation messaging
  - [x] RegisterUserPage with token validation and password setup
  - [x] Security enhancement: email enumeration prevention
- [x] Backend 2-step registration architecture
  - [x] RegistrationTokenEntity and repository for token management
  - [x] UserRegistrationService refactor with registerEmail/registerUser methods
  - [x] Stream API validation with filter chaining for security
  - [x] Configurable token expiry (default: 3 hours via mm.app.user-registration.token-expiry-hours)
- [x] Frontend registration flow updates
  - [x] RegisterEmailForm component with email-only input
  - [x] URL routing: /register-email → /register with token parameter
  - [x] Session storage integration for email display in success messages
  - [x] Updated authService with registerEmail/registerUser methods
- [x] Email service and template updates
  - [x] REGISTER_EMAIL template for initial registration email
  - [x] REGISTER_USER template for completion notification
  - [x] Consolidated email template architecture
- [x] i18n localization updates
  - [x] registerEmail and register sections with step-specific messages
  - [x] Error handling messages for token validation and expiry
  - [x] Japanese/English bilingual support for new flow
- [x] Security and architecture improvements
  - [x] UserEntity email confirmation field removal (responsibility separation)
  - [x] Property naming standardization (mm.app.user-registration.* prefix)
  - [x] Comprehensive test coverage updates for new registration flow

**Milestone 2**: ✅ COMPLETED - MVP user management system operational

---

### Phase 3: Database Configuration System (Weeks 5-6)

#### 3.1 Multi-Database Connection Management ✅ COMPLETED
- [x] Create database configuration entity and management (DatabaseConnection entity)
- [x] Implement dynamic DataSource creation with connection pooling
  - [x] HikariCP integration with configurable pool settings
  - [x] Dynamic DataSource cache management with concurrent access
  - [x] Database-specific JDBC URL generation (MySQL, MariaDB, PostgreSQL, H2)
  - [x] In-memory H2 support for testing environments
- [x] Build database connection testing functionality
  - [x] DatabaseConnectionController with CRUD operations and testing API
  - [x] Connection validation with timeout and error handling
  - [x] Entity/Model/DTO 3-layer architecture implementation
  - [x] GlobalExceptionHandler integration with custom exceptions
- [x] Support MySQL, MariaDB, PostgreSQL, H2 connections (enum defined)
- [x] Backend Architecture & Responsibility Separation
  - [x] Entity/Model/DTO 3-layer strict separation implementation
  - [x] Package-level import restriction enforcement (Controller↛Entity, Service↛DTO, Repository↛Model/DTO)
  - [x] Controller → Service → Repository dependency flow with proper type conversion
  - [x] Dedicated enums package for cross-layer accessibility

#### 3.2 Schema Import & Metadata Management ✅ COMPLETED
- [x] Create schema reader for different database types
  - [x] DatabaseSchemaService with multi-database support (MySQL/MariaDB/PostgreSQL/H2)
  - [x] DatabaseMetaData integration for schema introspection
  - [x] SchemaInfo/TableInfo/ColumnInfo/IndexInfo model hierarchy
  - [x] DatabaseSchemaController with REST API endpoints
- [x] Implement table/column metadata storage
  - [x] SchemaInfoEntity/TableInfoEntity/ColumnInfoEntity JPA entities
  - [x] Proper relationship mapping (OneToMany/ManyToOne)
  - [x] Repository layer with JPA queries for schema operations
- [x] Build schema update operations
  - [x] Schema refresh functionality with transaction support
  - [x] Incremental update detection and processing
  - [x] Batch processing for large schema imports
- [x] Add admin logging for schema operations
  - [x] AuditLogService integration for all schema operations
  - [x] Performance tracking and execution time logging
  - [x] Error handling with detailed audit trails

#### 3.3 Access Control System ✅ COMPLETED
- [x] Design table/column permission entities
  - [x] UserPermissionEntity/PermissionTemplateEntity with hierarchical scopes
  - [x] PermissionScope enum (CONNECTION/SCHEMA/TABLE/COLUMN)
  - [x] PermissionType enum (READ/WRITE/DELETE/ADMIN)
  - [x] Proper JPA relationships and unique constraints
- [x] Implement basic permission authentication interface
  - [x] PermissionAuthService with hierarchical permission checking
  - [x] PermissionManagementService for CRUD operations
  - [x] PermissionTemplateService for template-based permissions
  - [x] Spring Security integration with current user context
- [x] Create permission enforcement utilities
  - [x] @RequirePermission annotation for declarative access control
  - [x] PermissionAspect (AOP) for automatic permission checking
  - [x] PermissionUtils for programmatic permission validation
  - [x] Three-layer enforcement: Annotation → Programmatic → SQL Analysis
- [x] Build YAML export/import foundation
  - [x] PermissionExportData record hierarchy for YAML serialization
  - [x] PermissionYamlService with Jackson YAML processing
  - [x] PermissionExportController for file upload/download operations
  - [x] Validation endpoints and comprehensive error handling

**Milestone 3**: ✅ COMPLETED - Database configuration and access control system operational

---

### Phase 4: Data Access & Display (Weeks 7-8)

#### 4.1 Secure Data Access ✅ COMPLETED
- [x] Implement permission-based table listing
- [x] Create secure record retrieval with column filtering
- [x] Build pagination and basic filtering
- [x] Add large dataset query logging (100+ records threshold)

#### 4.2 Data Modification (Backend API Implementation) ✅ COMPLETED
- [x] Implement record creation with permission validation (Backend API)
- [x] Add record update functionality with transaction management (Backend API)
- [x] Implement record deletion with referential integrity checks (Backend API)
- [x] Create comprehensive audit logging for all CRUD operations (Backend)
- [x] Build unified CRUD API with transaction management (Backend)
- [x] Add data validation and error handling (Backend validation)
- [x] Transaction propagation strategy (success: REQUIRED, failure: REQUIRES_NEW)
- [x] Comprehensive error handling with detailed audit logging

#### 4.3 Frontend Data Display ✅ COMPLETED
- [x] Create table/view listing interface (DatabaseTreeView with permission integration)
- [x] Build record listing with pagination (DataTableView with full pagination controls)
- [x] Implement advanced filtering UI (TableFilterBarView with inline input and unified apply)
- [x] Add sorting and search functionality (Column-based sorting with UI indicators)
- [x] UI/UX Enhancements
  - [x] Tabbed interface for data/metadata display
  - [x] Responsive table design with horizontal scrolling
  - [x] Fixed columns (actions, primary key) for wide tables
  - [x] Stripe effects and hover states for better readability
  - [x] Compact column type/constraint display with tooltips
  - [x] Unified styling across all table components

#### 4.4 MVP Integration & Testing 🔄 IN PROGRESS
- [x] End-to-end testing documentation (Complete: docs/MVP_TEST_E2E.md)
- [x] End-to-end testing of MVP workflow (Manual: docs/MVP_TEST_E2E.md)
  - [x] User registration → Admin approval → Login flow
  - [x] Database connection setup and schema import
  - [x] Permission configuration and access control verification
  - [x] Table/record browsing with filtering and sorting (Database connection & schema import verified)
- [x] Backend Model layer naming standardization and build verification (Entity → Model naming completed)
- [ ] Basic stability testing (Manual: docs/MVP_TEST_STABILITY.md)
- [ ] Security validation and audit log verification (Manual: docs/MVP_TEST_SECURITY.md)
- [x] Documentation for MVP usage (Complete: docs/MVP_USER_MANUAL.md)

**MVP Delivery**: 🔄 TESTING IN PROGRESS - Core functionality operational, E2E testing execution in progress

---

### Phase 5: Enhanced Data Operations (Weeks 9-11) 🔄 READY TO START

#### 5.1 Record Editing (Frontend UX Implementation) ⏳ NEXT PRIORITY
- [ ] Implement record creation modal/form (Frontend UI with existing Backend APIs)
- [ ] Implement record editing modal/form (Frontend UI with existing Backend APIs)  
- [ ] Implement record deletion confirmation (Frontend UI with existing Backend APIs)
- [ ] Add frontend data validation and error handling
- [ ] Integrate with existing CRUD APIs (Phase 4.2 Backend APIs ready)

#### 5.2 Advanced Filtering & Query Input
- [ ] SQL WHERE clause manual input
- [ ] Advanced filtering UI components
- [ ] Query parameter validation
- [ ] Result export functionality

**Milestone 4**: Complete data maintenance capabilities

---

### Phase 6: Query Builder System (Weeks 12-15)

#### 6.1 Visual Query Builder Core
- [ ] Create tabbed interface (SELECT, FROM, JOIN, WHERE, etc.)
- [ ] Implement table selection with alias management
- [ ] Build column selection with permission integration
- [ ] Add basic aggregate function support

#### 6.2 Query Management & Storage
- [ ] Implement SQL generation from visual builder
- [ ] Create named query storage (public/private)
- [ ] Build parameterized query support
- [ ] Add query execution history

**Milestone 5**: Advanced query capabilities operational

---

### Phase 7: Production Readiness (Weeks 16-17)

#### 7.1 12 Factor App Compliance
- [ ] Externalize all configuration via environment variables
- [ ] Implement proper logging for production
- [ ] Create health check endpoints
- [ ] Build executable WAR with embedded server

#### 7.2 Testing & Quality Assurance
- [ ] Comprehensive unit testing (backend services)
- [ ] Integration testing for database operations
- [ ] Frontend component testing
- [ ] End-to-end workflow testing

#### 7.3 Documentation & Deployment
- [ ] Create deployment documentation
- [ ] Build Docker images for containerization
- [ ] Setup monitoring and logging configuration
- [ ] Create user documentation

**Milestone 6**: Production-ready release

---

## Risk Management

### High Priority Risks
- **Multi-Database Complexity**: Different SQL dialects and metadata structures
  - *Mitigation*: Start with single database type, expand incrementally
- **Permission System Performance**: Complex permission checks may impact performance
  - *Mitigation*: Implement caching early, optimize queries

### Medium Priority Risks
- **Single Developer Capacity**: Large scope for single developer
  - *Mitigation*: Focus on MVP first, incremental feature delivery
- **Email Integration**: Development vs production email configuration
  - *Mitigation*: Use profiles and external configuration

---

## Audit Logging Requirements

### Required Audit Events
- [ ] User login/logout with timestamp and IP
- [ ] Admin account approval/rejection
- [ ] RDBMS connection configuration changes
- [ ] Schema import operations
- [ ] Permission configuration changes
- [ ] Large dataset queries (>100 records, configurable threshold)
- [ ] All administrative operations

### Audit Log Format
- **Timestamp**: ISO 8601 format
- **User**: Username or system
- **Action**: Standardized action codes
- **Target**: Resource being operated on
- **Details**: JSON formatted additional information
- **IP Address**: Source IP for security tracking

---

## Future Enhancements (Post-MVP)

### Phase 8: Advanced Features (Future)
- [ ] Advanced query optimization and suggestions
- [ ] Data import/export functionality
- [ ] Advanced reporting and analytics
- [ ] Mobile-responsive enhancements
- [ ] API documentation and external integrations
- [ ] Multi-language support
- [ ] Advanced audit trail visualization

---

## Directory Structure

```
MasterMeister/
├── backend/                 # Spring Boot application
│   ├── src/main/java/      # Java source code
│   ├── src/main/resources/ # Configuration and static resources
│   ├── src/test/           # Test code
│   └── build.gradle        # Gradle build configuration
├── frontend/               # React application
│   ├── src/                # React source code
│   ├── public/             # Static assets
│   ├── package.json        # npm configuration
│   └── vite.config.js      # Vite configuration
├── devenv/                 # Development environment
│   ├── docker-compose.yml  # Development services
│   ├── mailpit/            # MailPit configuration
│   └── databases/          # Database initialization scripts
├── docs/                   # Additional documentation
├── ROADMAP.md              # This file
├── REQUIREMENTS.md         # Requirements specification
└── CLAUDE.md              # Development guidelines
```

---

*This roadmap prioritizes early MVP delivery with incremental feature expansion. All phases are designed for single-developer implementation with realistic timelines.*

**Last Updated**: September 8, 2025  
**Version**: 1.5  
**Next Review**: Phase 4.4 MVP Testing Execution Complete  
**Target MVP Delivery**: Week 8 (Testing In Progress)  
**Current Progress**: Phase 4.4 E2E Testing In Progress - Backend Model Standardization Complete, Database Connection & Schema Testing Verified