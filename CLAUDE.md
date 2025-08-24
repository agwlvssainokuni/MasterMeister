# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Master Data Maintenance Application - a Single Page Application (SPA) for maintaining master data stored in an RDBMS. The project is currently in Phase 4: Data Access & Display (Backend APIs completed, Frontend UI pending).

## Technical Architecture

**Frontend & Backend Stack:**
- Backend: Spring Boot 3.5 with Java 21
- Frontend: React 19 with Node.js 22
- Database: JPA for internal DB, connection pooling for target RDBMS
- Date/Time: java.time API for RDBMS operations

**Database Architecture:**
- Internal DB: Application operational data
- Target RDBMS: Master data to be maintained
- Uses connection pooling instead of DriverManager.getConnection()

## Core Application Features

**User Management:**
- User registration with email confirmation
- Administrator approval workflow
- Role-based authentication

**Database Configuration:**
- Admin dashboard for RDBMS connection setup
- Schema update operations to read table/view structures
- Granular access permissions (table-level and column-level)
- YAML-based permission import/export

**Master Data Maintenance:**
- Table/view listing with access control (Backend API ✅)
- Record listing with pagination and filtering (Backend API ✅)
- In-place record editing with transaction management (Backend API ✅)
- CRUD operations based on permissions (Backend API ✅)
- Single unified API for all operations (Backend API ✅)
- Frontend UI implementation (Pending)

**Query Features:**
- Visual query builder with tab interface (SELECT, FROM, JOIN, WHERE, etc.)
- Named query storage (public/private)
- Parameterized query support using ":param" format
- Query execution history with comprehensive logging
- SQL parsing and reverse engineering

**Key Technical Requirements:**
- Use NamedParameterJdbcTemplate for parameterized queries
- Implement proper transaction management for all operations
- Comprehensive audit logging for all database operations
- Access control enforcement at both table and column levels

## Development Setup

**Current Implementation Status:**
- ✅ **Phase 1 COMPLETED**: Project foundation and infrastructure
  - ✅ Spring Boot 3.5.4 backend with Gradle build system
  - ✅ Multi-environment configuration (dev/stg/prd)
  - ✅ H2 database with file-based persistence in non-default environments
  - ✅ Docker containerization ready
- ✅ **Phase 2 COMPLETED**: Authentication & User Management
  - ✅ JWT-based authentication with token rotation (5min/24h)
  - ✅ User registration with email confirmation workflow
  - ✅ Administrator approval system with email notifications
  - ✅ Comprehensive audit logging (login/logout/admin operations)
  - ✅ Email integration with MailPit for development
  - ✅ Admin user auto-initialization
  - ✅ Swagger/OpenAPI documentation with JWT authentication
- ✅ **Phase 3 COMPLETED**: Database Configuration System
  - ✅ Multi-database connection management (MySQL/MariaDB/PostgreSQL/H2)
  - ✅ Dynamic DataSource creation with HikariCP connection pooling
  - ✅ Schema introspection and metadata storage system
  - ✅ Comprehensive access control with hierarchical permissions
  - ✅ Permission enforcement: annotation-based, programmatic, SQL analysis
  - ✅ YAML-based permission configuration export/import
  - ✅ Backend architecture with strict responsibility separation
- 🔄 **Phase 4 PARTIAL**: Data Access & Display
  - ✅ **Phase 4.1 COMPLETED**: Secure Data Access
    - ✅ Permission-based table listing with access control
    - ✅ Secure record retrieval with column-level filtering
    - ✅ Pagination and basic filtering with RecordFilter
    - ✅ Large dataset query logging (100+ records threshold)
  - ✅ **Phase 4.2 COMPLETED**: Backend CRUD APIs
    - ✅ Record creation with permission validation (RecordCreateService)
    - ✅ Record update with transaction management (RecordUpdateService)
    - ✅ Record deletion with referential integrity checks (RecordDeleteService)
    - ✅ Comprehensive audit logging with transaction propagation strategy
    - ✅ Unified CRUD API with detailed error handling
  - ⏳ **Phase 4.3 PENDING**: Frontend Data Display UI
  - ⏳ **Phase 4.4 PENDING**: MVP Integration & Testing
- ✅ **Frontend Complete**: React 19 with modern architecture
  - ✅ React 19 frontend with Node.js 22
  - ✅ i18n internationalization (Japanese/English)
  - ✅ Comprehensive CSS design system with responsive design
  - ✅ Complete authentication flow (login/logout/register/email-confirm)
  - ✅ Admin dashboard with user approval workflow
  - ✅ StrictMode compatibility with duplicate execution prevention

## Code Organization Principles

**Context/Provider Pattern:**
- All React contexts follow single-file pattern (Context + Provider + Hook)
- Example: AuthContext.tsx, NotificationContext.tsx
- Avoids file fragmentation and improves maintainability

**Route Protection:**
- Use ProtectedRoute.tsx for all authentication logic
- AdminRoute is exported from ProtectedRoute.tsx (not separate file)
- Centralized authentication prevents code duplication

**Design System:**
- CSS variables use 4px-based spacing scale
- Values: xs(4px), sm(8px), md(12px), lg(16px), xl(20px), xxl(24px)
- Follows modern UI library standards (Tailwind CSS compatible)

**Backend Responsibility Separation:**
- Entity layer (entity package) for JPA/Repository operations only
- Model layer (model package) for Service business logic
- DTO layer (controller/dto package) for Controller input/output
- Controller never imports entity package (uses DTO → Model → Entity flow)
- Service never imports dto package (uses Model for all operations)
- Repository never imports dto/model packages (uses Entity only)
- Enums in dedicated package (enums) accessible by all layers

**Frontend Responsibility Separation:**
- Service layer (src/services/) handles all API communication and type conversion
- API types (types/api.ts) are confined to Service layer only
- Frontend types (types/frontend.ts) are used by Components/Pages/Contexts
- Service methods return Frontend types, never expose API Request/Response types
- Type conversion (API ↔ Frontend) must be performed within Service layer

**Build Commands:**
- `./gradlew build` - Build the application
- `./gradlew test` - Run all tests
- `./gradlew bootRun` - Run the application locally
- `docker build -t mastermeister .` - Build Docker image
- **Note**: Add `-Pfrontend` to any Gradle command to include frontend build and integration

**Frontend Commands:**
- `cd frontend && npm run dev` - Run frontend development server
- `cd frontend && npm run build` - Build frontend for production
- `cd frontend && npm run lint` - Run ESLint

**Environment Profiles:**
- `default/dev` - Development with H2 console, verbose logging
- `stg` - Staging with file-based H2, moderate logging
- `prd` - Production with file-based H2, minimal logging