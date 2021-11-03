/*
 * MIT License
 *
 * Copyright (c) 2021 Z
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */

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

    public MyConnectionPool(PoolConfig config) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("username", config.getUsername());
        properties.setProperty("password", config.getPassword());
        properties.setProperty("jdbcUrl", config.getJdbcUrl());
        this.source = new DriverSource(properties);
        this.bag = new ConnectionBag(this, config);
        initConnection();
    }

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
