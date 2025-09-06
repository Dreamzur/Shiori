package com.shiori.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ShioriApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShioriApplication.class, args);
	}

}
