package org.ic4j.spring.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.ic4j.spring.test")
public class LoanProviderTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoanProviderTestApplication.class, args);
    }
}
