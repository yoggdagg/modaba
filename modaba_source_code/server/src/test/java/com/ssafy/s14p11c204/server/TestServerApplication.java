package com.ssafy.s14p11c204.server;

import com.ssafy.s14p11c204.server.global.config.TestcontainersConfiguration;
import org.springframework.boot.SpringApplication;

public class TestServerApplication {

    static void main(String[] args) {
        SpringApplication.from(ServerApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
