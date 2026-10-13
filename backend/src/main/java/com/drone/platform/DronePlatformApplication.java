package com.drone.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.drone.platform.module.**.mapper")
public class DronePlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(DronePlatformApplication.class, args);
    }
}

