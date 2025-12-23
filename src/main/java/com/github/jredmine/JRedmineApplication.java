package com.github.jredmine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * JRedmine 应用主类
 * 
 * @author panfeng
 */
@SpringBootApplication
@MapperScan("com.github.jredmine.mapper")
public class JRedmineApplication {

    public static void main(String[] args) {
        SpringApplication.run(JRedmineApplication.class, args);
    }

}
