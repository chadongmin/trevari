package com.trevari;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class TrevariApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrevariApplication.class, args);
    }

}
