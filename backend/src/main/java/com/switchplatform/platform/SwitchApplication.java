package com.switchplatform.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SwitchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SwitchApplication.class, args);
    }
}
