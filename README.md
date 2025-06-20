                                                     Order Management System - Design Write-up
 Objective:
 To build a simplified Order Management System (OMS) that receives orders from upstream systems and sends them to an exchange, adhering to time-based constraints, per-second throttling, order queue management 
 (Modify/Cancel support), and round-trip latency tracking.
 
 Key Design Components:
 1. Trading Window Control  - The OMS can only send orders between a configurable time window (e.g., 10 AM to 1 PM IST).
                            - At the start time, sendLogon() is called.
                            - At the end time, sendLogout() is called.
                            - Orders outside this window are rejected.
 2. Throttling Orders - OMS sends a maximum of X orders per second (e.g., 100).
                      - Orders exceeding the limit are added to a queue and processed in the next available time slot.
                      - A ScheduledExecutorService runs every second to reset the counter and send queued orders.
 3. Order Queue Management- Modify: If the order exists in the queue, its price and quantity are updated.
                          - Cancel: If the order exists in the queue, it is removed.
                          - If the order is not in the queue, these requests are ignored.
 4. Latency Tracking- A map stores timestamps for each order sent.- When the response arrives, the latency is computed as the difference between current time and the sent timestamp.
                    - Latency data is logged with orderId and responseType.
    
 Architecture and Data Structures:- Concurrent Queue: Used to store orders exceeding the per-second threshold.
                                  - HashMap: Used for tracking order timestamps and matching Modify/Cancel requests.
                                  - ScheduledExecutorService: Manages periodic tasks for logon/logout and order flushing.
                                  
 Assumptions Made:- System time is in sync with the exchange.
                  - Upstream system and exchange integrations are already implemented.
                  - Order IDs are unique and identify a single order.
                  - No third-party libraries are used; only core Java.
                  
 Testing Strategy:- Unit tests simulate the following:
                  - Submitting orders within/outside trading hours
                  - Modifying and canceling queued orders
                  - Throttling logic
                  - Response latency tracking
                  
 Conclusion:
 This OMS provides a clean and efficient solution for handling high-frequency order submissions under real-world constraints. It is modular, scalable, and easy to extend with additional features such as priority-based 
 queues, persistence layers, or real-time dashboards.
