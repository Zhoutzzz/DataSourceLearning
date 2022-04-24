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

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author zhoutzzz
 */

@Slf4j
public class ConnectionBag {

    private final ReentrantLock lock = new ReentrantLock();

    private final AtomicBoolean shutdownStatus = new AtomicBoolean(false);

    private final CopyOnWriteArrayList<MyProxyConnection> connectionList;

    private final BagConnectionListener listener;

    private final AtomicInteger waiters = new AtomicInteger(0);

    private final static int LOCK_TIMEOUT = 2000;

    public ConnectionBag(BagConnectionListener listener, Integer maxPoolSize, Integer minIdle) {
        this.listener = listener;
        this.connectionList = new CopyOnWriteArrayList<>();
    }

    public Connection borrow(long timeout, TimeUnit unit) throws SQLException {
        if (shutdownStatus.get()) {
            throw new SQLException("连接池已经关闭");
        }
        MyProxyConnection conn = null;
        long startTime = System.nanoTime();
        waiters.incrementAndGet();
        int connIndex = connectionList.size() - 1;
        do {
            if (connIndex > 0) {
                try {
                    conn = connectionList.get(connIndex);
                    boolean b = conn.compareAndSet(ConnectionState.NOT_USE_STATE, ConnectionState.USE_STATE);
                    if (b) {
                        break;
                    }
                    connIndex--;
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.debug("超时时间内获取连接失败，继续重试");
                    }
                    continue;
                }
            }
            if (waiters.get() > 1) {
                listener.addBagItem();
            }
            timeout = unit.toNanos(timeout) - (System.nanoTime() - startTime);
        } while (timeout > 0);
        waiters.decrementAndGet();
        return conn;
    }

    public void requite(MyProxyConnection connection) {
        if (connection.getState() == ConnectionState.USE_STATE) {
            while (!connection.compareAndSet(ConnectionState.USE_STATE, ConnectionState.NOT_USE_STATE)) {
                log.debug("正在归还连接");
            }
        }
        log.debug("此连接成功归还");
    }

    void add(MyProxyConnection conn) {
        conn.lazySet(ConnectionState.NOT_USE_STATE);
        connectionList.add(conn);
    }

    public void clean() {
        boolean b = false;
        try {
            b = lock.tryLock(LOCK_TIMEOUT, MILLISECONDS);
            if (b) {
                while (!shutdownStatus.compareAndSet(false, true)) {
                    log.info("shutdown");
                }
                while (connectionList.size() > 0) {
                    for (MyProxyConnection proxyConnection : connectionList) {
                        proxyConnection.compareAndSet(ConnectionState.NOT_USE_STATE, ConnectionState.REMOVE_STATE);
                        proxyConnection.remove();
                    }
                    connectionList.clear();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (b) {
                lock.unlock();
            }
        }
    }

    public Collection<MyProxyConnection> values(final int state) {
        List<MyProxyConnection> result = new ArrayList<>(connectionList.size());
        for (final MyProxyConnection entry : connectionList) {
            if (entry.getState() == state) {
                result.add(entry);
            }
        }

        return result;
    }

    public void evict() {
        connectionList.removeIf(conn -> conn.getState() == ConnectionState.REMOVE_STATE);
    }

    public interface BagConnectionListener {
        Future<Boolean> addBagItem();
    }

    public interface ConnectionState {
        int REMOVE_STATE = -1;
        int NOT_USE_STATE = 0;
        int USE_STATE = 1;
        int RESERVE_STATE = 2;

        boolean compareAndSet(int expect, int newValue);

        void lazySet(int value);

        int getState();
    }
}
