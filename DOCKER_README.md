# Docker Compose Setup for E-Commerce Backend

This directory contains Docker configuration files for running the Spring Boot e-commerce application with PostgreSQL database.

## Files

- `Dockerfile` - Multi-stage build for the Spring Boot application
- `docker-compose.yml` - Orchestrates PostgreSQL and Spring Boot services
- `.dockerignore` - Excludes unnecessary files from Docker build context

## Prerequisites

- Docker Desktop (or Docker Engine + Docker Compose)
- At least 2GB of free disk space

## Quick Start

1. **Build and start all services:**
   ```bash
   docker-compose up -d
   ```

2. **View logs:**
   ```bash
   docker-compose logs -f app
   ```

3. **Stop all services:**
   ```bash
   docker-compose down
   ```

4. **Stop and remove volumes (clean database):**
   ```bash
   docker-compose down -v
   ```

## Services

### PostgreSQL Database
- **Container:** `ecom-postgres`
- **Port:** `5432` (mapped to host)
- **Database:** `studio_sara`
- **User:** `studio_sara`
- **Password:** `StudioSara@111288`
- **Data Persistence:** Stored in Docker volume `postgres_data`

### Spring Boot Application
- **Container:** `ecom-app`
- **Port:** `8080` (mapped to host)
- **URL:** http://localhost:8080
- **Health Check:** Waits for PostgreSQL to be healthy before starting

## Environment Variables

All configuration is managed through environment variables in `docker-compose.yml`. Key configurations include:

- Database connection (automatically points to PostgreSQL container)
- JWT secrets and expiration
- OAuth2 Google credentials
- Mail server configuration
- Cloudinary and Swipe API settings

## Building the Application

The Dockerfile uses a multi-stage build:
1. **Build stage:** Uses Maven to compile and package the application
2. **Runtime stage:** Uses lightweight JRE Alpine image

To rebuild the application:
```bash
docker-compose build app
```

## Troubleshooting

### Application won't start
- Check if PostgreSQL is healthy: `docker-compose ps`
- View application logs: `docker-compose logs app`
- Ensure port 8080 is not already in use

### Database connection issues
- Verify PostgreSQL container is running: `docker-compose ps postgres`
- Check database logs: `docker-compose logs postgres`
- Ensure the database URL in environment variables uses `postgres` as hostname (not `localhost`)

### Port conflicts
If port 8080 or 5432 are already in use, modify the port mappings in `docker-compose.yml`:
```yaml
ports:
  - "8081:8080"  # Use 8081 on host instead
```

## Development Tips

- For development, you can mount your source code as a volume for hot-reload (requires additional configuration)
- Database data persists in Docker volumes - use `docker-compose down -v` to reset
- To access PostgreSQL directly: `docker-compose exec postgres psql -U studio_sara -d studio_sara`
