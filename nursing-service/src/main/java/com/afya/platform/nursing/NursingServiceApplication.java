package com.afya.platform.nursing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.afya.platform.nursing", "com.afya.platform.shared"})
public class NursingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NursingServiceApplication.class, args);
    }
}
