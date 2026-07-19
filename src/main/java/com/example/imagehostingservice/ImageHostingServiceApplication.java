package com.example.imagehostingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@ConfigurationPropertiesScan
@SpringBootApplication
public class ImageHostingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImageHostingServiceApplication.class, args);
	}

}
