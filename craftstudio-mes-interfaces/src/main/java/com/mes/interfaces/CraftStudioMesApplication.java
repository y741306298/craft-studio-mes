package com.mes.interfaces;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@SpringBootApplication(
	scanBasePackages = {
			"com.mes.interfaces",
			"com.mes.application",
			"com.mes.domain",
			"com.mes.infra",
	},
	nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
public class CraftStudioMesApplication {
	public static void main(String[] args) {
		SpringApplication.run(CraftStudioMesApplication.class, args);
	}

}
