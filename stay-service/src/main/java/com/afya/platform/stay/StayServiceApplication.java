package com.afya.platform.stay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages = {"com.afya.platform.stay", "com.afya.platform.shared"})
public class StayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StayServiceApplication.class, args);
    }
}
