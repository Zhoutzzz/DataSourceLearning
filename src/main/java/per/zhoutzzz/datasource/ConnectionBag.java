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

import java.net.ConnectException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.SECONDS;

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

    private static final Integer DEFAULT_MAX_SIZE = Runtime.getRuntime().availableProcessors();

    private static final Integer DEFAULT_IDLE_SIZE = 1;

    private final int poolActiveSize;

    private final AtomicInteger allConnectionSize = new AtomicInteger(0);

    private final AtomicInteger waiters = new AtomicInteger(0);

    public ConnectionBag(BagConnectionListener listener, Integer maxPoolSize, Integer minIdle) {
        int poolMinIdle = Objects.isNull(maxPoolSize) ? DEFAULT_IDLE_SIZE : minIdle;
        poolActiveSize = Objects.isNull(maxPoolSize) ? DEFAULT_MAX_SIZE : maxPoolSize;
        this.listener = listener;
        this.activeLinkQueue = new ArrayBlockingQueue<>(poolActiveSize);
        this.idleLinkQueue = new ArrayBlockingQueue<>(poolMinIdle);
    }

    public Connection borrow(long timeout, TimeUnit unit) throws SQLException {
        MyProxyConnection conn = null;
        if (allConnectionSize.get() < poolActiveSize && idleLinkQueue.isEmpty()) {
            conn = listener.addBagItem();
            allConnectionSize.incrementAndGet();
            while (!activeLinkQueue.offer(conn)) ;
        } else if (idleLinkQueue.isEmpty()) {
            return null;
        } else {
            try {
                conn = idleLinkQueue.poll(timeout, unit);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return conn;
    }

    public void requite(MyProxyConnection connection) {
        boolean isRemove;
        do {
            isRemove = activeLinkQueue.remove(connection);
        }
        while (!isRemove);
        idleLinkQueue.offer(connection);
    }

    void add(MyProxyConnection conn) throws ConnectException {
        idleLinkQueue.add(conn);
    }

    public void clean() {
        boolean b = false;
        try {
            b = lock.tryLock(2, SECONDS);
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
