package com.cdc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DatabaseTestService {

    public boolean testMysqlConnection(String host, int port, String username, String password) {
        String url = String.format("jdbc:mysql://%s:%d?useSSL=false&serverTimezone=UTC", host, port);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, username, password);
            conn.close();
            return true;
        } catch (Exception e) {
            log.error("MySQL connection test failed", e);
            return false;
        }
    }

    public List<String> getMysqlDatabases(String host, int port, String username, String password) {
        List<String> databases = new ArrayList<>();
        String url = String.format("jdbc:mysql://%s:%d?useSSL=false&serverTimezone=UTC", host, port);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, username, password);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW DATABASES");
            while (rs.next()) {
                databases.add(rs.getString(1));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            log.error("Get MySQL databases failed", e);
        }
        return databases;
    }

    public List<String> getMysqlTables(String host, int port, String username, String password, String database) {
        List<String> tables = new ArrayList<>();
        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", host, port, database);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, username, password);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW TABLES");
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            log.error("Get MySQL tables failed", e);
        }
        return tables;
    }

    public boolean testDorisConnection(String feNodes, String username, String password) {
        String[] nodes = feNodes.split(",");
        String firstNode = nodes[0].split(":")[0];
        int port = 9030;

        if (nodes[0].contains(":")) {
            String[] parts = nodes[0].split(":");
            firstNode = parts[0];
            port = Integer.parseInt(parts[1]);
        }

        String url = String.format("jdbc:mysql://%s:%d?useSSL=false&serverTimezone=UTC", firstNode, port);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, username, password);
            conn.close();
            return true;
        } catch (Exception e) {
            log.error("Doris connection test failed", e);
            return false;
        }
    }

    public List<String> getDorisDatabases(String feNodes, String username, String password) {
        List<String> databases = new ArrayList<>();
        String[] nodes = feNodes.split(",");
        String firstNode = nodes[0].split(":")[0];
        int port = 9030;

        if (nodes[0].contains(":")) {
            String[] parts = nodes[0].split(":");
            firstNode = parts[0];
            port = Integer.parseInt(parts[1]);
        }

        String url = String.format("jdbc:mysql://%s:%d?useSSL=false&serverTimezone=UTC", firstNode, port);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, username, password);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW DATABASES");
            while (rs.next()) {
                databases.add(rs.getString(1));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            log.error("Get Doris databases failed", e);
        }
        return databases;
    }

    public List<String> getDorisTables(String feNodes, String username, String password, String database) {
        List<String> tables = new ArrayList<>();
        String[] nodes = feNodes.split(",");
        String firstNode = nodes[0].split(":")[0];
        int port = 9030;

        if (nodes[0].contains(":")) {
            String[] parts = nodes[0].split(":");
            firstNode = parts[0];
            port = Integer.parseInt(parts[1]);
        }

        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", firstNode, port, database);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, username, password);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW TABLES");
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            log.error("Get Doris tables failed", e);
        }
        return tables;
    }
}
