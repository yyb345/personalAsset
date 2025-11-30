package com.example.finance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FinanceApplication {

    private static final Logger logger = LoggerFactory.getLogger(FinanceApplication.class);

    public static void main(String[] args) {
        logger.info("Starting Finance Application...");
        SpringApplication.run(FinanceApplication.class, args);
        logger.info("Finance Application started successfully");
    }

}
