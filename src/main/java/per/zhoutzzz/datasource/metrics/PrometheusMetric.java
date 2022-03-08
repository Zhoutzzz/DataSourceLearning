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

/**
 * @author zhoutzzz
 */
public class PrometheusMetric extends SimpleCollector<PrometheusMetric> {

    ConnectionBag bag;

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
        List<MetricFamilySamples> list = new ArrayList<>();
        List<MetricFamilySamples.Sample> list1 = new ArrayList<>();
        MetricFamilySamples.Sample sample = new MetricFamilySamples.Sample("activeConnection", new ArrayList<>() {{
            add("active_connection");
        }}, new ArrayList<>() {{
            add(String.valueOf(bag.values(ConnectionBag.ConnectionState.NOT_USE_STATE).size()));
        }}, 1.0);
        list1.add(sample);
        MetricFamilySamples metricFamilySamples = new MetricFamilySamples("connection_pool_total_connection", Type.COUNTER, "Total connection.", list1);
        list.add(metricFamilySamples);
        return list;
    }

    static class PrometheusMetricBuild extends Builder<PrometheusMetric.PrometheusMetricBuild, PrometheusMetric> {
        @Override
        public PrometheusMetric create() {
            return new PrometheusMetric(this);
        }
    }
}
