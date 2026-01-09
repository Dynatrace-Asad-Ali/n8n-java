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

/**
 * Order Service with improved resource management.
 * This version fixes connection leaks and makes error handling more robust.
 */
public class OrderService {
    // Database configuration - uses H2 in-memory database
    private static final String DB_URL = DatabaseInitializer.H2_URL;
    private static final String DB_USER = DatabaseInitializer.H2_USER;
    private static final String DB_PASSWORD = DatabaseInitializer.H2_PASSWORD;

    // Connection pool with configurable limit
    private static SimpleConnectionPool connectionPool;

    static {
        // Initialize H2 database with test data
        try {
            DatabaseInitializer.initializeH2Database();
            System.out.println("* Database initialized successfully");
        } catch (SQLException e) {
            System.err.println("✗ Failed to initialize database!");
            e.printStackTrace();
        }

        // Initialize connection pool
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
        }
        return items;
    }

    /**
     * Enhanced code path with validation. Fixed leak with try-with-resources.
     */
    private List<OrderItem> getOrderItemsWithValidation(int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        // Always close resources, even in error cases or exceptional flow.
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
                    // Validation logic - checking for valid data
                    if (item.getQuantity() > 0 && item.getPrice() > 0) {
                        items.add(item);
                    }
                }
            }
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
        }
        return null;
    }

    /**
     * Detailed path with address formatting. Fixed leak with try-with-resources.
     */
    private ShippingInfo getShippingInfoDetailed(int orderId) throws SQLException {
        // Always close resources, even with enhanced formatting or exceptions.
        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT address, city, state, zip_code, tracking_number FROM shipping WHERE order_id = ?")) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                ShippingInfo info = null;
                if (rs.next()) {
                    info = new ShippingInfo();
                    info.setAddress(rs.getString("address"));
                    info.setCity(rs.getString("city"));
                    info.setState(rs.getString("state"));
                    info.setZipCode(rs.getString("zip_code"));
                    info.setTrackingNumber(rs.getString("tracking_number"));
                    // Enhancement: Format address nicely
                    String formattedAddress = info.getAddress().trim().toUpperCase();
                    info.setAddress(formattedAddress);
                }
                return info;
            }
        }
        return null;
    }

    public String generateOrderReport(List<Order> orders) {
        String report = ""; // ISSUE: Using String concatenation
        for (Order order : orders) {
            report += "Order ID: " + order.getOrderId() + "\n";
            report += "Date: " + order.getOrderDate() + "\n";
            report += "Total: $" + order.getTotalAmount() + "\n";
            for (OrderItem item : order.getItems()) {
                report += "  - " + item.getProductName() + " x" + item.getQuantity() + "\n";
            }
            report += "------------------------\n";
        }
        return report;
    }

    public void processOrder(Order order) throws Exception {
        Thread.sleep(5000); // Simulating slow external API call
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO processed_orders (order_id, status) VALUES (?, ?)"
        );
        stmt.setInt(1, order.getOrderId());
        stmt.setString(2, "PROCESSED");
        stmt.executeUpdate();
        // ISSUE: Resources not closed
    }

    public static void testConnection() {
        System.out.println("Testing database connection...");
        System.out.println("DB_URL: " + DB_URL);
        System.out.println("DB_USER: " + DB_USER);
        System.out.println("DB_PASSWORD: " + (DB_PASSWORD != null && !DB_PASSWORD.isEmpty() ? "***SET***" : "***NOT SET***"));
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("✓ Connection successful!");
            System.out.println("Database: " + conn.getCatalog());
            System.out.println("User: " + conn.getMetaData().getUserName());
        } catch (SQLException e) {
            System.err.println("✗ Connection failed!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        testConnection();
    }
}
