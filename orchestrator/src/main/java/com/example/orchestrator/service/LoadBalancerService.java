package com.example.orchestrator.service;

import org.springframework.context.annotation.Bean;

public interface LoadBalancerService {

  /**
   * Gets the next server that is available to receive a request.
   *
   * @return Returns the address of the server.
   */
  String getBackendServerUrl();
}
