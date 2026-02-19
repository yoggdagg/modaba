package com.ssafy.s14p11c204.server;

import com.ssafy.s14p11c204.server.global.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
