# MasterMeister

A Master Data Maintenance Single Page Application (SPA) for maintaining master data stored in RDBMS systems.

## Project Overview

MasterMeister is a web-based application designed to provide a secure, user-friendly interface for maintaining master data across multiple database systems. The application features role-based access control, visual query building, and comprehensive audit logging.

## Features

### Core Functionality
- **User Management**: Registration workflow with admin approval
- **Multi-Database Support**: Connect to MySQL, MariaDB, PostgreSQL, and H2 databases
- **Access Control**: Granular table and column-level permissions
- **Data Display**: View tables and records with advanced filtering, sorting, and pagination
- **Data Maintenance**: Record creation, editing, and deletion with transaction support (Backend APIs ready)
- **Visual Query Builder**: Intuitive interface for building complex SQL queries (planned)
- **Query Management**: Save, share, and execute parameterized queries (planned)
- **Audit Logging**: Comprehensive tracking of user activities and data operations
- **Responsive UI**: Tabbed interface with horizontal scrolling and fixed columns

### Security Features
- JWT-based authentication
- Role-based authorization
- Permission-based data access control
- Audit trail for all operations
- Two-step email-first registration flow

## Technology Stack

### Backend
- **Java 21** (LTS)
- **Spring Boot 3.5** with Spring Security
- **Gradle 9.0** build system
- **H2 Database** for internal application data
- **JPA/Hibernate** for database operations
- **Connection Pooling** for target database connections

### Frontend
- **Node.js 22** (LTS)
- **React 19** with modern hooks
- **Vite** for fast development and building
- **Modern JavaScript/TypeScript**

### Database Support
- MySQL
- MariaDB
- PostgreSQL
- H2 Database

### Architecture
- **Backend**: Entity/Model/DTO 3-layer separation with strict responsibility boundaries
- **Frontend**: Service layer handles API communication with type conversion isolation
- **Database**: Connection pooling with dynamic DataSource management
- **Security**: JWT-based authentication with role-based authorization

### Deployment
- **12 Factor App** compliant
- **Executable WAR** for standalone deployment
- **Docker** containerization support
- **Multi-environment** configuration support

## Project Status

✅ **Phase 4.3 Complete** - 🔄 **Phase 4.4 In Progress**

**Frontend Data Display Complete - MVP E2E Testing In Progress**

**Completed Features:**
- ✅ **Phase 1**: Project foundation and development environment
- ✅ **Phase 2**: Authentication & User Management **COMPLETE**
  - ✅ Two-step email-first registration with configurable token expiry
  - ✅ Administrator approval workflow with notifications  
  - ✅ JWT-based authentication with token rotation
  - ✅ Comprehensive audit logging
  - ✅ Admin dashboard UI with user management
  - ✅ React 19 frontend with i18n support
  - ✅ Complete authentication flow (login/logout/register)
  - ✅ Responsive design and modern architecture
- ✅ **Phase 3**: Database Configuration System **COMPLETE**
  - ✅ **Phase 3.1**: Multi-Database Connection Management
    - ✅ Dynamic DataSource creation with HikariCP connection pooling
    - ✅ MySQL/MariaDB/PostgreSQL/H2 support with connection testing
    - ✅ Backend architecture with strict Entity/Model/DTO separation
  - ✅ **Phase 3.2**: Schema Import & Metadata Management
    - ✅ Schema reader for multiple database types with DatabaseMetaData
    - ✅ Table/column metadata storage with JPA entity relationships
    - ✅ Schema update operations with transaction support and batch processing
    - ✅ Admin logging integration for all schema operations
  - ✅ **Phase 3.3**: Access Control System
    - ✅ Hierarchical permission entities (CONNECTION→SCHEMA→TABLE→COLUMN)
    - ✅ Permission authentication services with Spring Security integration
    - ✅ Three-layer enforcement: @RequirePermission annotation, programmatic Utils, SQL analysis
    - ✅ YAML configuration management with export/import/validation endpoints

