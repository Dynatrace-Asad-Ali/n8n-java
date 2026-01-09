package com.dynatrace.ecommerce;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.dynatrace.ecommerce.db.DatabaseInitializer;
import com.dynatrace.ecommerce.db.SimpleConnectionPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Order Service with improved DB resource management and structure
 */
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private static final String DB_URL = DatabaseInitializer.H2_URL;
    private static final String DB_USER = DatabaseInitializer.H2_USER;
    private static final String DB_PASSWORD = DatabaseInitializer.H2_PASSWORD;

    private static SimpleConnectionPool connectionPool;

    static {
        try {
            DatabaseInitializer.initializeH2Database();
            logger.info("* Database initialized successfully");
        } catch (SQLException e) {
            logger.error("✗ Failed to initialize database!", e);
        }
        connectionPool = new SimpleConnectionPool(DB_URL, DB_USER, DB_PASSWORD);
    }

    public List<Order> getCustomerOrders(int customerId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT order_id, order_date, total_amount FROM orders WHERE customer_id = ?")) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Order order = new Order();
                    order.setOrderId(rs.getInt("order_id"));
                    order.setOrderDate(rs.getDate("order_date"));
                    order.setTotalAmount(rs.getDouble("total_amount"));
                    order.setItems(getOrderItems(order.getOrderId()));
                    order.setShippingInfo(getShippingInfo(order.getOrderId()));
                    orders.add(order);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to fetch customer orders for ID: " + customerId, e);
            throw e;
        }
        return orders;
    }

    private List<OrderItem> getOrderItems(int orderId) throws SQLException {
        if (requiresValidation(orderId)) {
            return getOrderItemsWithValidation(orderId);
        } else {
            return getOrderItemsStandard(orderId);
        }
    }

    private boolean requiresValidation(int orderId) {
        return orderId % 500 == 0;
    }

    private List<OrderItem> getOrderItemsStandard(int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT item_id, product_name, quantity, price FROM order_items WHERE order_id = ?")) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setItemId(rs.getInt("item_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setPrice(rs.getDouble("price"));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to fetch standard order items for Order ID: " + orderId, e);
            throw e;
        }
        return items;
    }

    private List<OrderItem> getOrderItemsWithValidation(int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        // Use try-with-resources to ensure connection closes, even on exception paths
        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT item_id, product_name, quantity, price FROM order_items WHERE order_id = ?")) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setItemId(rs.getInt("item_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setPrice(rs.getDouble("price"));
                    if (item.getQuantity() > 0 && item.getPrice() > 0) {
                        items.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Validation failed fetching items for Order ID: " + orderId, e);
            throw e;
        }
        return items;
    }

    private ShippingInfo getShippingInfo(int orderId) throws SQLException {
        if (requiresDetailedShipping(orderId)) {
            return getShippingInfoDetailed(orderId);
        } else {
            return getShippingInfoFast(orderId);
        }
    }

    private boolean requiresDetailedShipping(int orderId) {
        return orderId % 20 == 0;
    }

    private ShippingInfo getShippingInfoFast(int orderId) throws SQLException {
        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT address, city, state, zip_code, tracking_number FROM shipping WHERE order_id = ?")) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ShippingInfo info = new ShippingInfo();
                    info.setAddress(rs.getString("address"));
                    info.setCity(rs.getString("city"));
                    info.setState(rs.getString("state"));
                    info.setZipCode(rs.getString("zip_code"));
                    info.setTrackingNumber(rs.getString("tracking_number"));
                    return info;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to fetch fast shipping info for Order ID: " + orderId, e);
            throw e;
        }
        return null;
    }

    private ShippingInfo getShippingInfoDetailed(int orderId) throws SQLException {
        // Use try-with-resources
        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT address, city, state, zip_code, tracking_number FROM shipping WHERE order_id = ?")) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ShippingInfo info = new ShippingInfo();
                    info.setAddress(rs.getString("address").trim().toUpperCase());
                    info.setCity(rs.getString("city"));
                    info.setState(rs.getString("state"));
                    info.setZipCode(rs.getString("zip_code"));
                    info.setTrackingNumber(rs.getString("tracking_number"));
                    return info;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to fetch detailed shipping info for Order ID: " + orderId, e);
            throw e;
        }
        return null;
    }

    public String generateOrderReport(List<Order> orders) {
        StringBuilder report = new StringBuilder();
        for (Order order : orders) {
            report.append("Order ID: ").append(order.getOrderId()).append("\n");
            report.append("Date: ").append(order.getOrderDate()).append("\n");
            report.append("Total: $").append(order.getTotalAmount()).append("\n");
            for (OrderItem item : order.getItems()) {
                report.append("  - ").append(item.getProductName()).append(" x").append(item.getQuantity()).append("\n");
            }
            report.append("------------------------\n");
        }
        return report.toString();
    }

    public void processOrder(Order order) throws Exception {
        // More robust error/logging for the slow API
        try {
            Thread.sleep(5000); // Simulate slow call
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO processed_orders (order_id, status) VALUES (?, ?)")) {
                stmt.setInt(1, order.getOrderId());
                stmt.setString(2, "PROCESSED");
                stmt.executeUpdate();
                logger.info("Processed order: " + order.getOrderId());
            }
        } catch (InterruptedException e) {
            logger.error("Order processing interrupted for Order ID: " + order.getOrderId(), e);
            Thread.currentThread().interrupt();
            throw e;
        } catch (SQLException e) {
            logger.error("Failed database operation for processed order ID: " + order.getOrderId(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error in processOrder for order ID: " + order.getOrderId(), e);
            throw e;
        }
    }

    public static void testConnection() {
        logger.info("Testing database connection...");
        logger.info("DB_URL: " + DB_URL);
        logger.info("DB_USER: " + DB_USER);
        logger.info("DB_PASSWORD: " + (DB_PASSWORD != null && !DB_PASSWORD.isEmpty() ? "***SET***" : "***NOT SET***"));
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            logger.info("✓ Connection successful!");
            logger.info("Database: " + conn.getCatalog());
            logger.info("User: " + conn.getMetaData().getUserName());
        } catch (SQLException e) {
            logger.error("✗ Connection failed!", e);
        }
    }

    public static void main(String[] args) {
        testConnection();
    }
}
