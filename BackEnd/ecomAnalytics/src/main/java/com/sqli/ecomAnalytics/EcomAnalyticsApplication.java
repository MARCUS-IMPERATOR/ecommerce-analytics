package com.sqli.ecomAnalytics;

import com.sqli.ecomAnalytics.configuration.DataGenerationProp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication()
@EnableJpaAuditing
@EnableConfigurationProperties(DataGenerationProp.class)
public class EcomAnalyticsApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcomAnalyticsApplication.class, args);
	}

}
