# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Master Data Maintenance Application - a Single Page Application (SPA) for maintaining master data stored in an RDBMS. The project is currently in initial setup phase.

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

This repository is currently in initial setup phase. No build files or source code have been created yet.

Future development will require:
- Setting up Spring Boot backend with Maven/Gradle
- Setting up React frontend with npm/yarn
- Database configuration for both internal and target RDBMS
- Implementation of the permission system and query builder components