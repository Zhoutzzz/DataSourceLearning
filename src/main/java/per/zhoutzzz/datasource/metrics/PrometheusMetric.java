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

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.HTTPServer;

/**
 * @author zhoutzzz
 */
public class PrometheusMetric {

    public static void main(String[] args) {
        try {
            HTTPServer server = new HTTPServer.Builder().withPort(10001)
                .build();
            Counter.build("connection_pool_total_connection", "Total connection.").register().inc();
            Gauge.build("connection_pool_start_time", "Connection Time.").register().startTimer();
            Histogram.build("connection_pool_histogram", "Connection.").register().startTimer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
