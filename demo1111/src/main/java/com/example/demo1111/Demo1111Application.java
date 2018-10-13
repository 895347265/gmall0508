package com.example.demo1111;

import com.netflix.discovery.EurekaNamespace;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class Demo1111Application {

    public static void main(String[] args) {
        SpringApplication.run(Demo1111Application.class, args);
    }
}
