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
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author zhoutzzz
 */

@Slf4j
public class ConnectionBag {

    private final ReentrantLock lock = new ReentrantLock();

    private final AtomicBoolean shutdownStatus = new AtomicBoolean(false);

    private final ArrayBlockingQueue<MyProxyConnection> activeLinkQueue;

    private final ArrayBlockingQueue<MyProxyConnection> idleLinkQueue;

    private final BagConnectionListener listener;

    private static final Integer DEFAULT_SIZE = Runtime.getRuntime().availableProcessors();

    private static final Long DEFAULT_TIMEOUT_MILLS = 300000L;

    private final Integer maxPoolSize;

    private final Long connectionTimeoutMills;


    public ConnectionBag(BagConnectionListener listener, PoolConfig config) {
        this.listener = listener;
        this.activeLinkQueue = new ArrayBlockingQueue<>(DEFAULT_SIZE << 1);
        this.idleLinkQueue = new ArrayBlockingQueue<>(DEFAULT_SIZE);
        this.maxPoolSize = config.getMaxPoolSize() == null ? DEFAULT_SIZE : config.getMaxPoolSize();
        this.connectionTimeoutMills = config.getConnectionTimeoutMills() == null ? DEFAULT_TIMEOUT_MILLS : config.getConnectionTimeoutMills();
    }

    public ConnectionBag(BagConnectionListener listener) {
        this.listener = listener;
        activeLinkQueue = new ArrayBlockingQueue<>(Runtime.getRuntime().availableProcessors() << 1);
        idleLinkQueue = new ArrayBlockingQueue<>(Runtime.getRuntime().availableProcessors());
        this.maxPoolSize = DEFAULT_SIZE;
        this.connectionTimeoutMills = DEFAULT_TIMEOUT_MILLS;
    }

    public Connection borrow(long timeout, TimeUnit unit) throws SQLException {
        MyProxyConnection conn;
        if (idleLinkQueue.size() > 0) {
            try {
                if (timeout <= 0) {
                    conn = idleLinkQueue.poll();
                } else {
                    conn = idleLinkQueue.poll(timeout, unit);
                }
                activeLinkQueue.offer(conn);
            } catch (InterruptedException e) {
                throw new SQLTimeoutException("get connection timeout");
            }
        } else {
            if (shutdownStatus.get()) {
                throw new SQLException("shutdown, no connection");
            }
            if (activeLinkQueue.size() == idleLinkQueue.size()) {
                throw new SQLException("pool is full");
            }
            conn = listener.addBagItem();
            boolean offer = activeLinkQueue.offer(conn);
            if (!offer) {
                conn.close();
                throw new SQLException("pool is full");
            }
        }
        return conn;

    }

    public void requite(MyProxyConnection connection) {
        activeLinkQueue.remove(connection);
        idleLinkQueue.offer(connection);
    }

    void add(MyProxyConnection conn) throws ConnectException {
        idleLinkQueue.offer(conn);
    }

    public void clean() {
        boolean b = false;
        try {
            b = lock.tryLock(2, TimeUnit.SECONDS);
            if (b) {
                while (!shutdownStatus.compareAndSet(false, true)) {
                    log.info("shutdown");
                }
                for (MyProxyConnection proxyConnection : idleLinkQueue) {
                    proxyConnection.remove();
                }
                idleLinkQueue.clear();
                while (activeLinkQueue.size() > 0) {
                    log.info("waiting active connection close");
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

    public interface BagConnectionListener {
        MyProxyConnection addBagItem();
    }
}
