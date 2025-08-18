# Master Data Maintenance Application - Development Roadmap

## Project Overview
Development roadmap for a Master Data Maintenance Single Page Application (SPA) with Spring Boot backend and React frontend. Designed for single-developer implementation with early MVP delivery.

## Current Status
- ✅ Requirements specification completed
- ✅ Project documentation established
- ✅ Development planning phase
- ✅ **Phase 1 COMPLETED** - Project structure setup (August 18, 2025)

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

#### 2.1 Core Authentication
- [x] Implement User entity and UserRepository
- [x] Create Spring Security configuration with JWT
- [ ] Build user registration API with email validation
- [ ] Integrate MailPit for confirmation emails

#### 2.2 Admin Approval Workflow
- [ ] Create admin dashboard for user approval
- [ ] Implement approval/rejection workflow
- [ ] Build notification system for approval results
- [ ] Create login/logout endpoints and frontend components

#### 2.3 Basic Security & Logging
- [x] Create admin operation audit logging (AuditLog entity)
- [x] Setup basic error handling and validation (API response format)
- [ ] Implement authentication logging (login/logout)

**Milestone 2**: MVP user management system operational

---

### Phase 3: Database Configuration System (Weeks 5-6)

#### 3.1 Multi-Database Connection Management
- [x] Create database configuration entity and management (DatabaseConnection entity)
- [ ] Implement dynamic DataSource creation with connection pooling
- [ ] Build database connection testing functionality
- [x] Support MySQL, MariaDB, PostgreSQL, H2 connections (enum defined)

#### 3.2 Schema Import & Metadata Management
- [ ] Create schema reader for different database types
- [ ] Implement table/column metadata storage
- [ ] Build schema update operations
- [ ] Add admin logging for schema operations

#### 3.3 Permission System Foundation
- [ ] Design table/column permission entities
- [ ] Implement basic permission assignment interface
- [ ] Create permission enforcement utilities
- [ ] Build YAML export/import foundation

**Milestone 3**: Database configuration and schema import operational

---

### Phase 4: Data Access & Display (Weeks 7-8)

#### 4.1 Secure Data Access
- [ ] Implement permission-based table listing
- [ ] Create secure record retrieval with column filtering
- [ ] Build pagination and basic filtering
- [ ] Add large dataset query logging (100+ records threshold)

#### 4.2 Frontend Data Display
- [ ] Create table/view listing interface
- [ ] Build record listing with pagination
- [ ] Implement basic filtering UI
- [ ] Add sorting and search functionality

#### 4.3 MVP Integration & Testing
- [ ] End-to-end testing of MVP workflow
- [ ] Performance testing with development datasets
- [ ] Security validation and audit log verification
- [ ] Documentation for MVP usage

**MVP Delivery**: Core functionality operational

---

### Phase 5: Enhanced Data Operations (Weeks 9-11)

#### 5.1 Record Editing
- [ ] Implement in-place record editing
- [ ] Create unified CRUD API with transaction management
- [ ] Build record creation/deletion features
- [ ] Add data validation and error handling

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

**Last Updated**: August 2025  
**Version**: 1.1  
**Next Review**: End of Phase 1  
**Target MVP Delivery**: Week 8