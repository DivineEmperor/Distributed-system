package com.example.node.controller;

import com.example.node.utils.PaxosStep;

import java.io.FileNotFoundException;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

public interface FileController {

  public ResponseEntity<?> fetchSubDirectories(
          @RequestParam(value = "path", defaultValue = "/") String path);

  public ResponseEntity makeDirectory(@RequestParam(value = "path") String path,
                                      @RequestParam(required = false) PaxosStep step);

  public ResponseEntity<?> deleteDirectory(@RequestParam(value = "path") String path,
                                           @RequestParam(required = false) PaxosStep step);

  public ResponseEntity<Resource> fetchFile(@RequestParam(value = "path") String path);

  public ResponseEntity<?> pushFile(@RequestBody MultipartFile file,
                                    @RequestParam("path") String path, @RequestParam(required = false) PaxosStep step);

  public ResponseEntity<?> deleteFile(@RequestParam(value = "path") String path,
                                      @RequestParam(required = false) PaxosStep step);

  public ResponseEntity<?> handleStorageFileNotFound(FileNotFoundException exc);

}
