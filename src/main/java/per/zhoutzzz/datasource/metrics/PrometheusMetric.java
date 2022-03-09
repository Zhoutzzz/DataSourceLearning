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

import io.prometheus.client.SimpleCollector;
import per.zhoutzzz.datasource.pool.ConnectionBag;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zhoutzzz
 */
public class PrometheusMetric extends SimpleCollector<PrometheusMetric> {

    private ConnectionBag bag;

    private static final ScheduledExecutorService LABEL_EXECUTOR = new ScheduledThreadPoolExecutor(1, new ThreadPoolExecutor.AbortPolicy());
    ;

    private PrometheusMetric(Builder b) {
        super(b);
        this.register();
    }

    public static PrometheusMetricBuild build() {
        return new PrometheusMetricBuild();
    }

    @Override
    protected PrometheusMetric newChild() {
        return PrometheusMetric.build().create();
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> samples = new ArrayList<>();
        List<MetricFamilySamples.Sample> sampleItems = new ArrayList<>();
        List<String> labelValues = new ArrayList<>();
        labelValues.add(String.valueOf(bag.values(ConnectionBag.ConnectionState.NOT_USE_STATE).size()));

        LABEL_EXECUTOR.scheduleAtFixedRate(() -> {
            labelValues.clear();
            String count = String.valueOf(bag.values(ConnectionBag.ConnectionState.NOT_USE_STATE).size());
            labelValues.add(count);
        },30000, 30000, TimeUnit.MILLISECONDS);

        MetricFamilySamples.Sample sample = new MetricFamilySamples.Sample("activeConnection", new ArrayList<>() {{
            add("active_connection");
        }}, labelValues, 1.0);
        sampleItems.add(sample);
        MetricFamilySamples metricFamilySamples = new MetricFamilySamples("connection_pool_total_connection", Type.COUNTER, "Total connection.", sampleItems);
        samples.add(metricFamilySamples);
        return samples;
    }

    static class PrometheusMetricBuild extends Builder<PrometheusMetric.PrometheusMetricBuild, PrometheusMetric> {
        @Override
        public PrometheusMetric create() {
            return new PrometheusMetric(this);
        }
    }
}
