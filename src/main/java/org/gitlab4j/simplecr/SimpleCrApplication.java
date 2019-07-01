package org.gitlab4j.simplecr;

import org.gitlab4j.simplecr.config.SimpleCrConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SimpleCrConfiguration.class)
public class SimpleCrApplication {
    
	public static void main(String[] args) {
		SpringApplication.run(SimpleCrApplication.class, args);
	}
}
