package com.ssafy.s14p11c204.server.global.config;

import org.springframework.boot.test.context.TestConfiguration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads environment variables from the root .env file into System Properties
 * so that tests can resolve placeholders like ${DB_URL} in application.yml.
 * 
 * This effectively disables Testcontainers by ensuring the application connects
 * to the locally running services (defined in docker-compose-local.yml)
 * using the configuration from the SSOT .env file.
 */
@TestConfiguration
public class TestcontainersConfiguration {

    static {
        loadEnvVariables();
    }

    private static void loadEnvVariables() {
        try {
            // Attempt to locate .env in the project root (parent of server/)
            Path envPath = Paths.get("..", ".env").toAbsolutePath().normalize();
            if (!Files.exists(envPath)) {
                // Fallback: Check current directory
                envPath = Paths.get(".env").toAbsolutePath().normalize();
            }

            if (Files.exists(envPath)) {
                System.out.println("Loading .env from: " + envPath);
                try (BufferedReader reader = new BufferedReader(new FileReader(envPath.toFile()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        // Skip empty lines and comments
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            String key = parts[0].trim();
                            String value = parts[1].trim();
                            
                            // Remove surrounding quotes if present
                            if ((value.startsWith("\"") && value.endsWith("\"")) || 
                                (value.startsWith("'") && value.endsWith("'"))) {
                                value = value.substring(1, value.length() - 1);
                            }
                            
                            // Set System property if not already set (System properties take precedence)
                            if (System.getProperty(key) == null) {
                                System.setProperty(key, value);
                            }
                        }
                    }
                }
            } else {
                System.err.println("WARNING: .env file not found at " + envPath + ". Tests may fail due to missing environment variables.");
            }
        } catch (IOException e) {
            System.err.println("Failed to load .env file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
