package per.zhoutzzz.datasource;

import lombok.Getter;
import lombok.Setter;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Properties;

/**
 * @author zhoutzzz
 */
public class DriverSource implements DataSource {

    @Setter
    @Getter
    private String username;

    @Setter
    @Getter
    private String password;

    @Setter
    @Getter
    private String url;

    private final Driver driver;

    private final Properties variables = new Properties();

    public DriverSource(String username, String password, String url) throws Exception {
        this.username = username;
        this.password = password;
        this.url = url;
        this.driver = DriverManager.getDriver(url);
        init();
    }

    public DriverSource(Properties dataSourceProps) throws Exception {
        this(dataSourceProps.getProperty("username"), dataSourceProps.getProperty("password"), dataSourceProps.getProperty("jdbcUrl"));
    }

    private void init() {
        try {
            this.variables.setProperty("password", password);
            this.variables.setProperty("user", username);
            this.variables.setProperty("jdbcUrl", url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return driver.connect(url, variables);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Properties cloned = (Properties)this.variables.clone();
        if (username != null) {
            cloned.put("user", username);
            if (cloned.containsKey("username")) {
                cloned.put("username", username);
            }
        }

        if (password != null) {
            cloned.put("password", password);
        }

        return this.driver.connect(this.url, cloned);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException
    {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLogWriter(PrintWriter logWriter) throws SQLException
    {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException
    {
        DriverManager.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException
    {
        return DriverManager.getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        return driver.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return false;
    }
}
