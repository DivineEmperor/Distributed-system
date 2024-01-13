package com.example.orchestrator.controller;

import com.example.orchestrator.service.LoadBalancerService;
import com.example.orchestrator.service.RedisMutex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class FileControllerImpl implements FileController {

  @Autowired
  LoadBalancerService loadBalancer;
  @Autowired
  RedisMutex mutexManager;

  @PostMapping("/directories")
  @ResponseBody
  @Override
  public ResponseEntity<?> makeDirectory(String path) {
    String lockName = path;
    String lockType = "makeDirectoryLock";
    boolean lockAcquired = mutexManager.lock(lockName, lockType, 60);
    if (lockAcquired) {
      // Construct and send an HTTP POST request to the backend server
      String backendServerUrl = loadBalancer.getBackendServerUrl();
      RestTemplate restTemplate = new RestTemplate();
      System.out.println(backendServerUrl);
      ResponseEntity<?> response = restTemplate.exchange(backendServerUrl + "/directories?path=" + path,
              HttpMethod.POST, null, String.class);
      // Release the acquired lock
      mutexManager.releaseLock(lockName, lockType);
      return response;
    } else {
      // If the lock cannot be acquired, return a conflict status response
      String errorMessage = "Failed to create directory: " + path + ". Directory is currently being modified.";
      return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
    }
  }

  @GetMapping("/files")
  @ResponseBody
  @Override
  public ResponseEntity<?> fetchFile(String path) {
    // Get the URL of the backend server
    String backendServerUrl = loadBalancer.getBackendServerUrl();
    RestTemplate restTemplate = new RestTemplate();
    // Send an HTTP GET request to download the specified file
    ResponseEntity<Resource> response = restTemplate.exchange(
            backendServerUrl + "/files?path=" + path,
            HttpMethod.GET, null, Resource.class);
    // Return the downloaded file
    return response;
  }

  @GetMapping("/directories")
  @ResponseBody
  @Override
  public ResponseEntity<?> fetchSubDirectories(String path) {
    // Get the URL of the backend server
    String backendServerUrl = loadBalancer.getBackendServerUrl();
    RestTemplate restTemplate = new RestTemplate();
    // Send an HTTP GET request to retrieve subdirectories of the specified directory
    ResponseEntity<?> response = restTemplate.exchange(
            backendServerUrl + "/directories?path=" + path,
            HttpMethod.GET, null, String.class);
    // Return the list of subdirectories in the specified directory
    return response;

  }

  @DeleteMapping("/directories")
  @ResponseBody
  @Override
  public void deleteDirectory(String path) {
    // Acquire a lock on the specified directory path to prevent concurrent modification
    String lockName = path;
    String lockType = "deleteDirectoryLock";
    boolean lockAcquired = mutexManager.lock(lockName, lockType, 60);
    // If the lock is successfully acquired, delete the directory
    if (lockAcquired) {
      // Get the URL of the backend server
      String backendServerUrl = loadBalancer.getBackendServerUrl();
      RestTemplate restTemplate = new RestTemplate();
      restTemplate.delete(backendServerUrl + "/directories?path=" + path);
      // Release the lock on the directory path
      mutexManager.releaseLock(lockName, lockType);
    }
  }

  @RequestMapping(path = "/files", method = POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @Override
  public ResponseEntity<?> pushFile(String path, MultipartFile file) {
    // Acquire a lock on the specified file path to prevent concurrent modification
    String lockName = path;
    String lockType = "pushFileLock";
    boolean lockAcquired = mutexManager.lock(lockName, lockType, 60);
    // If the lock is successfully acquired, upload the file
    if (lockAcquired) {
      // Prepare the file upload request
      MultiValueMap<String, Object> requestMap = new LinkedMultiValueMap<>();
      requestMap.add("file", file.getResource());
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);
      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(requestMap, headers);
      // Send the file upload request to the backend server
      String backendServerUrl = loadBalancer.getBackendServerUrl();
      RestTemplate restTemplate = new RestTemplate();
      String response = restTemplate.postForObject(
              backendServerUrl + "/files?path=" + path,
              requestEntity, String.class);
      // Release the lock on the file path
      mutexManager.releaseLock(lockName, lockType);
      // Return the response from the backend server
      return ResponseEntity.ok(response);
    } else {
      // If the lock cannot be acquired, return a conflict status response
      String errorMessage = "Failed to upload file: " + path + ". File is currently being modified.";
      return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
    }
  }

  @DeleteMapping("/files")
  @ResponseBody
  @Override
  public void deleteFile(String path) {
    // Acquire a lock on the specified file path to prevent concurrent modification
    String lockName = path;
    String lockType = "deleteFileLock";
    boolean lockAcquired = mutexManager.lock(lockName, lockType, 60);
    // If the lock is successfully acquired, delete the file
    if (lockAcquired) {
      // Send an HTTP DELETE request to the backend server to delete the file
      String backendServerUrl = loadBalancer.getBackendServerUrl();
      RestTemplate restTemplate = new RestTemplate();
      restTemplate.delete(backendServerUrl + "/files?path=" + path);
      // Release the lock on the file path
      mutexManager.releaseLock(lockName, lockType);
    }
  }
}
