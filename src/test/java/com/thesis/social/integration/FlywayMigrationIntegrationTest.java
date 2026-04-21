package com.thesis.social.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SuppressWarnings("resource")
class FlywayMigrationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("social_features")
        .withUsername("social")
        .withPassword("social");

    @Test
    void flywayShouldCreateExpectedTables() throws Exception {
        Flyway flyway = Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .load();

        flyway.migrate();

        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {
            assertTrue(tableExists(stmt, "friend_requests"));
            assertTrue(tableExists(stmt, "friendships"));
            assertTrue(tableExists(stmt, "profile_blocks"));
            assertTrue(tableExists(stmt, "chat_channels"));
            assertTrue(tableExists(stmt, "chat_participants"));
            assertTrue(tableExists(stmt, "messages"));
        }
    }

    private boolean tableExists(Statement stmt, String tableName) throws Exception {
        try (ResultSet rs = stmt.executeQuery("SELECT to_regclass('public." + tableName + "')")) {
            rs.next();
            return rs.getString(1) != null;
        }
    }
}
