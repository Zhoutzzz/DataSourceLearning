package per.zhoutzzz.datasource;

import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.net.ConnectException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author zhoutzzz
 */
@RequiredArgsConstructor
public class MyConnectionPool implements ConnectionBag.BagConnectionListener {

    private final ConnectionBag bag;

    private final DataSource source;

    public MyConnectionPool(Properties dataSourceProp) throws Exception {
        this.source = new DriverSource(dataSourceProp);
        this.bag = new ConnectionBag(this);
        initConnection();
    }

    private void initConnection() throws SQLException, ConnectException {
        Connection connection = source.getConnection();
        MyProxyConnection proxyConnection = new MyProxyConnection(connection, this.bag);
        bag.add(proxyConnection);
    }

    public Connection getConnection() throws SQLException {
        return bag.borrow();
    }

    public DataSource getSource() {
        return this.source;
    }

    @Override
    public MyProxyConnection addBagItem() {
        MyProxyConnection conn = null;
        try {
            Connection connection = source.getConnection();
            conn = new MyProxyConnection(connection, this.bag);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    public void shutdown() {
        this.bag.clean();
    }
}
