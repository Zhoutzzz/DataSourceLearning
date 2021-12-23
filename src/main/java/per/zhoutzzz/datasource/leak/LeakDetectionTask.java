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

package per.zhoutzzz.datasource.leak;

import lombok.extern.slf4j.Slf4j;
import per.zhoutzzz.datasource.pool.MyProxyConnection;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author zhoutzzz
 */
@Slf4j
public class LeakDetectionTask implements Runnable {

    private ScheduledExecutorService leakExecutor;

    private volatile int threshold;

    private Future<?> leakTaskFuture;

    private String connectionName;

    private LeakDetectionTask(String connectionName, LeakDetectionTask parent) {
        this.connectionName = connectionName;
        leakTaskFuture = parent.leakExecutor.schedule(this, parent.threshold, TimeUnit.MILLISECONDS);
    }

    public LeakDetectionTask(ScheduledExecutorService leakExecutor, int threshold) {
        this.leakExecutor = leakExecutor;
        this.threshold = threshold;
    }

    public LeakDetectionTask schedule(MyProxyConnection connection) {
        LeakDetectionTask leakDetectionTask = null;
        try {
            this.connectionName = connection.getClientInfo().entrySet().iterator().next().getValue().toString();
            leakDetectionTask = new LeakDetectionTask(connectionName, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return leakDetectionTask == null ? this : leakDetectionTask;
    }

    public void cancel() {
        leakTaskFuture.cancel(false);
    }

    @Override
    public void run() {
        log.debug("检测到泄露的连接" + connectionName);
    }
}
