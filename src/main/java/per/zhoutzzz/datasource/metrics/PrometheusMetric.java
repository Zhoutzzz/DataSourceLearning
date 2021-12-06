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

package per.zhoutzzz.datasource.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

/**
 * @author zhoutzzz
 */
public class PrometheusMetric {
    private static final CollectorRegistry REGISTRY = new CollectorRegistry();

    public void counter() {
        Counter build = Counter.build("connection-pool.total-connection", "Total connection.").register(REGISTRY);
        build.inc();
    }

    public void gauge() {
        Gauge build = Gauge.build("connection-pool.total-connection", "Total connection.").register(REGISTRY);
        build.startTimer();
        build.inc();
    }
}
