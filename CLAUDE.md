# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Master Data Maintenance Application - a Single Page Application (SPA) for maintaining master data stored in an RDBMS. The project is currently in Phase 2: Authentication & User Management implementation.

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
- Table/view listing with access control
- Record listing with pagination and filtering
- In-place record editing with transaction management
- CRUD operations based on permissions
- Single unified API for all operations

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
- ✅ **Frontend Complete**: React 19 with modern architecture
  - ✅ React 19 frontend with Node.js 22
  - ✅ i18n internationalization (Japanese/English)
  - ✅ Comprehensive CSS design system with responsive design
  - ✅ Complete authentication flow (login/logout/register/email-confirm)
  - ✅ Admin dashboard with user approval workflow
  - ✅ StrictMode compatibility with duplicate execution prevention

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