package com.ssafy.s14p11c204.server;

import com.ssafy.s14p11c204.server.global.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public class DiagnoseDbTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void checkEnumValues() {
        System.out.println("=== DIAGNOSIS START ===");
        try {
            // Check room_type enum values
            String sql = "SELECT unnest(enum_range(NULL::room_type))::text";
            List<String> enumValues = jdbcTemplate.queryForList(sql, String.class);
            System.out.println("Existing room_type values: " + enumValues);

            // Check if 'KYUNGDO' exists
            if (enumValues.contains("KYUNGDO")) {
                System.out.println("SUCCESS: 'KYUNGDO' is present.");
            } else {
                System.err.println("FAILURE: 'KYUNGDO' is MISSING!");
            }
            
            // Check users table
            List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT * FROM users");
            System.out.println("Users count: " + users.size());
            
            // Check chat_logs table
            try {
                jdbcTemplate.execute("SELECT 1 FROM chat_logs LIMIT 1");
                System.out.println("SUCCESS: 'chat_logs' table exists.");
            } catch (Exception e) {
                System.err.println("FAILURE: 'chat_logs' table does NOT exist!");
            }
            
            // Check updated_at column in friendships
            try {
                jdbcTemplate.execute("SELECT updated_at FROM friendships LIMIT 1");
                System.out.println("SUCCESS: 'updated_at' column exists in 'friendships'.");
            } catch (Exception e) {
                System.err.println("FAILURE: 'updated_at' column does NOT exist in 'friendships'!");
            }

        } catch (Exception e) {
            System.err.println("Diagnosis failed: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("=== DIAGNOSIS END ===");
    }
}
