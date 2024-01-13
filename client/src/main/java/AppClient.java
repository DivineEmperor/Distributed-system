import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class AppClient {
  private Path currentDir = Paths.get("/");
  private boolean active = true;

  public static final String loadBalancerUrl = "http://127.0.0.1:8080";

  public static void main(final String[] args) {
    new AppClient(args[0]);
  }

  public AppClient(String user) {
    Scanner inputScanner = new Scanner(System.in);

    showCommandPrompt();
    while (active) {
      try {
        System.out.printf("***** User: %s | Distributed File Storage:> %s ", user, currentDir);
        String command = inputScanner.nextLine().trim();
        processCommand(command);
      } catch (IOException e) {
        System.out.println("Server error: " + e);
      }
    }
  }

  private void showCommandPrompt() {
    StringBuilder sb = new StringBuilder();
    sb.append("\nEnter one of the following commands, followed by arguments denoted by <>\n");
    sb.append("Get-ChildItem\n");
    sb.append("Set-Location <path>\n");
    sb.append("New-Directory <path>\n");
    sb.append("Remove-Directory <path>\n");
    sb.append("Remove-File <path>\n");
    sb.append("Get-Location\n");
    sb.append("Get-File <path>\n");
    sb.append("Push-File <path>\n");
    sb.append("Exit");
    System.out.println(sb.toString());
  }

  String fetchFiles(Path targetPath) throws IOException {
    try {
      String path = targetPath.toString().replace(File.separator, "/");
      URIBuilder builder = new URIBuilder(loadBalancerUrl
              + "/directories");
      builder.addParameter("path", path);

      Content content = Request.Get(builder.build())
              .execute()
              .returnContent();
      return content.toString();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  void pushFile(Path sourcePath) throws IOException, URISyntaxException {
    URIBuilder builder = new URIBuilder(loadBalancerUrl + "/files");
    String path = currentDir.toString().replace(File.separator, "/");
    builder.addParameter("path", path);

    File sourceFile = new File(sourcePath.toString());
    HttpClient httpClient = HttpClients.createDefault();
    HttpPost httpPost = new HttpPost(builder.build());
    MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
    entityBuilder.addPart("file", new FileBody(sourceFile));
    HttpEntity httpEntity = entityBuilder.build();
    httpPost.setEntity(httpEntity);
    HttpResponse httpResponse = httpClient.execute(httpPost);

    InputStream responseStream = httpResponse.getEntity().getContent();
    String resultString = IOUtils.toString(responseStream, StandardCharsets.UTF_8);
    System.out.println(resultString);
  }

  void fetchFile(Path filePath) throws IOException {
    try {
      URIBuilder builder = new URIBuilder(loadBalancerUrl + "/files");
      builder.addParameter("path", filePath.toString().replace(File.separator, "/"));
      Path downloadsDir = Paths.get("").resolve("fetchedFiles");
      Files.createDirectories(downloadsDir);
      String downloadFileName = filePath.getFileName().toString();
      Path targetDownloadPath = downloadsDir.resolve(downloadFileName);
      File targetFile = new File(targetDownloadPath.toString());
      Request.Get(builder.build())
              .execute()
              .saveContent(targetFile);
      System.out.println("File has been downloaded to directory: " + downloadsDir.toAbsolutePath());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  void createDirectory(Path directoryPath) {
    try {
      URIBuilder builder = new URIBuilder(loadBalancerUrl + "/directories");
      String path = directoryPath.toString().replace(File.separator, "/");
      builder.addParameter("path", path);
      Request.Post(builder.build())
              .execute()
              .returnContent();
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  void remove(boolean isDirectory, Path targetPath) {
    try {
      URIBuilder builder = new URIBuilder(loadBalancerUrl + (isDirectory ? "/directories" : "/files"));
      String path = targetPath.toString().replace(File.separator, "/");
      builder.addParameter("path", path);
      Request.Delete(builder.build())
              .execute()
              .returnContent();
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  Path fetchAbsolutePath(String path) {
    if (path.startsWith("/")) {
      return Paths.get(path).normalize();
    } else {
      return currentDir.resolve(path).normalize();
    }
  }

  void processCommand(String commandInput) throws IOException {
    try {
      String[] splitArgs = commandInput.split(" ");

      switch (splitArgs[0]) {
        case "Exit":
          active = false;
          break;
        case "Get-ChildItem":
          Path targetPath = splitArgs.length > 1 ? fetchAbsolutePath(splitArgs[1]) : currentDir;
          System.out.println(fetchFiles(targetPath));
          break;
        case "New-Directory":
          String newFolder = splitArgs[1];
          createDirectory(fetchAbsolutePath(newFolder));
          break;
        case "Remove-Directory":
          String folderToDelete = splitArgs[1];
          remove(true, fetchAbsolutePath(folderToDelete));
          break;
        case "Remove-File":
          String fileToRemove = splitArgs[1];
          remove(false, fetchAbsolutePath(fileToRemove));
          break;
        case "Set-Location":
          String newDirectory = splitArgs[1];

          try {
            Path newPath = fetchAbsolutePath(newDirectory);
            fetchFiles(newPath);
            currentDir = newPath;
          } catch (Exception e) {
            System.out.printf("cd: The directory '%s' does not exist%n", newDirectory);
          }
          break;
        case "Get-Location":
          System.out.println(currentDir);
          break;
        case "Get-File":
          fetchFile(fetchAbsolutePath(splitArgs[1]));
          break;
        case "Push-File":
          pushFile(Paths.get(splitArgs[1]));
          break;
        default:
          System.out.println("Invalid Command");
      }
    } catch (ArrayIndexOutOfBoundsException | URISyntaxException e) {
      System.out.println("Error: Invalid Command" + e.getMessage());
    } catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
    }
  }
}
