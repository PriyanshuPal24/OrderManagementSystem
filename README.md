# Order Management System - Design Write-up

## Objective
To build a simplified Order Management System (OMS) that receives orders from upstream systems and sends them to an exchange, adhering to:
- Time-based constraints
- Per-second throttling
- Order queue management (Modify/Cancel support)
- Round-trip latency tracking

---

## Key Design Components

### 1. Trading Window Control
- The OMS only sends orders during a configurable time window (e.g., 10 AM to 1 PM IST).
- At the start time, `sendLogon()` is called.
- At the end time, `sendLogout()` is called.
- Orders received outside this window are rejected.

### 2. Throttling Orders
- A maximum of `X` orders per second (e.g., 100) can be sent.
- Excess orders are queued and processed in the next available second.
- `ScheduledExecutorService` runs every second to reset the count and flush the queue.

### 3. Order Queue Management
- **Modify:** If an order exists in the queue, its price and quantity are updated.
- **Cancel:** If an order exists in the queue, it is removed.
- Modify/Cancel requests for non-queued orders are ignored.

### 4. Latency Tracking
- A `HashMap` stores timestamps for each sent order.
- When a response arrives, latency is calculated as the difference between current time and sent time.
- Latency, along with `orderId` and `responseType`, is logged.

---

## Architecture and Data Structures
- **Concurrent Queue:** Stores excess orders beyond the throttle limit.
- **HashMap:** Tracks timestamps and supports Modify/Cancel operations.
- **ScheduledExecutorService:** Periodically triggers logon/logout and flush logic.

---

## Assumptions Made
- System time is synchronized with the exchange.
- Integration with upstream systems and exchange is already handled.
- Order IDs are unique.
- No third-party libraries are used; only core Java.

---

## Testing Strategy
- Unit tests simulate:
  - Order submissions within and outside trading hours
  - Modifying and canceling queued orders
  - Order throttling behavior
  - Response latency tracking

---

## Conclusion
This OMS offers a clean and robust solution for managing high-frequency trading orders. It is modular, scalable, and can be extended to support advanced features like:
- Priority-based queues
- Persistence (e.g., logging to file or DB)
- Real-time dashboards for operational visibility
