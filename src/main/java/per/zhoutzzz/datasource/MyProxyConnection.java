package per.zhoutzzz.datasource;

import lombok.RequiredArgsConstructor;

import java.io.Closeable;
import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @author zhoutzzz
 */
@RequiredArgsConstructor
public class MyProxyConnection implements Connection, Closeable {

    private final Connection currentConnection;

    private final ConnectionBag bag;

//    public Connection getConnection() {
//        return currentConnection;
//    }

    @Override
    public Statement createStatement() throws SQLException {
        return currentConnection.createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return currentConnection.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return currentConnection.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return currentConnection.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        currentConnection.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return currentConnection.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        currentConnection.commit();
    }

    @Override
    public void rollback() throws SQLException {
        currentConnection.rollback();
    }

    public void close() {
        bag.requite(this);
    }

    @Override
    public boolean isClosed() throws SQLException {
        return currentConnection.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return currentConnection.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        currentConnection.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return currentConnection.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        currentConnection.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        return currentConnection.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        currentConnection.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return currentConnection.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return currentConnection.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        currentConnection.clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return currentConnection.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return currentConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return currentConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return currentConnection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        currentConnection.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        currentConnection.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        return currentConnection.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return currentConnection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return currentConnection.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        currentConnection.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        currentConnection.releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return currentConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return currentConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return currentConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return currentConnection.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return currentConnection.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return currentConnection.prepareStatement(sql, columnNames);
    }

    @Override
    public Clob createClob() throws SQLException {
        return currentConnection.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return currentConnection.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return currentConnection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return currentConnection.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return currentConnection.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        currentConnection.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        currentConnection.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return currentConnection.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return currentConnection.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return currentConnection.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return currentConnection.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {

    }

    @Override
    public String getSchema() throws SQLException {
        return currentConnection.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {

    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {

    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return 0;
    }

    public void remove() {
        try {
            currentConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return currentConnection.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
