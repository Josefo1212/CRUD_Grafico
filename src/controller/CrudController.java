package controller;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CrudController {
    private Connection conn;

    public boolean connect(String host, String port, String dbName, String user, String password) throws SQLException {
        var url = "jdbc:postgresql://%s:%s/%s".formatted(host, port, dbName);
        conn = DriverManager.getConnection(url, user, password);
        return conn != null && !conn.isClosed();
    }

    public List<String> listTables() throws SQLException {
        var tables = new ArrayList<String>();
        var meta = conn.getMetaData();
        try (var rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    public String getPrimaryKey(String table) throws SQLException {
        var meta = conn.getMetaData();
        try (var rs = meta.getPrimaryKeys(null, null, table)) {
            if (rs.next()) return rs.getString("COLUMN_NAME");
        }
        return null;
    }

    public String getPrimaryKeyType(String table, String pkColumn) throws SQLException {
        if (pkColumn == null || pkColumn.isBlank()) return null;
        var meta = conn.getMetaData();
        try (var rs = meta.getColumns(null, null, table, pkColumn)) {
            if (rs.next()) return rs.getString("TYPE_NAME");
        }
        return null;
    }

    public List<Map<String, Object>> readTable(String table) throws SQLException {
        var list = new ArrayList<Map<String, Object>>();
        var sql = "SELECT * FROM " + table;
        try (var stmt = conn.createStatement(); var rs = stmt.executeQuery(sql)) {
            var md = rs.getMetaData();
            var colCount = md.getColumnCount();
            while (rs.next()) {
                var row = new LinkedHashMap<String, Object>();
                for (var i = 1; i <= colCount; i++) {
                    row.put(md.getColumnLabel(i), rs.getObject(i));
                }
                list.add(row);
            }
        }
        return list;
    }

    public void insertRow(String table, Map<String, Object> values) throws SQLException {
        if (values.isEmpty()) return;
        var cols = String.join(", ", values.keySet());
        var marks = String.join(", ", Collections.nCopies(values.size(), "?"));
        var sql = "INSERT INTO " + table + " (" + cols + ") VALUES (" + marks + ")";
        try (var ps = conn.prepareStatement(sql)) {
            var i = 1;
            for (var v : values.values()) ps.setObject(i++, v);
            ps.executeUpdate();
        }
    }

    public boolean updateRow(String table, String pkColumn, Object pkValue, Map<String, Object> values) throws SQLException {
        if (values.isEmpty()) return false;
        var sets = new StringBuilder();
        for (var k : values.keySet()) {
            if (!sets.isEmpty()) sets.append(", ");
            sets.append(k).append(" = ?");
        }
        var sql = "UPDATE " + table + " SET " + sets + " WHERE " + pkColumn + " = ?";
        try (var ps = conn.prepareStatement(sql)) {
            var i = 1;
            for (var v : values.values()) ps.setObject(i++, v);
            ps.setObject(i, pkValue);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteRow(String table, String pkColumn, Object pkValue) throws SQLException {
        var sql = "DELETE FROM " + table + " WHERE " + pkColumn + " = ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setObject(1, pkValue);
            return ps.executeUpdate() > 0;
        }
    }

    public void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {}
        }
    }

    public List<ColumnInfo> listColumnsInfo(String table) throws SQLException {
        var cols = new ArrayList<ColumnInfo>();
        var meta = conn.getMetaData();
        try (var rs = meta.getColumns(null, null, table, "%")) {
            while (rs.next()) {
                var name = rs.getString("COLUMN_NAME");
                var auto = "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"));
                var def = rs.getString("COLUMN_DEF");
                if (def != null && def.toLowerCase().contains("nextval(")) auto = true;
                var typeName = rs.getString("TYPE_NAME");
                cols.add(new ColumnInfo(name, auto, typeName));
            }
        }
        return cols;
    }

    public record ColumnInfo(String name, boolean auto, String typeName) {}
}
