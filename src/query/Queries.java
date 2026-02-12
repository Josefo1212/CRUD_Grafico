package query;

public final class Queries {
    public static final String SELECT_ALL = "SELECT * FROM %s";
    public static final String SELECT_BY_PK = "SELECT * FROM %s WHERE %s = ?";
    public static final String INSERT_TEMPLATE = "INSERT INTO %s (%s) VALUES (%s)";
    public static final String UPDATE_TEMPLATE = "UPDATE %s SET %s WHERE %s = ?";
    public static final String DELETE_TEMPLATE = "DELETE FROM %s WHERE %s = ?";

    private Queries() {
    }
}
