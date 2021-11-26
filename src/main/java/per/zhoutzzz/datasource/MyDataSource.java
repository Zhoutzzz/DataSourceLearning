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

import per.zhoutzzz.datasource.config.PoolConfig;
import per.zhoutzzz.datasource.pool.MyConnectionPool;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * @author zhoutzzz
 */
public class MyDataSource implements DataSource {

    private final MyConnectionPool pool;

    private final AtomicBoolean isClose = new AtomicBoolean(false);

    public MyDataSource(PoolConfig config) throws Exception {
        this.pool = new MyConnectionPool(config);
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (isClose.get()) {
            throw new SQLException("DataSource has been closed");
        }
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

    public void close() {
        if (isClose.getAndSet(true)) {
            return;
        }
        pool.shutdown();
        System.out.println("close DataSource");
    }
}
