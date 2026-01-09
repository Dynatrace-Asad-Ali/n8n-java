# DB Utilities

No connection pool or resource handling code was updated for this leak fix. Connection management changes are limited to `OrderService.java` and use try-with-resources patterns directly.