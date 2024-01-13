package com.example.orchestrator;

import com.example.orchestrator.service.LoadBalancerService;
import com.example.orchestrator.service.LoadBalancerServiceImpl;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Collections;

@SpringBootApplication
public class OrchestratorApplication {
  public static void main(String[] args) {
    System.out.println("Starting app");
    SpringApplication springApp = new SpringApplication(OrchestratorApplication.class);
    springApp.setDefaultProperties(
            Collections.singletonMap("server.port", Integer.parseInt(args[0])));
    springApp.run(args);
  }

  @Bean
  CommandLineRunner init(LoadBalancerService loadBalancerService) {
    return (args) -> {
    };
  }
}
