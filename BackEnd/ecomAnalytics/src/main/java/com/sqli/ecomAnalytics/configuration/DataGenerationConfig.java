package com.sqli.ecomAnalytics.configuration;

import com.github.javafaker.Faker;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Configuration
@EnableConfigurationProperties(DataGenerationProp.class)
public class DataGenerationConfig {
    @Bean
    public Faker faker(){
        return new Faker(new Locale("en-Us"));
    }
}
