package com.toddwu.toj_judge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TojJudgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TojJudgeApplication.class, args);
    }

}
