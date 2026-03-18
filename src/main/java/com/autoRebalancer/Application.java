package com.autoRebalancer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@EnableScheduling 기존 스케줄러를 aws lambda로 대체
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
