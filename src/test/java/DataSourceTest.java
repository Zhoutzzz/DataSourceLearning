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

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import per.zhoutzzz.datasource.MyDataSource;
import per.zhoutzzz.datasource.config.PoolConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhoutzzz
 */
// shardingsphere函数支持程度
public class DataSourceTest {
    public static void main(String[] args) throws Exception {
        MyDataSource myDataSource = createDs();
        testClose(myDataSource);
//        HikariDataSource hikariDataSource = new HikariDataSource();
//        hikariDataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/demo_ds_0?useSSL=false");
//        hikariDataSource.setUsername("postgres");
//        hikariDataSource.setPassword("postgres");
//        HikariDataSource hikariDataSource1 = new HikariDataSource();
//        hikariDataSource1.setJdbcUrl("jdbc:mysql://localhost:3306/study?useSSL=false");
//        hikariDataSource1.setUsername("root");
//        hikariDataSource1.setPassword("root");
//        try {
//            Connection connection = hikariDataSource.getConnection();
//            PreparedStatement preparedStatement = connection.prepareStatement("select * from t_order_0 where user_id = ?");
//            Connection connection1 = hikariDataSource1.getConnection();
//            PreparedStatement preparedStatement1 = connection1.prepareStatement("select * from tests where id = ?");
////            connection.setAutoCommit(false);
//            Scanner sc = new Scanner(System.in);
//            while (sc.hasNext()) {
//                // pg
//                preparedStatement.setString(1, sc.nextLine());
//                for (int i = 0; i < 5; i++) {
//                    System.out.println(i);
//                    ResultSet resultSet = preparedStatement.executeQuery();
//                }
//
//                connection.prepareStatement("ALTER TABLE t_order_0 ALTER COLUMN user_id TYPE varchar(100);\n").execute();
//                for (int i = 5; i < 10; i++) {
//                    System.out.println(i);
//                    try {
//                        ResultSet resultSetx = preparedStatement.executeQuery();
//                    } catch (Exception e) {
//                        System.out.println(e.getMessage());
//                    }
//                }
//
                // mysql
//                preparedStatement1.setInt(1, sc.nextInt());
//                ResultSet resultSet1 = preparedStatement1.executeQuery();
//                while (resultSet1.next()) {
//                    System.out.println(Thread.currentThread().getName() + "@" + connection1 + " -> " + resultSet1.getObject(1) + ":" + resultSet1.getObject(2));
//                }
//                connection1.prepareStatement("ALTER TABLE tests MODIFY COLUMN id int(100);\n").execute();
//                ResultSet resultSet1x = preparedStatement1.executeQuery();
//                while (resultSet1x.next()) {
//                    System.out.println(Thread.currentThread().getName() + "@" + connection1 + " -> " + resultSet1x.getObject(1) + ":" + resultSet1x.getObject(2));
//                }
//            }
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }

    }

    private static MyDataSource createDs() throws Exception {
        PoolConfig.PoolConfigBuilder config = PoolConfig.builder();
        config.username("root");
        config.password("root");
        config.jdbcUrl("jdbc:mysql://localhost:3306/study?useSSL=false");
        config.maxPoolSize(10);
        config.minIdle(4);
        config.connectionTimeoutMills(3000L);

        return new MyDataSource(config.build());
    }

    private static void testClose(MyDataSource myDataSource) {
//        for (int i = 0; i < 200; i++) {
//            new Thread(new Task(myDataSource)).start();
//        }
//
//        try {
//            Thread.sleep(10000L);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        try {
            Connection connection1 = myDataSource.getConnection();
            PreparedStatement preparedStatement1 = connection1.prepareStatement("select * from tests");
            preparedStatement1.setInt(1, 1);
            ResultSet resultSet1 = preparedStatement1.executeQuery();
            while (resultSet1.next()) {
                System.out.println(Thread.currentThread().getName() + "@" + connection1 + " -> " + resultSet1.getObject(1) + ":" + resultSet1.getObject(2));
            }
            System.out.println("线程创建完成");
            myDataSource.close();
        } catch (Exception e) {
            e.printStackTrace();
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
//                while (true) {
//                    try {
//                        Connection connection = hikariDataSource.getConnection();
//                        PreparedStatement preparedStatement = connection.prepareStatement("select * from tests");
//                        ResultSet resultSet = preparedStatement.executeQuery();
//                        while (resultSet.next()) {
//                            System.out.println(Thread.currentThread().getName() + "@" + connection + " -> " + resultSet.getObject(1) + ":" + resultSet.getObject(2));
//                        }
//                        connection.close();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }).start();
//
//        }
//    }
}

@RequiredArgsConstructor
class Task implements Runnable {

    private final MyDataSource myDataSource;

    @Override
    public void run() {
        int count = 0;
//        while (true) {
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
                System.out.println(Thread.currentThread().getName() + "@" + connection + " -> " + resultSet.getObject(1) + ":" + resultSet.getObject(2));
            }
            connection.close();
            Thread.yield();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
//        }
    }
}