import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import per.zhoutzzz.datasource.MyDataSource;
//import per.zhoutzzz.datasource.MyDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhoutzzz
 */

public class DataSourceTest {

    static final AtomicInteger integer = new AtomicInteger();

    public static void main(String[] args) throws Exception {
        String username = "root";
        String pwd = "root";
        String url = "jdbc:mysql://localhost:3306/study?useSSL=false";
        MyDataSource myDataSource = new MyDataSource(username, pwd, url);
        for (int i = 0; i < 40; i++) {
            new Thread(() -> {
                int count = 0;
                try {
                    Connection connection;
                    do {
                        System.out.println(Thread.currentThread().getName() + " -> can't get connection, retry acquire connection.");
                        Thread.sleep(1000L);
                        connection = myDataSource.getConnection();
                    } while (connection == null && ++count < 3);
                    if (count == 3) {
                        return;
                    }
                    PreparedStatement preparedStatement = connection.prepareStatement("select * from tests");
                    ResultSet resultSet = preparedStatement.executeQuery();
                    while (resultSet.next()) {
                        System.out.println(Thread.currentThread().getName() + "@" + connection.toString() + " -> " + resultSet.getObject(1) + ":" + resultSet.getObject(2));
                    }
                    connection.close();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }).start();
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
//
//                    Connection connection = hikariDataSource.getConnection();
//                    PreparedStatement preparedStatement = connection.prepareStatement("select * from tests");
//                    ResultSet resultSet = preparedStatement.executeQuery();
//                    while (resultSet.next()) {
//                        System.out.println(Thread.currentThread().getName() + "@" + connection.toString() + " -> " + resultSet.getObject(1) + ":" + resultSet.getObject(2));
//                    }
//                    connection.close();
//                } catch (Exception e) {
//
//                }
//            }).start();
//
//        }
//    }
}
