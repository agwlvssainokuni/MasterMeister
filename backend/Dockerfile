# Runtime image for Spring Boot WAR
FROM eclipse-temurin:21-jre

# Set working directory
WORKDIR /app

# Copy pre-built WAR file
COPY build/libs/*.war app.war

# Create user, directories and set permissions in one layer
RUN addgroup --system spring && \
    adduser --system --group spring --shell /bin/false && \
    mkdir -p /app/data /app/logs && \
    chown -R spring:spring /app && \
    chmod 755 /app/data /app/logs

# Switch to non-root user
USER spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.war"]
