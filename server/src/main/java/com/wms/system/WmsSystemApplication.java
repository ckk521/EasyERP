package com.wms.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wms.system.repository")
public class WmsSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(WmsSystemApplication.class, args);
    }
}
