# MasterMeister

A Master Data Maintenance Single Page Application (SPA) for maintaining master data stored in RDBMS systems.

## Project Overview

MasterMeister is a web-based application designed to provide a secure, user-friendly interface for maintaining master data across multiple database systems. The application features role-based access control, visual query building, and comprehensive audit logging.

## Features

### Core Functionality
- **User Management**: Registration workflow with admin approval
- **Multi-Database Support**: Connect to MySQL, MariaDB, PostgreSQL, and H2 databases
- **Access Control**: Granular table and column-level permissions
- **Data Maintenance**: View, edit, create, and delete records with transaction support
- **Visual Query Builder**: Intuitive interface for building complex SQL queries
- **Query Management**: Save, share, and execute parameterized queries
- **Audit Logging**: Comprehensive tracking of user activities and data operations

### Security Features
- JWT-based authentication
- Role-based authorization
- Permission-based data access control
- Audit trail for all operations
- Email-based user verification

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

### Deployment
- **12 Factor App** compliant
- **Executable WAR** for standalone deployment
- **Docker** containerization support
- **Multi-environment** configuration support

## Project Status

🚧 **Currently in Development** 🚧

This project is in the initial development phase. The following components are planned:

- [ ] Project foundation and development environment
- [ ] User authentication and management system
- [ ] Database configuration and schema import
- [ ] Data access and display functionality
- [ ] Query builder and execution system
- [ ] Production deployment readiness

See [ROADMAP.md](ROADMAP.md) for detailed development plans and milestones.

## Project Structure

```
MasterMeister/
├── backend/                 # Spring Boot application (planned)
├── frontend/               # React application (planned)
├── devenv/                 # Development environment (planned)
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

*Development setup instructions will be added as the project structure is implemented.*

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

**Current Phase**: Project Planning and Foundation Setup  
**MVP Target**: Week 8 of development  
**Next Milestone**: Development environment setup

For detailed progress tracking, see [ROADMAP.md](ROADMAP.md).

---

*This README will be updated as development progresses and more components become available.*