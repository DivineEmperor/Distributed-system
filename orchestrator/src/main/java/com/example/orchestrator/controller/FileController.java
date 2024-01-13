package com.example.orchestrator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

/**
 * Defines the functionalities of the Central Orchestrator.
 */
@Controller
public interface FileController {

  /**
   * Creates a directory.
   * @param directoryPath the path of the directory
   * @return returns if the operation was successful or not.
   */
  ResponseEntity<?> makeDirectory(String directoryPath);

  /**
   * Fetches the file given its path.
   * @param filePath the path of the file.
   * @return the file
   */
  ResponseEntity<?> fetchFile(String filePath);

  /**
   * Get names of all the sub files and folders in a directory.
   * @param directoryPath the path of the directory
   * @return a String response containing the sub files and directories.
   */
  ResponseEntity<?> fetchSubDirectories(String directoryPath);

  /**
   * Deletes a directory.
   * @param directoryPath the path of the directory.
   */
  void deleteDirectory(String directoryPath);

  /**
   * Uploads a file.
   * @param filePath  the path of the file.
   * @param file the file that needs to be pushed.
   * @return returns if the operation was successful or not.
   */
  ResponseEntity<?> pushFile(String filePath, MultipartFile file);

  /**
   * Deletes a file given the path.
   * @param filePath the path of the file.
   */
  void deleteFile(String filePath);
}
