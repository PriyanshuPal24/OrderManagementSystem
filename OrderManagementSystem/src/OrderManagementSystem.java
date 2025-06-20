import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public class OrderManagementSystem {
    public static void main(String[] args) throws InterruptedException {
        OrderManagementSystem system = new OrderManagementSystem();

        system.onOrderRequest(new OrderRequest(101, 250.5, 10, 'B', 1001, RequestType.New));
        system.onOrderRequest(new OrderRequest(101, 251.0, 5, 'B', 1002, RequestType.New));
        system.onOrderRequest(new OrderRequest(101, 249.0, 3, 'S', 1003, RequestType.New));

        system.onOrderRequest(new OrderRequest(101, 260.0, 15, 'B', 1002, RequestType.Modify));
        system.onOrderRequest(new OrderRequest(101, 0, 0, 'B', 1003, RequestType.Cancel));

        Thread.sleep(2000);
        system.onOrderResponse(new OrderResponse(1001, ResponseType.Accept));
        system.onOrderResponse(new OrderResponse(1002, ResponseType.Reject));
    }

    private final LocalTime START_TIME = LocalTime.of(10, 0);
    private final LocalTime END_TIME = LocalTime.of(13, 0);
    
    private final int MAX_ORDERS_PER_SECOND = 100;

    private final Queue<OrderRequest> pendingOrders = new ConcurrentLinkedQueue<>();
    private final Map<Long, Long> sentOrderTimestamps = new ConcurrentHashMap<>();

    private int ordersSentThisSecond = 0;
    private long currentSecondTimestamp = -1;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean logonSent = false;

    public OrderManagementSystem() {
        scheduler.scheduleAtFixedRate(this::processQueuedOrders, 0, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::checkLogonLogoutStatus, 0, 1, TimeUnit.SECONDS);
    }

    public synchronized void onOrderRequest(OrderRequest request) {
        if (!isWithinTradingHours()) {
            System.out.println("Rejected: Outside trading window");
            return;
        }

        switch (request.requestType) {
            case Modify:
                for (OrderRequest queuedOrder : pendingOrders) {
                    if (queuedOrder.orderId == request.orderId) {
                        queuedOrder.price = request.price;
                        queuedOrder.quantity = request.quantity;
                        return;
                    }
                }
                break;
            case Cancel:
                pendingOrders.removeIf(queuedOrder -> queuedOrder.orderId == request.orderId);
                return;
            case New:
                long currentSecond = System.currentTimeMillis() / 1000;
                if (currentSecond != currentSecondTimestamp) {
                    currentSecondTimestamp = currentSecond;
                    ordersSentThisSecond = 0;
                }

                if (ordersSentThisSecond < MAX_ORDERS_PER_SECOND) {
                    sendToExchange(request);
                    sentOrderTimestamps.put(request.orderId, System.currentTimeMillis());
                    ordersSentThisSecond++;
                } else {
                    pendingOrders.offer(request);
                }
                break;
            default:
                System.out.println("Unknown request type. Ignored.");
        }
    }

    public void onOrderResponse(OrderResponse response) {
        long sentTime = sentOrderTimestamps.getOrDefault(response.orderId, -1L);
        if (sentTime != -1) {
            long latency = System.currentTimeMillis() - sentTime;
            logOrderResponse(response, latency);
        }
    }

    private void processQueuedOrders() {
        long currentSecond = System.currentTimeMillis() / 1000;
        if (currentSecond != currentSecondTimestamp) {
            currentSecondTimestamp = currentSecond;
            ordersSentThisSecond = 0;
        }

        while (!pendingOrders.isEmpty() && ordersSentThisSecond < MAX_ORDERS_PER_SECOND) {
            OrderRequest request = pendingOrders.poll();
            if (request != null) {
                sendToExchange(request);
                sentOrderTimestamps.put(request.orderId, System.currentTimeMillis());
                ordersSentThisSecond++;
            }
        }
    }

    private void checkLogonLogoutStatus() {
        boolean withinTrading = isWithinTradingHours();
        if (withinTrading && !logonSent) {
            sendLogon();
            logonSent = true;
        } else if (!withinTrading && logonSent) {
            sendLogout();
            logonSent = false;
        }
    }

    private boolean isWithinTradingHours() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(START_TIME) && !now.isAfter(END_TIME);
    }

    private void logOrderResponse(OrderResponse response, long latency) {
        System.out.printf("Logged: OrderId=%d, Response=%s, Latency=%dms%n",
                response.orderId, response.responseType, latency);
    }

    public void sendToExchange(OrderRequest request) {
        System.out.println("Sent to exchange: " + request.orderId);
    }

    public void sendLogon() {
        System.out.println("Logon sent to exchange");
    }

    public void sendLogout() {
        System.out.println("Logout sent to exchange");
    }

    static class OrderRequest {
        int symbolId;
        double price;
        long quantity;
        char side;
        long orderId;
        RequestType requestType;

        public OrderRequest(int symbolId, double price, long quantity, char side, long orderId, RequestType requestType) {
            this.symbolId = symbolId;
            this.price = price;
            this.quantity = quantity;
            this.side = side;
            this.orderId = orderId;
            this.requestType = requestType;
        }
    }

    static class OrderResponse {
        long orderId;
        ResponseType responseType;

        public OrderResponse(long orderId, ResponseType responseType) {
            this.orderId = orderId;
            this.responseType = responseType;
        }
    }

    enum RequestType { Unknown, New, Modify, Cancel }
    enum ResponseType { Unknown, Accept, Reject }
}
