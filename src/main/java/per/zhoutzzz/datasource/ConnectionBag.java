package per.zhoutzzz.datasource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author zhoutzzz
 */

@RequiredArgsConstructor
@Slf4j
public class ConnectionBag {

    private final ReentrantLock lock = new ReentrantLock();

    private final AtomicBoolean shutdownStatus = new AtomicBoolean(false);

    private static final ArrayBlockingQueue<MyProxyConnection> ACTIVE_LINK_QUEUE = new ArrayBlockingQueue<>(2);

    private static final ArrayBlockingQueue<MyProxyConnection> IDLE_LINK_QUEUE = new ArrayBlockingQueue<>(2);

    private final BagConnectionListener listener;

    public Connection borrow() throws SQLException {
        MyProxyConnection conn;
        if (IDLE_LINK_QUEUE.size() > 0) {
            conn = IDLE_LINK_QUEUE.poll();
            ACTIVE_LINK_QUEUE.offer(conn);
            return conn;
        } else {
            if (shutdownStatus.get()) {
                throw new SQLException("shutdown, no connection");
            }
            MyProxyConnection myProxyConnection = listener.addBagItem();
            boolean offer = ACTIVE_LINK_QUEUE.offer(myProxyConnection);
            if (offer) {
                throw new SQLException("pool is full");
            }
            return myProxyConnection;
        }

    }

    public void requite(MyProxyConnection connection) {
        ACTIVE_LINK_QUEUE.remove(connection);
        IDLE_LINK_QUEUE.offer(connection);
    }

    void add(MyProxyConnection conn) throws ConnectException {
        if (ACTIVE_LINK_QUEUE.size() < 16) {
            ACTIVE_LINK_QUEUE.offer(conn);
        } else {
            throw new ConnectException("connection is busy, no more connection available.");
        }
    }

    public void clean() {
        boolean b = false;
        try {
            b = lock.tryLock(2, TimeUnit.SECONDS);
            if (b) {
                while (!shutdownStatus.compareAndSet(false, true)) {
                    log.info("shutdown");
                }
                for (MyProxyConnection proxyConnection : IDLE_LINK_QUEUE) {
                    proxyConnection.remove();
                }
                IDLE_LINK_QUEUE.clear();
                while (ACTIVE_LINK_QUEUE.size() > 0) {
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
