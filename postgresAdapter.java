import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class postgresAdapter {
    private final String url = "jdbc:postgresql://localhost:5433/postgres";
    private final String user = "postgres";
    private final String password = "123";

    public Connection connect() throws SQLException {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to postgres");
        } catch (SQLException e) {
            System.out.println(e);
        }
        return conn;
    }

    
}
