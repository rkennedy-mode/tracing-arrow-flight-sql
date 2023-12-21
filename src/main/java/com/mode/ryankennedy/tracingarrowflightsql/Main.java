package com.mode.ryankennedy.tracingarrowflightsql;

import io.opentelemetry.contrib.attach.RuntimeAttach;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.sql.*;

public class Main {
    public static void main(String[] args) throws SQLException {
        RuntimeAttach.attachJavaagentToCurrentJvm();

        var hostName = args[0];
        var portNumber = Integer.parseInt(args[1]);
        var username = args[2];
        var password = args[3];

        inspectDriver(hostName, portNumber, username, password);
    }

    @WithSpan
    private static void inspectDriver(String hostName, int portNumber, String username, String password) throws SQLException {
        try (var connection = connectToDatabase(hostName, portNumber, username, password)) {
            var connectionMetadata = getConnectionMetaData(connection);
            printDatabaseVersion(connectionMetadata);
            printDatabaseTables(connectionMetadata);

            executeStatement(connection, 1);
            executeStatement(connection, 1_000_000);
        }
    }

    @WithSpan
    private static Connection connectToDatabase(String hostName, int portNumber, String username, String password) throws SQLException {
        var jdbcUrl = "jdbc:arrow-flight-sql://%s:%d/?%s".formatted(
                hostName,
                portNumber,
                "useEncryption=true&useSystemTrustStore=false");
        System.out.printf("Connecting to %s%n", jdbcUrl);
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    @WithSpan
    private static DatabaseMetaData getConnectionMetaData(Connection connection) throws SQLException {
        return connection.getMetaData();
    }

    @WithSpan
    private static void printDatabaseVersion(DatabaseMetaData connectionMetadata) throws SQLException {
        System.out.printf("Database: %s %s (%d.%d)%n",
                connectionMetadata.getDatabaseProductName(),
                connectionMetadata.getDatabaseProductVersion(),
                connectionMetadata.getDatabaseMajorVersion(),
                connectionMetadata.getDatabaseMinorVersion());
    }

    @WithSpan
    private static void printDatabaseTables(DatabaseMetaData connectionMetadata) throws SQLException {
        try (var results = connectionMetadata.getTables(null, null, null, null)) {
            while (results.next()) {
                System.out.printf("Table: %s.%s.%s%n",
                        results.getString("TABLE_CAT"),
                        results.getString("TABLE_SCHEM"),
                        results.getString("TABLE_NAME"));
            }
        }
    }

    @WithSpan
    private static void executeStatement(Connection connection,
                                         @SpanAttribute("rowCount") int desiredRowCount) throws SQLException {
        var sql = """
                SELECT *
                FROM repeat_row('Hello, World!', num_rows = %d)
                """.formatted(desiredRowCount);
        try (var statement = connection.createStatement();
             var results = statement.executeQuery(sql)) {
            while (results.next()) {
                results.getString(1);
            }
        }
    }
}
