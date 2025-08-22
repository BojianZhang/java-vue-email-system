package com.enterprise.email;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 企业邮件系统启动类
 * 
 * @author Enterprise Email System
 * @version 1.0.0
 */
@SpringBootApplication
@MapperScan("com.enterprise.email.mapper")
@EnableAsync
@EnableScheduling
public class EmailSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmailSystemApplication.class, args);
        System.out.println("=================================");
        System.out.println("企业邮件系统启动成功！");
        System.out.println("API访问地址: http://localhost:9000/api");
        System.out.println("Swagger文档: http://localhost:9000/api/swagger-ui.html");
        System.out.println("Druid监控: http://localhost:9000/api/druid");
        System.out.println("=================================");
    }
}