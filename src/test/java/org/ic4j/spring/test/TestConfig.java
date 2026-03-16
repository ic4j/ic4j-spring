package org.ic4j.spring.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@ComponentScan(basePackages = "org.ic4j.spring.test") // Adjust the package if necessary
public class TestConfig {

}
