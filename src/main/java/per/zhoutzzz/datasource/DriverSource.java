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

import lombok.Getter;
import lombok.Setter;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

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

    private void init() {
        try {
            this.variables.setProperty("password", password);
            this.variables.setProperty("user", username);
            this.variables.setProperty("jdbcUrl", url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final AtomicInteger i = new AtomicInteger(0);

    @Override
    public Connection getConnection() throws SQLException {
        System.out.println("创建第" + i.incrementAndGet() + "个连接");
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
