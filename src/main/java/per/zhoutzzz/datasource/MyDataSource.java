package per.zhoutzzz.datasource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author zhoutzzz
 */
public class MyDataSource implements DataSource {

    private final MyConnectionPool pool;

    public MyDataSource(String username, String password, String url) throws Exception {
        Properties variables = new Properties();
        variables.setProperty("password", password);
        variables.setProperty("username", username);
        variables.setProperty("jdbcUrl", url);
        this.pool = new MyConnectionPool(variables);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return pool.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return pool.getConnection();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return pool.getSource().getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter logWriter) throws SQLException {
        pool.getSource().setLogWriter(logWriter);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        pool.getSource().setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return pool.getSource().getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return pool.getSource().getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return pool.getSource().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return pool.getSource().isWrapperFor(iface);
    }
}
