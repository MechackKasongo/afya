package com.afya.platform.careentry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages = {"com.afya.platform.careentry", "com.afya.platform.shared"})
public class CareEntryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareEntryServiceApplication.class, args);
    }
}
