package com.hostelconnect.listener;

import com.hostelconnect.dao.UserDAO;
import com.hostelconnect.util.DBConnection;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.InputStream;
import java.sql.Connection;
import java.util.Properties;

@WebListener
public class AppContextListener implements ServletContextListener {

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("HostelConnect application starting up...");
        try {
            // Read initial credentials from db.properties if provided
            Properties props = new Properties();
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("db.properties")) {
                if (in != null) {
                    props.load(in);
                }
            } catch (Exception ignored) {}

            String adminId = props.getProperty("admin.id", "admin123");
            String adminName = props.getProperty("admin.name", "Hostel Administrator");
            String adminEmail = props.getProperty("admin.email", "admin@hostelconnect.com");
            String adminPass = props.getProperty("admin.password", "Admin@123");

            String wardenId = props.getProperty("warden.id", "warden123");
            String wardenName = props.getProperty("warden.name", "Hostel Warden");
            String wardenEmail = props.getProperty("warden.email", "warden@hostelconnect.com");
            String wardenPass = props.getProperty("warden.password", "Warden@123");
            String hostelName = props.getProperty("warden.hostel", "Main Hostel Block");

            // Seed designated Admin and Warden safely with BCrypt hashes
            userDAO.provisionDefaultAdminAndWarden(
                adminId, adminName, adminEmail, adminPass,
                wardenId, wardenName, wardenEmail, wardenPass, hostelName
            );
            System.out.println("HostelConnect initial accounts verified/provisioned.");
        } catch (Exception e) {
            System.err.println("Could not auto-provision initial accounts on startup: " + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("HostelConnect shutting down.");
    }
}
