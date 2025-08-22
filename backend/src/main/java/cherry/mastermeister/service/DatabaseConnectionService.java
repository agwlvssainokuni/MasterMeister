/*
 * Copyright 2025 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cherry.mastermeister.service;

import cherry.mastermeister.entity.DatabaseConnection;
import cherry.mastermeister.entity.DatabaseConnection.DatabaseType;
import cherry.mastermeister.exception.DatabaseConnectionNotFoundException;
import cherry.mastermeister.model.DatabaseConnectionModel;
import cherry.mastermeister.repository.DatabaseConnectionRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DatabaseConnectionService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DatabaseConnectionRepository databaseConnectionRepository;
    private final Map<Long, HikariDataSource> dataSourceCache = new ConcurrentHashMap<>();

    public DatabaseConnectionService(DatabaseConnectionRepository databaseConnectionRepository) {
        this.databaseConnectionRepository = databaseConnectionRepository;
    }

    public DataSource getDataSource(Long connectionId) {
        return dataSourceCache.computeIfAbsent(connectionId, this::createDataSource);
    }

    public boolean testConnection(Long connectionId) {
        DatabaseConnection dbConnection = databaseConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Database connection not found: " + connectionId));

        try {
            DataSource dataSource = getDataSource(connectionId);
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(10);
                updateTestResult(dbConnection, isValid);
                return isValid;
            }
        } catch (SQLException e) {
            logger.error("Database connection test failed for connection ID: {}", connectionId, e);
            updateTestResult(dbConnection, false);
            return false;
        }
    }

    public void closeDataSource(Long connectionId) {
        HikariDataSource dataSource = dataSourceCache.remove(connectionId);
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Closed DataSource for connection ID: {}", connectionId);
        }
    }

    public void closeAllDataSources() {
        dataSourceCache.forEach((id, dataSource) -> {
            if (!dataSource.isClosed()) {
                dataSource.close();
                logger.info("Closed DataSource for connection ID: {}", id);
            }
        });
        dataSourceCache.clear();
    }

    private HikariDataSource createDataSource(Long connectionId) {
        DatabaseConnection dbConnection = databaseConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Database connection not found: " + connectionId));

        if (!dbConnection.isActive()) {
            throw new IllegalStateException("Database connection is not active: " + connectionId);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(buildJdbcUrl(dbConnection));
        config.setUsername(dbConnection.getUsername());
        config.setPassword(dbConnection.getPassword());
        config.setDriverClassName(getDriverClassName(dbConnection.getDbType()));

        // Connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);

        // Connection validation
        config.setConnectionTestQuery(getValidationQuery(dbConnection.getDbType()));
        config.setValidationTimeout(5000);

        // Pool name for identification
        config.setPoolName("MasterMeister-" + dbConnection.getName() + "-" + connectionId);

        logger.info("Creating DataSource for connection: {} (ID: {})", dbConnection.getName(), connectionId);
        return new HikariDataSource(config);
    }

    private String buildJdbcUrl(DatabaseConnection dbConnection) {
        String baseUrl = switch (dbConnection.getDbType()) {
            case MYSQL -> String.format("jdbc:mysql://%s:%d/%s",
                    dbConnection.getHost(), dbConnection.getPort(), dbConnection.getDatabaseName());
            case MARIADB -> String.format("jdbc:mariadb://%s:%d/%s",
                    dbConnection.getHost(), dbConnection.getPort(), dbConnection.getDatabaseName());
            case POSTGRESQL -> String.format("jdbc:postgresql://%s:%d/%s",
                    dbConnection.getHost(), dbConnection.getPort(), dbConnection.getDatabaseName());
            case H2 -> dbConnection.getHost().equals("mem")
                    ? String.format("jdbc:h2:mem:%s", dbConnection.getDatabaseName())
                    : String.format("jdbc:h2:tcp://%s:%d/%s",
                    dbConnection.getHost(), dbConnection.getPort(), dbConnection.getDatabaseName());
        };

        if (dbConnection.getConnectionParams() != null && !dbConnection.getConnectionParams().trim().isEmpty()) {
            return baseUrl + "?" + dbConnection.getConnectionParams();
        }
        return baseUrl;
    }

    private String getDriverClassName(DatabaseType dbType) {
        return switch (dbType) {
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case MARIADB -> "org.mariadb.jdbc.Driver";
            case POSTGRESQL -> "org.postgresql.Driver";
            case H2 -> "org.h2.Driver";
        };
    }

    private String getValidationQuery(DatabaseType dbType) {
        return switch (dbType) {
            case MYSQL, MARIADB -> "SELECT 1";
            case POSTGRESQL -> "SELECT 1";
            case H2 -> "SELECT 1";
        };
    }

    public List<DatabaseConnectionModel> getAllConnections() {
        return databaseConnectionRepository.findAll().stream()
                .map(this::toModel)
                .toList();
    }

    public DatabaseConnectionModel getConnection(Long connectionId) {
        DatabaseConnection entity = findEntityById(connectionId);
        return toModel(entity);
    }

    public DatabaseConnectionModel createConnection(DatabaseConnection connection) {
        connection.setId(null);
        DatabaseConnection saved = databaseConnectionRepository.save(connection);
        return toModel(saved);
    }

    public DatabaseConnectionModel updateConnection(Long connectionId, DatabaseConnection connection) {
        DatabaseConnection existingConnection = findEntityById(connectionId);
        connection.setId(connectionId);
        connection.setCreatedAt(existingConnection.getCreatedAt());

        closeDataSource(connectionId);
        DatabaseConnection updated = databaseConnectionRepository.save(connection);
        return toModel(updated);
    }

    public void deleteConnection(Long connectionId) {
        DatabaseConnection connection = findEntityById(connectionId);
        closeDataSource(connectionId);
        databaseConnectionRepository.delete(connection);
    }

    public Map<String, Object> testConnectionWithDetails(Long connectionId) {
        DatabaseConnection dbConnection = findEntityById(connectionId);
        boolean isConnected = testConnection(connectionId);

        return Map.of(
                "connected", isConnected,
                "lastTestedAt", dbConnection.getLastTestedAt(),
                "testResult", dbConnection.getTestResult()
        );
    }

    public DatabaseConnectionModel activateConnection(Long connectionId) {
        DatabaseConnection connection = findEntityById(connectionId);
        connection.setActive(true);
        DatabaseConnection updated = databaseConnectionRepository.save(connection);
        return toModel(updated);
    }

    public DatabaseConnectionModel deactivateConnection(Long connectionId) {
        DatabaseConnection connection = findEntityById(connectionId);
        connection.setActive(false);
        closeDataSource(connectionId);
        DatabaseConnection updated = databaseConnectionRepository.save(connection);
        return toModel(updated);
    }

    private DatabaseConnection findEntityById(Long connectionId) {
        return databaseConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new DatabaseConnectionNotFoundException("Database connection not found: " + connectionId));
    }

    private DatabaseConnectionModel toModel(DatabaseConnection entity) {
        return new DatabaseConnectionModel(
                entity.getId(),
                entity.getName(),
                entity.getDbType(),
                entity.getHost(),
                entity.getPort(),
                entity.getDatabaseName(),
                entity.getUsername(),
                entity.getConnectionParams(),
                entity.isActive(),
                entity.getLastTestedAt(),
                entity.getTestResult(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private void updateTestResult(DatabaseConnection dbConnection, boolean testResult) {
        dbConnection.setLastTestedAt(LocalDateTime.now());
        dbConnection.setTestResult(testResult);
        databaseConnectionRepository.save(dbConnection);
    }
}
