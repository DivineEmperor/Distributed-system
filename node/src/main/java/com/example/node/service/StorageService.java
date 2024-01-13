package com.example.node.service;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

  void store(String path, MultipartFile file);

  /**
   * create a directory by path.
   *
   * @param dirPath path of the directory.
   */
  void createDirectory(String dirPath);

  /**
   * delete a directory by its path
   *
   * @param dirPath path of the directory
   */
  void deleteDirectory(String dirPath);

  /**
   * delete a file by its path.
   *
   * @param filePath path of the file
   */
  void deleteFile(String filePath);

  Stream<Path> loadAllByPath(String rootPath) throws NoSuchFileException;

  /**
   * get the path of an entry on disk,
   * if the entry is not a sub entry of the root location, return root location.
   *
   * @param entryName name of the entry
   * @return path on disk
   */
  Path getPath(String entryName);

  /**
   * Gets file resource
   *
   * @param filePath path of the file
   * @return resource of the file
   */
  Resource loadAsResource(String filePath);

  void start();
}