- ✅ **Phase 4**: Data Access & Display **COMPLETE**
  - ✅ **Phase 4.1**: Secure Data Access (Backend APIs)
    - ✅ Permission-based table listing with access control
    - ✅ Secure record retrieval with column-level filtering  
    - ✅ Pagination and basic filtering functionality
    - ✅ Large dataset query logging (100+ records threshold)
  - ✅ **Phase 4.2**: Backend CRUD APIs
    - ✅ Record creation with permission validation
    - ✅ Record update with transaction management
    - ✅ Record deletion with referential integrity checks
    - ✅ Comprehensive audit logging with failure-resistant strategy
  - ✅ **Phase 4.3**: Frontend Data Display UI
    - ✅ Advanced filtering with separated filter bar layout
    - ✅ Horizontal scrolling with fixed columns for wide tables
    - ✅ Responsive design with tabbed interface (data/metadata)
    - ✅ Database tree view with optimized density
    - ✅ Unified table styling with stripe effects and hover states
  - 🔄 **Phase 4.4**: MVP Integration & Testing
    - ✅ Backend Model layer naming standardization completed
    - ✅ E2E test documentation prepared (docs/MVP_TEST_E2E.md)
    - 🔄 E2E testing execution in progress (4.4.1-4.4.3 complete, 4.4.4 in progress)

**Next Phase Options:**
- 🔄 **Phase 4.4**: MVP Integration & Testing (In Progress - E2E Testing Execution)
- 🚧 **Phase 5**: Enhanced Data Operations (Frontend UX for CRUD APIs - Ready to Start)

See [ROADMAP.md](ROADMAP.md) for detailed development plans and milestones.

## Project Structure

```
MasterMeister/
├── backend/                 # Spring Boot 3.5.4 application
│   ├── src/main/java/      # Java source code
│   ├── src/main/resources/ # Configuration files
│   ├── src/test/           # Test code
│   ├── build.gradle        # Gradle build configuration
│   └── Dockerfile          # Docker container configuration
├── frontend/               # React 19 application with Vite
├── devenv/                 # Development environment with Docker Compose
├── docs/                   # Documentation
├── ROADMAP.md              # Development roadmap
├── REQUIREMENTS.md         # Detailed requirements specification
├── CLAUDE.md              # Development guidelines
└── README.md              # This file
```

## Getting Started

### Prerequisites

- **Java 21** or higher
- **Node.js 22** or higher
- **Docker** and **Docker Compose** (for development environment)
- **Gradle 9.0** (or use Gradle Wrapper)

### Development Setup

#### Backend (Spring Boot)

1. **Build and run the application:**
   ```bash
   cd backend
   ./gradlew build
   ./gradlew bootRun
   ```

2. **Run with different profiles:**
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

3. **Run tests:**
   ```bash
   ./gradlew test
   ```

4. **Docker deployment:**
   ```bash
   ./gradlew build
   docker build -t mastermeister .
   docker run -p 8080:8080 mastermeister
   ```

#### Frontend (React)

1. **Install dependencies and run development server:**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

2. **Build for production:**
   ```bash
   npm run build
   ```

3. **Run linting:**
   ```bash
   npm run lint
   ```

#### Full Stack Development
- **Backend + Frontend build**: `./gradlew build -Pfrontend`
- **Frontend development**: http://localhost:5173 (Vite dev server)
- **Backend API**: http://localhost:8080 (Spring Boot)

#### API Documentation
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api/v3/api-docs
- **H2 Console** (dev only): http://localhost:8080/h2-console

#### Default Admin User
- **Email**: admin@example.com
- **Password**: password

## Documentation

- [Requirements Specification](REQUIREMENTS.md) - Detailed functional and technical requirements
- [Development Roadmap](ROADMAP.md) - Project phases, milestones, and timeline
- [Development Guidelines](CLAUDE.md) - Architecture and development guidance

## Target Use Cases

MasterMeister is designed for organizations that need to:

- Maintain reference data across multiple database systems
- Provide controlled access to sensitive master data
- Enable business users to query and update data safely
- Maintain comprehensive audit trails for compliance
- Reduce dependency on database administrators for routine maintenance

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Development Status

**Current Phase**: Phase 4.4 - MVP Integration & Testing (E2E Testing In Progress)
**MVP Target**: Week 8 of development  
**Latest Milestone**: ✅ Milestone 4.3 Complete - Frontend Data Display with Advanced UI/UX Operational
**Current Work**: Backend Model standardization complete, E2E testing 4.4.1-4.4.3 verified, 4.4.4 in progress

For detailed progress tracking, see [ROADMAP.md](ROADMAP.md).

---

*This README will be updated as development progresses and more components become available.*