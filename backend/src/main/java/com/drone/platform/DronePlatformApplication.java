package com.drone.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.drone.platform.module.**.mapper")
public class DronePlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(DronePlatformApplication.class, args);
    }
}
