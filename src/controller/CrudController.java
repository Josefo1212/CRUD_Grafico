package controller;

import model.Cliente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CrudController {
    private Connection conn;

    public boolean connect(String host, String port, String dbName, String user, String password) throws SQLException {
        var url = String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);
        conn = DriverManager.getConnection(url, user, password);
        createTableIfNotExists();
        return conn != null && !conn.isClosed();
    }

    private void createTableIfNotExists() throws SQLException {
        var sql = "CREATE TABLE IF NOT EXISTS clientes (id SERIAL PRIMARY KEY, nombre TEXT NOT NULL, correo TEXT);";
        try (var stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public Cliente createCliente(String nombre, String correo) throws SQLException {
        var sql = "INSERT INTO clientes (nombre, correo) VALUES (?, ?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, correo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return new Cliente(id, nombre, correo);
                }
            }
        }
        return null;
    }

    public List<Cliente> readAllClientes() throws SQLException {
        var list = new ArrayList<Cliente>();
        var sql = "SELECT id, nombre, correo FROM clientes ORDER BY id";
        try (var stmt = conn.createStatement(); var rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Cliente(rs.getInt("id"), rs.getString("nombre"), rs.getString("correo")));
            }
        }
        return list;
    }

    public boolean updateCliente(Cliente c) throws SQLException {
        var sql = "UPDATE clientes SET nombre = ?, correo = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getCorreo());
            ps.setInt(3, c.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteCliente(int id) throws SQLException {
        var sql = "DELETE FROM clientes WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
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
}
