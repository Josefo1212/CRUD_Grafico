package config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {
    private static final Map<String, String> ENV = loadDotEnv();

    static final String DB_HOST = env("DB_HOST", "localhost");
    static final String DB_PORT = env("DB_PORT", "5432");
    static final String DB_NAME = env("DB_NAME", "");
    static final String DB_USER = env("DB_USER", "");
    static final String DB_PASSWORD = env("DB_PASSWORD", "");

    public static Connection getConnection() throws SQLException {
        if (DB_NAME.isBlank()) {
            throw new IllegalStateException("DB_NAME no esta configurado en .env ni en variables de entorno.");
        }
        var url = "jdbc:postgresql://%s:%s/%s".formatted(DB_HOST, DB_PORT, DB_NAME);
        return DriverManager.getConnection(url, DB_USER, DB_PASSWORD);
    }

    private static Map<String, String> loadDotEnv() {
        var map = new HashMap<>(System.getenv());
        for (var path : List.of(Path.of(".env"), Path.of("src", ".env"))) {
            if (!Files.exists(path)) continue;
            try {
                for (var raw : Files.readAllLines(path)) {
                    var line = raw.strip();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    var parts = line.split("=", 2);
                    if (parts.length < 2) continue;

                    var key = parts[0].strip();
                    var value = parts[1].strip();
                    if ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    map.put(key, value);
                }
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer el archivo .env", e);
            }
        }
        return map;
    }

    private static String env(String key, String fallback) {
        var value = ENV.get(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
