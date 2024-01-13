package com.example.node.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStoreService implements StorageService {

  private final Path root;

  @Autowired
  public FileStoreService(FileStoreProperties fileProperties) {
    this.root = Paths.get(fileProperties.getLocation());
  }
  @Override
  public void store(String path, MultipartFile file) {
    try {
      //check if the file is empty
      if (file.isEmpty()) {
        throw new IOException("File is empty and cannot be saved");
      }
      Path storePath = getPath(path).normalize().toAbsolutePath();

      if (!Files.exists(storePath)) {
        throw new IllegalStateException("Directory does not exits");
      }

      Path fileDest = storePath.resolve(Paths.get(file.getOriginalFilename())).normalize()
          .toAbsolutePath();

      try (InputStream ip = file.getInputStream()) {
        //copy the file to the dest
        Files.copy(ip, fileDest, StandardCopyOption.REPLACE_EXISTING);
      }

    } catch (IOException e) {
      throw new IllegalStateException("Unable to store file");
    }
  }

  @Override
  public void createDirectory(String dirPath) {
    try {
      Path folderPath = getPath(dirPath);
      Files.createDirectories(folderPath);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create directory");
    }
  }

  @Override
  public void deleteDirectory(String dirPath) {
    try {
      Path folderPath = getPath(dirPath);

      if (!Files.exists(folderPath) | !Files.isDirectory(folderPath)) {
        throw new IllegalStateException("Directory doesn't exist or is not a valid directory");
      }

      Files.deleteIfExists(folderPath);

    } catch (IOException e) {
      throw new IllegalStateException("Failed to delete directory");
    }
  }

  @Override
  public void deleteFile(String filePath) {
    try {
      Path Path = getPath(filePath);

      if (!Files.exists(Path) | Files.isDirectory(Path)) {
        throw new IllegalStateException("File doesn't exist or is not a valid file");
      }

      Files.deleteIfExists(Path);

    } catch (IOException e) {
      throw new IllegalStateException("Failed to delete directory");
    }
  }

  @Override
  public Stream<Path> loadAllByPath(String rootPath) throws NoSuchFileException {
    try {
      System.out.println("Root path in service is " + rootPath);
      System.out.println("This is the separator" + File.separator);
      if (!rootPath.startsWith("\\") && !rootPath.startsWith("/")) {
        throw new IllegalStateException("Path should begin with a '/'");
      }

      Path dirPath = getPath(rootPath);

      if (!Files.isDirectory(dirPath)) {
        throw new IllegalStateException("This path is not a directory");
      }

      return Files.walk(dirPath, 1).filter(p -> !p.equals(dirPath)).map(dirPath::relativize);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Path getPath(String entryName) {
    if (entryName.startsWith("\\") || entryName.startsWith("/")) {
      entryName = entryName.substring(1);
    }

    System.out.println("The entry name now is " + entryName);

    Path entryPath = this.root.resolve(entryName).normalize();

    // check if the path is outside the root folder
    if (!entryPath.startsWith(this.root)) {
      entryPath = this.root;
    }
    return entryPath;
  }

  @Override
  public Resource loadAsResource(String filePath) {
    try {
      Path file = getPath(filePath);

      if (Files.isDirectory(file)) {
        throw new IllegalStateException("Cannot download a folder");
      }
      Resource resource = new UrlResource(file.toUri());
      if (resource.exists() || resource.isReadable()) {
        return resource;
      } else {
        throw new IllegalStateException(
            "Could not read file: " + filePath);

      }
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Could not read file: " + filePath);
    }
  }
  @Override
  public void start() {
    try{
      Files.createDirectories(this.root);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
