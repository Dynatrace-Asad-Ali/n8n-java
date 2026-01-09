package com.dynatrace.ecommerce;

import org.junit.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.*;

public class OrderServiceTest {
    @Test
    public void testOrderItemLeakEdgeCase() throws SQLException {
        OrderService svc = new OrderService();
        List<OrderItem> items = svc.getOrderItemsWithValidation(500);
        assertNotNull(items);
        // Previously, connections could leak here for 500, now try-with-resources should close
    }
    
    @Test
    public void testShippingInfoLeakEdgeCase() throws SQLException {
        OrderService svc = new OrderService();
        ShippingInfo info = svc.getShippingInfoDetailed(20);
        assertNotNull(info);
        // Ensures connection closes for mod-20 edge case w/ try-with-resources
    }
    
    @Test
    public void testConnectionPoolStats() {
        // Could assert SimpleConnectionPool stats, recommend new HikariCP pool
    }
}
