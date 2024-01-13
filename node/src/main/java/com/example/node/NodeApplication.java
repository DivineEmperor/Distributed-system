package com.example.node;

import com.example.node.service.FileStoreProperties;
import com.example.node.service.StorageService;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties(FileStoreProperties.class)
@SpringBootApplication
public class NodeApplication {

  public static void main(String [] args) {

    SpringApplication springApp = new SpringApplication(NodeApplication.class);
    springApp.setDefaultProperties(
        Collections.singletonMap("server.port", Integer.parseInt(args[0])));

		springApp.run(args);
  }
	@Bean
	CommandLineRunner init(StorageService filestore){return (args) -> {filestore.start();};}

}
