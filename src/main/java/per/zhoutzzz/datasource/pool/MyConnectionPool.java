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

package per.zhoutzzz.datasource.pool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import per.zhoutzzz.datasource.DriverSource;
import per.zhoutzzz.datasource.config.PoolConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhoutzzz
 */
@RequiredArgsConstructor
@Slf4j
public class MyConnectionPool implements ConnectionBag.BagConnectionListener {

    private final ConnectionBag bag;

    private final DataSource source;

    private final AtomicInteger totalConnections;

    private PoolConfig config;

    private final ExecutorService connectionCreator = createThreadExecutor();

    private final ScheduledExecutorService keepAliveExecutor = new ScheduledThreadPoolExecutor(1);

    private final ConnectionCreator createTask = new ConnectionCreator();

    private final ScheduledExecutorService leakTaskExecutor = new ScheduledThreadPoolExecutor(1);

    private ScheduledFuture<?> leakFuture;

    private static final int INIT_VALUE = 0, INIT_DELAY = 0;

    public MyConnectionPool(PoolConfig config) throws Exception {
        this.source = new DriverSource(config.getUsername(), config.getPassword(), config.getJdbcUrl());
        this.bag = new ConnectionBag(this, config.getMaxPoolSize(), config.getMinIdle());
        this.config = config;
        this.totalConnections = new AtomicInteger(INIT_VALUE);
        keepAliveExecutor.scheduleWithFixedDelay(new KeepAliveTask(), INIT_DELAY, 30, TimeUnit.SECONDS);
        leakFuture = leakTaskExecutor.schedule(new LeakTask(), INIT_DELAY, TimeUnit.SECONDS);
        this.initConnection();
    }

    private void initConnection() {
        addBagItem();
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
    public Future<Boolean> addBagItem() {
        return connectionCreator.submit(createTask);
    }

    public void shutdown() {
        this.bag.clean();
        leakFuture.cancel(false);
        this.leakTaskExecutor.shutdown();
        this.keepAliveExecutor.shutdown();
        this.connectionCreator.shutdown();
    }

    private static ExecutorService createThreadExecutor() {
        return new ThreadPoolExecutor(1, 1, 3000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), r -> {
            Thread thread = new Thread(r, "create-connection-thread");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.DiscardPolicy());
    }

    private class ConnectionCreator implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            Connection newConn = null;
            try {
                if (totalConnections.get() < config.getMaxPoolSize()) {
                    totalConnections.incrementAndGet();
                    log.debug("开始创建连接,此时线程为 -> {}，此时总数为 -> {}", Thread.currentThread().getName(), totalConnections.get());
                    newConn = source.getConnection();
                    Class<?> proxyConn = this.getClass().getClassLoader().loadClass(MyProxyConnection.class.getPackageName() + ".MyProxyConnection$ByteBuddy");
                    bag.add((MyProxyConnection) proxyConn.getDeclaredConstructor().newInstance());
                    return Boolean.TRUE;
                }
            } catch (Exception e) {
                if (newConn != null) {
                    newConn.setNetworkTimeout(Executors.newSingleThreadExecutor(), 5000);
                }
            }
            return Boolean.FALSE;
        }
    }

    private class KeepAliveTask implements Runnable {

        @Override
        public void run() {
            log.debug("开始处理空闲线程");
            Collection<MyProxyConnection> idleConnList = bag.values(ConnectionBag.ConnectionState.NOT_USE_STATE);
            int removable = idleConnList.size() - config.getMinIdle();
            if (removable <= 0) {
                return;
            }
            for (MyProxyConnection curConn : idleConnList) {
                curConn.remove();
                if (--removable <= 0) {
                    break;
                }
            }
            for (MyProxyConnection each : idleConnList) {
                if (each.getState() == ConnectionBag.ConnectionState.REMOVE_STATE) {
                    continue;
                }
                try (PreparedStatement statement = each.prepareStatement("select 1")) {
                    statement.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class LeakTask implements Runnable {

        @Override
        public void run() {
            bag.evict();
        }
    }
}
