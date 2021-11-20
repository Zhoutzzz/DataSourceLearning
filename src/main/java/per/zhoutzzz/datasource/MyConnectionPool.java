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
import java.sql.SQLTimeoutException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhoutzzz
 */
@RequiredArgsConstructor
public class MyConnectionPool implements ConnectionBag.BagConnectionListener {

    private final ConnectionBag bag;

    private final DataSource source;

    private final AtomicInteger totalConnections;

    private PoolConfig config;

    private final ExecutorService connectionCreator = createThreadExecutor();

    private final ConnectionCreator createTask = new ConnectionCreator();

    public MyConnectionPool(PoolConfig config) throws Exception {
        this.source = new DriverSource(config.getUsername(), config.getPassword(), config.getJdbcUrl());
        this.bag = new ConnectionBag(this, config.getMaxPoolSize(), config.getMinIdle());
        this.config = config;
        this.totalConnections = new AtomicInteger(0);
        this.initConnection();
    }

    private void initConnection() throws ConnectException {
        while (!addBagItem()) ;
    }

    public Connection getConnection() throws SQLException {
        return this.getConnection(0, TimeUnit.SECONDS);
    }

    public Connection getConnection(long timeout, TimeUnit unit) throws SQLException {
        var startTime = System.currentTimeMillis();
        do {
            Connection conn = bag.borrow(timeout, unit);
            if (conn == null) {
                continue;
            }
            return conn;
        } while (startTime - System.currentTimeMillis() < config.getConnectionTimeoutMills());
        throw new SQLTimeoutException("get connection timeout;");
    }

    public DataSource getSource() {
        return this.source;
    }

    @Override
    public Boolean addBagItem() {
        try {
            if (totalConnections.get() < config.getMaxPoolSize()) {
                totalConnections.incrementAndGet();
                System.out.println("开始创建连接,此时线程为 -> " + Thread.currentThread().getName() + "，此时总数为 -> " + totalConnections.get());
                Future<MyProxyConnection> submit = connectionCreator.submit(createTask);
                if (submit.isDone()) {
                    this.bag.add(new MyProxyConnection(submit.get(), this.bag));
                }
                Connection connection = source.getConnection();
                return Boolean.TRUE;
            }
        } catch (InterruptedException | ExecutionException | SQLException e) {
            e.printStackTrace();
        }
        return Boolean.FALSE;
    }

    public void shutdown() {
        this.bag.clean();
    }

    private static ExecutorService createThreadExecutor() {
        return new ThreadPoolExecutor(1, 1, 3000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), r -> {
            Thread thread = new Thread(r, "create-connection-thread");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.DiscardPolicy());
    }

    private class ConnectionCreator implements Callable<MyProxyConnection> {

        @Override
        public MyProxyConnection call() throws Exception {
            return new MyProxyConnection(source.getConnection(), bag);
        }
    }
}
