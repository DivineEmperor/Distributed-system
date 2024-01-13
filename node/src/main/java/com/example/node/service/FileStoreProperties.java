package com.example.node.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Used as a configuration class for the file storage system.
 */
@ConfigurationProperties("storage")
public class FileStoreProperties {
  private String loc = "file-dir";

  public String getLocation() {
    return loc;
  }

  public void setLocation(String location) {
    this.loc = location;
  }
}
