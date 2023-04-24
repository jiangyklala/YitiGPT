package com.jxm.yitiGPT;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@MapperScan("com.jxm.yitiGPT.mapper")
public class YitiGptApplication {

	public static void main(String[] args) {
		SpringApplication.run(YitiGptApplication.class, args);
	}

}
