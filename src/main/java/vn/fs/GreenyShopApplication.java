package vn.fs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GreenyShopApplication {

	public static void main(String[] args) {
		SpringApplication.run(GreenyShopApplication.class, args);
	}

}
