package com.wms.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.wms")
@MapperScan({"com.wms.system.repository", "com.wms.inbound.repository", "com.wms.inventory.repository", "com.wms.stocktake.repository", "com.wms.exception.repository"})
@EnableScheduling
public class WmsSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(WmsSystemApplication.class, args);
    }
}
