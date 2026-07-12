package com.senk.sqliteviewer;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatabaseService {

    private Connection connection;
    private File currentFile;

    public void open(File file) throws SQLException {
        close();
        String url = "jdbc:sqlite:" + file.getAbsolutePath().replace("\\", "/");
        connection = DriverManager.getConnection(url);
        currentFile = file;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            connection = null;
            currentFile = null;
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public List<String> getTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tables.add(rs.getString("name"));
            }
        }
        return tables;
    }

    public List<String> getViews() throws SQLException {
        List<String> views = new ArrayList<>();
        String sql = "SELECT name FROM sqlite_master WHERE type='view' ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                views.add(rs.getString("name"));
            }
        }
        return views;
    }

    public long getTotalCount(String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM \"" + tableName.replace("\"", "\"\"") + "\"";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    public QueryResult getPage(String tableName, int offset, int limit) throws SQLException {
        return getPage(tableName, offset, limit, null, null);
    }

    public QueryResult getPage(String tableName, int offset, int limit,
                                String orderBy, String direction) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM \"").append(tableName.replace("\"", "\"\"")).append("\"");
        if (orderBy != null && !orderBy.isEmpty()) {
            sql.append(" ORDER BY \"").append(orderBy.replace("\"", "\"\"")).append("\"");
            if ("DESC".equalsIgnoreCase(direction)) {
                sql.append(" DESC");
            }
        }
        sql.append(" LIMIT ").append(limit).append(" OFFSET ").append(offset);
        return executeQuery(sql.toString());
    }

    public QueryResult executeQuery(String sql) throws SQLException {
        long startTime = System.currentTimeMillis();

        List<String> columns = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnName(i));
            }

            while (rs.next()) {
                List<Object> row = new ArrayList<>(colCount);
                for (int i = 1; i <= colCount; i++) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        return new QueryResult(columns, rows, elapsed);
    }

    public List<ColumnInfo> getColumnInfo(String tableName) throws SQLException {
        String sql = "PRAGMA table_info(\"" + tableName.replace("\"", "\"\"") + "\")";
        List<ColumnInfo> result = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new ColumnInfo(
                        rs.getInt("cid"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getBoolean("notnull"),
                        rs.getString("dflt_value"),
                        rs.getBoolean("pk")
                ));
            }
        }
        return result;
    }

    public static class QueryResult {
        private final List<String> columns;
        private final List<List<Object>> rows;
        private final long executionTimeMs;

        public QueryResult(List<String> columns, List<List<Object>> rows, long executionTimeMs) {
            this.columns = columns;
            this.rows = rows;
            this.executionTimeMs = executionTimeMs;
        }

        public List<String> getColumns() {
            return columns;
        }

        public List<List<Object>> getRows() {
            return rows;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public int getRowCount() {
            return rows.size();
        }
    }

    public static class ColumnInfo {
        private final int cid;
        private final String name;
        private final String type;
        private final boolean notNull;
        private final String defaultValue;
        private final boolean primaryKey;

        public ColumnInfo(int cid, String name, String type, boolean notNull,
                          String defaultValue, boolean primaryKey) {
            this.cid = cid;
            this.name = name;
            this.type = type;
            this.notNull = notNull;
            this.defaultValue = defaultValue;
            this.primaryKey = primaryKey;
        }

        public int getCid() { return cid; }
        public String getName() { return name; }
        public String getType() { return type; }
        public boolean isNotNull() { return notNull; }
        public String getDefaultValue() { return defaultValue; }
        public boolean isPrimaryKey() { return primaryKey; }
    }
}
