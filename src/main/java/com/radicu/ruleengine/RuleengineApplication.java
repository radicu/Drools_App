package com.radicu.ruleengine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.radicu.ruleengine.model.Spindle;
import com.radicu.ruleengine.service.MongoService;

@SpringBootApplication
public class RuleengineApplication implements CommandLineRunner {

    // Use field injection instead of constructor injection
    @Autowired
    private MongoService mongoService;

    public static void main(String[] args) {
        SpringApplication.run(RuleengineApplication.class, args);
    }

    @Override
    public void run(String... args) {
        // Pass mongoService to Spindle
        Spindle.setMongoService(mongoService);
    }
}