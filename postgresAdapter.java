import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class postgresAdapter {
    // The Java Database connectivity (JDBC) driver 
    // specify connection to Postgres with this URL
    // user and password is set in Postgres
    private final String url = "jdbc:postgresql://localhost:5433/postgres";
    private final String user = "postgres";
    private final String password = "123";

    public Connection connect() throws SQLException {
        Connection conn = null;
        try {
            // DriverManager get method from JDBC driver
            conn = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            System.out.println(e);
        }
        return conn;
    }

    
}
