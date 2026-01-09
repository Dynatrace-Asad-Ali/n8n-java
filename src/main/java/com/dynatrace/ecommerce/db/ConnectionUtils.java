package com.dynatrace.ecommerce.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConnectionUtils {
    // Utility method to get and close connections safely
    public static void closeQuietly(Connection conn) {
        if (conn != null) try { conn.close(); } catch (SQLException ignore) {}
    }
    public static void closeQuietly(PreparedStatement stmt) {
        if (stmt != null) try { stmt.close(); } catch (SQLException ignore) {}
    }
    public static void closeQuietly(ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (SQLException ignore) {}
    }
}
