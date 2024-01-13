package com.example.orchestrator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.Random;

@PropertySource(value={"classpath:application.properties"})
@Service
public class LoadBalancerServiceImpl implements LoadBalancerService {

  private Random random = new Random();

  @Autowired
  public LoadBalancerServiceImpl() {

  }

  @Value("${node.urls}")
  private String[] nodeUrls;
  @Override
  public String getBackendServerUrl() {
    return nodeUrls[random.nextInt(nodeUrls.length)];
  }
}
