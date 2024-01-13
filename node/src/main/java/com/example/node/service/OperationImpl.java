package com.example.node.service;

import java.io.File;

public class OperationImpl implements Operation {

  private File file;
  private String path;

  public OperationImpl(File file, String path) {
    this.file = file;
    this.path = path;
  }

  @Override
  public File getFile() {
    return this.file;
  }

  @Override
  public String getPath() {

    return this.path;
  }
}
