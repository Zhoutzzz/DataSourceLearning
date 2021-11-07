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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import per.zhoutzzz.datasource.MyDataSource;
import per.zhoutzzz.datasource.PoolConfig;
//import per.zhoutzzz.datasource.MyDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhoutzzz
 */

public class DataSourceTest {

    public static void main(String[] args) throws Exception {
        PoolConfig.PoolConfigBuilder config = PoolConfig.builder();
        config.username("root");
        config.password("root");
        config.jdbcUrl("jdbc:mysql://localhost:3306/study?useSSL=false");
        config.maxPoolSize(10);
        config.minIdle(4);
        config.connectionTimeoutMills(3000L);

        MyDataSource myDataSource = new MyDataSource(config.build());

        for (int i = 0; i < 20; i++) {
            new Thread(new Task(myDataSource)).start();
        }
    }

//    public static void main(String[] args) throws Exception {
//        HikariConfig hikariConfig = new HikariConfig();
//        hikariConfig.setJdbcUrl("jdbc:mysql://localhost:3306/study?useSSL=false");
//        hikariConfig.setUsername("root");
//        hikariConfig.setPassword("root");
//        hikariConfig.setMaximumPoolSize(2);
//        HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
//        for (int i = 0; i < 10; i++) {
//            new Thread(() -> {
//                try {
//                    Connection connection = hikariDataSource.getConnection();
//                    PreparedStatement preparedStatement = connection.prepareStatement("select * from tests");
//                    ResultSet resultSet = preparedStatement.executeQuery();
//                    while (resultSet.next()) {
//                        System.out.println(Thread.currentThread().getName() + "@" + connection + " -> " + resultSet.getObject(1) + ":" + resultSet.getObject(2));
//                    }
//                    connection.close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }).start();
//
//        }
//    }
}

@RequiredArgsConstructor
class Task implements Runnable{

    private final MyDataSource myDataSource;

    @Override
    public void run() {
        int count = 0;
        while (true) {
            try {
                Connection connection;
                do {
                    connection = myDataSource.getConnection();
                } while (connection == null && ++count < 3);
                if (count == 3) {
                    System.out.println(Thread.currentThread().getName() + " -> can't get connection, retry acquire connection.");
                    return;
                }
                PreparedStatement preparedStatement = connection.prepareStatement("select * from tests");
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
//                    System.out.println(Thread.currentThread().getName() + "@" + connection + " -> " + resultSet.getObject(1) + ":" + resultSet.getObject(2));
                }
                connection.close();
                Thread.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}