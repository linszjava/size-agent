package com.size;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AliyunApplication {

    public static void main(String[] args) {

        SpringApplication.run(AliyunApplication.class,args);

        System.out.println("====大模型测试项目成功启动=====");
    }
}
