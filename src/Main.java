import config.database;
import java.sql.SQLException;

void main() {
    try (var connection = database.getConnection()) {
        System.out.println("Conexion exitosa: " + (connection != null && !connection.isClosed()));
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
