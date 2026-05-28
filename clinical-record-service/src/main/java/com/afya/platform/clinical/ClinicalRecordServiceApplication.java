package com.afya.platform.clinical;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages = {"com.afya.platform.clinical", "com.afya.platform.shared"})
public class ClinicalRecordServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicalRecordServiceApplication.class, args);
    }
}
