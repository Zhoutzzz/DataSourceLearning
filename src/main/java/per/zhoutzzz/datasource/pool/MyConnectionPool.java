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
import per.zhoutzzz.datasource.exception.GetConnectionTimeoutException;
import per.zhoutzzz.datasource.leak.LeakDetectionTask;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

    private static final AtomicInteger THREAD_NUM = new AtomicInteger(0);

    private PoolConfig config;

    private final ExecutorService connectionCreator = createThreadExecutor();

    private final ScheduledExecutorService keepAliveExecutor = new ScheduledThreadPoolExecutor(1);

    private final ScheduledExecutorService leakTaskExecutor = new ScheduledThreadPoolExecutor(1);

    private final ConnectionCreator createTask = new ConnectionCreator();

    private LeakDetectionTask leakTask;

    private static final long CREATE_CONNECTION_TIMEOUT = 3000L;

    private static final int INIT_VALUE = 0, INIT_DELAY = 0, INIT_LEAK_THRESHOLD = 1000, KEEP_ALIVE = 600000;

    public MyConnectionPool(PoolConfig config) throws SQLException {
        this.source = new DriverSource(config.getUsername(), config.getPassword(), config.getJdbcUrl());
        this.bag = new ConnectionBag(this, config.getMaxPoolSize(), config.getMinIdle());
        this.config = config;
        this.totalConnections = new AtomicInteger(INIT_VALUE);
        keepAliveExecutor.scheduleWithFixedDelay(new KeepAliveTask(), INIT_DELAY, 15000, TimeUnit.MILLISECONDS);
        this.leakTask = new LeakDetectionTask(leakTaskExecutor,
            config.getLeakThreshold() == null ? INIT_LEAK_THRESHOLD : config.getLeakThreshold());
        this.initConnection();
    }

    private void initConnection() {
        try {
            Boolean isCreate = addBagItem().get(CREATE_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
            if (!isCreate)
                log.warn("初始化创建连接失败");
        } catch (Exception e) {
            this.shutdown();
            log.error(e.getMessage());
        }
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
        throw new GetConnectionTimeoutException("get connection timeout;");
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
        leakTask.cancel();
        try {
            this.keepAliveExecutor.shutdown();
            if (!keepAliveExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                this.keepAliveExecutor.shutdownNow();
                log.info("连接维护器强制关闭");
            }
            this.connectionCreator.shutdown();
            if (!connectionCreator.awaitTermination(3, TimeUnit.SECONDS)) {
                this.connectionCreator.shutdownNow();
                log.info("连接创建器强制关闭");
            }
            this.leakTaskExecutor.shutdown();
            if (!leakTaskExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                this.leakTaskExecutor.shutdownNow();
                log.info("泄漏检查器强制关闭");
            }
        } catch (Exception e) {
            log.error("关闭连接池出现问题, {}", e.getMessage());
        }
    }

    private static ExecutorService createThreadExecutor() {
        return new ThreadPoolExecutor(1, 1, KEEP_ALIVE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), r -> {
            Thread thread = new Thread(r, "create-connection-thread-" + THREAD_NUM.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.DiscardPolicy());
    }

    private class ConnectionCreator implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            Connection newConn = null;
            try {
                if (totalConnections.incrementAndGet() <= config.getMaxPoolSize()) {
                    log.debug("开始创建连接,此时线程为 -> {}，此时总数为 -> {}", Thread.currentThread().getName(), totalConnections.get());
                    newConn = source.getConnection();
                    MyProxyConnection connection = ConnectionFactory.getConnection(newConn, bag);
                    bag.add(connection);
                    if (config.getLeakThreshold() < config.getIdleTimeout()) {
                        throw new Exception("threshold is small");
                    }
                    leakTask.setThreshold(config.getLeakThreshold());
                    leakTask = leakTask.schedule(connection);
                    return Boolean.TRUE;
                }
            } catch (Exception e) {
                if (newConn != null) {
                    newConn.setNetworkTimeout(Executors.newSingleThreadExecutor(), 5000);
                }
                throw e;
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
                    leakTask.cancel();
                    break;
                }
            }
            for (MyProxyConnection each : idleConnList) {
                if (each.getState() == ConnectionBag.ConnectionState.REMOVE_STATE) {
                    continue;
                }
                try {
                    if (!each.isValid(10)) {
                        each.remove();
                    }
                } catch (SQLException e) {
                    log.error(e.getMessage());
                }
            }
        }
    }
}
