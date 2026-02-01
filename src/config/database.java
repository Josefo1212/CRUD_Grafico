package config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class database {
    private static final String ENV_PATH = "src/.env";
    private static final Properties ENV = loadEnv();

    public static final String DB_HOST = getEnv("DB_HOST", "localhost");
    public static final String DB_PORT = getEnv("DB_PORT", "5432");
    public static final String DB_NAME = getEnv("DB_NAME", "");
    public static final String DB_USER = getEnv("DB_USER", "");
    public static final String DB_PASSWORD = getEnv("DB_PASSWORD", "");

    public static Connection getConnection() throws SQLException {
        if (DB_NAME.isBlank()) {
            throw new IllegalStateException("DB_NAME no esta configurado en .env ni en variables de entorno.");
        }
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Driver de PostgreSQL no encontrado. Verifica lib/postgresql-42.7.8.jar.");
        }
        var url = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
        return DriverManager.getConnection(url, DB_USER, DB_PASSWORD);
    }

    private static Properties loadEnv() {
        var props = new Properties();
        var envPath = Paths.get(ENV_PATH);
        if (!Files.exists(envPath)) {
            envPath = Paths.get(".env");
        }
        if (!Files.exists(envPath)) {
            return props;
        }
        try {
            var lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
            for (var line : lines) {
                var trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                var idx = trimmed.indexOf('=');
                if (idx <= 0 || idx == trimmed.length() - 1) {
                    continue;
                }
                var key = trimmed.substring(0, idx).trim();
                var value = trimmed.substring(idx + 1).trim();
                props.setProperty(key, value);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo .env", e);
        }
        return props;
    }

    private static String getEnv(String key, String defaultValue) {
        var sysValue = System.getenv(key);
        if (sysValue != null && !sysValue.isBlank()) {
            return sysValue;
        }
        var fileValue = ENV.getProperty(key);
        if (fileValue != null && !fileValue.isBlank()) {
            return fileValue;
        }
        return defaultValue;
    }
}
