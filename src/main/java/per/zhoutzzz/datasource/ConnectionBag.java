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

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.SECONDS;

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

    public ConnectionBag(BagConnectionListener listener, Integer maxPoolSize, Integer minIdle) {
        this.listener = listener;
        this.connectionList = new CopyOnWriteArrayList<>();
    }

    public Connection borrow(long timeout, TimeUnit unit) throws SQLException {
        if (shutdownStatus.get()) {
            Thread.currentThread().interrupt();
            return null;
        }
        MyProxyConnection conn = null;
        long startTime = System.nanoTime();
        waiters.incrementAndGet();
        do {
            if (connectionList.size() > 0) {
                try {
                    conn = connectionList.remove(connectionList.size() - 1);
                } catch (Exception e) {
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
        connectionList.add(connection);
    }

    void add(MyProxyConnection conn) {
        connectionList.add(conn);
    }

    public void clean() {
        boolean b = false;
        try {
            b = lock.tryLock(2, SECONDS);
            if (b) {
                while (!shutdownStatus.compareAndSet(false, true)) {
                    System.out.println("shutdown");
                }
                while (connectionList.size() > 0) {
                    for (MyProxyConnection proxyConnection : connectionList) {
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

    public interface BagConnectionListener {
        Future<Boolean> addBagItem();
    }
}
