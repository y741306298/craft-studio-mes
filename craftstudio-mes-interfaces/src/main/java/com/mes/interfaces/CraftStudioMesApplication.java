package com.mes.interfaces;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@SpringBootApplication(
	scanBasePackages = {
			"com.mes.interfaces",
			"com.mes.application",
			"com.mes.domain",
			"com.mes.infra",
	},
	nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
	exclude = {
		DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class
	}
)
@ComponentScan(
	basePackages = {
		"com.mes.interfaces",
		"com.mes.application",
		"com.mes.domain",
		"com.mes.infra",
		"com.piliofpala.craftstudio.shared"
	},
	excludeFilters = @ComponentScan.Filter(
		type = FilterType.ASSIGNABLE_TYPE,
		classes = com.piliofpala.craftstudio.shared.infra.db.DatabaseConfig.class
	),
	nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
public class CraftStudioMesApplication {
	public static void main(String[] args) {
		SpringApplication.run(CraftStudioMesApplication.class, args);
	}

}
