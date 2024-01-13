package com.example.node.controller;

import static com.example.node.service.OperationStates.TXN_FAILED;
import static com.example.node.service.OperationStates.TXN_PROPOSE_FAILED;
import static com.example.node.service.OperationStates.TXN_PROPOSE_SUCCESS;
import static com.example.node.service.OperationStates.TXN_SUCCESS;
import static com.example.node.service.OperationStates.TXN_UNKNOWN_MODE;
import static com.example.node.utils.PaxosStep.COMMIT;
import static com.example.node.utils.PaxosStep.PROPOSE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import java.net.URISyntaxException;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.fluent.Request;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.net.URIBuilder;
import com.example.node.service.Operation;
import com.example.node.service.OperationImpl;
import com.example.node.service.StorageService;
import com.example.node.utils.PaxosStep;
import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * The actual controller class that works with springboot and the storage service object.
 */
@Controller
@PropertySource(value={"classpath:application.properties"})
public class FileManagementController implements
    FileController {

  @Value("${node.urls}")

  private String[] nodeUrls;
  private final StorageService fileStore;
  private String path;

  @Autowired
  public FileManagementController(StorageService fileStore) {
    this.fileStore = fileStore;
  }

  /**
   * Get all os object (files and directories under a particular directory
   *
   * @param path The path  of the root directory
   * @return A string[] with all the names of the files and folders
   */

  @GetMapping("/directories")
  @Override
  public ResponseEntity<?> fetchSubDirectories(
      @RequestParam(value = "path") String path) {
    if(path == null || path.equals("\\") || path.equals("/") || path.equals("")) {
      path = File.separator;
    }

    try {
      Stream<Path> pathStream = fileStore.loadAllByPath(path);
      Stream<String> allPaths = pathStream.map(Path::toString);
      String filesAndFolders = String.join(" ", allPaths.collect(Collectors.toList()));

      return ResponseEntity.ok(filesAndFolders);
    } catch (NoSuchFileException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Path not valid or directory does not exist");
    }

  }

  @PostMapping("/directories")
  @ResponseBody
  @Override
  public ResponseEntity makeDirectory(@RequestParam(value = "path") String path,
                                      @RequestParam(required = false, value = "step") PaxosStep step) {

    if (step == null) {
      step = PaxosStep.PREPARE;
    }

    switch (step) {
      case COMMIT:
        fileStore.createDirectory(path);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(String.format("Directory %s created", path));
      case PREPARE:

        //assign this node as the leader and start the paxos algorithm
        Boolean proposal = this.sendPaxosMessage(new OperationImpl(null, path), "/directories",
            "create", PROPOSE);

        if (!proposal) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_PROPOSE_FAILED);
        }
        Boolean commit = this.sendPaxosMessage(new OperationImpl(null, path), "/directories",
            "create", COMMIT);
        if (!commit) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_FAILED);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(TXN_SUCCESS);
      case PROPOSE:
        return ResponseEntity.status(HttpStatus.OK).body(TXN_PROPOSE_SUCCESS);
      default:
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_UNKNOWN_MODE);
    }

  }

  @DeleteMapping("/directories")
  @ResponseBody
  @Override
  public ResponseEntity<?> deleteDirectory(@RequestParam(value = "path") String path,
                                           @RequestParam(required = false, value = "step") PaxosStep step) {
    if (step == null) {
      step = PaxosStep.PREPARE;
    }
    switch (step) {
      case COMMIT:
        fileStore.deleteDirectory(path);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(String.format("Directory %s deleted", path));
      case PREPARE:

        //assign this node as the leader and start the paxos algorithm
        Boolean proposal = this.sendPaxosMessage(new OperationImpl(null, path), "/directories",
            "delete", PROPOSE);

        if (!proposal) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_PROPOSE_FAILED);
        }
        Boolean commit = this.sendPaxosMessage(new OperationImpl(null, path), "/directories",
            "delete", COMMIT);
        if (!commit) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_FAILED);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(TXN_SUCCESS);
      case PROPOSE:
        return ResponseEntity.status(HttpStatus.OK).body(TXN_PROPOSE_SUCCESS);
      default:
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_UNKNOWN_MODE);
    }

  }

  @GetMapping("/files")
  @ResponseBody
  @Override
  public ResponseEntity<Resource> fetchFile(@RequestParam(value = "path") String path) {
    Resource file = fileStore.loadAsResource(path);
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"" + file.getFilename() + "\"").body(file);

  }

  @RequestMapping(path = "/files", method = POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @Override
  public ResponseEntity<?> pushFile(@RequestBody MultipartFile file,
                                    @RequestParam("path") String path, @RequestParam(required = false, value = "step") PaxosStep step) {
    if (step == null) {
      step = PaxosStep.PREPARE;
    }
    switch (step) {
      case COMMIT:
        fileStore.store(path, file);
        return ResponseEntity.status(HttpStatus.CREATED).body("File uploaded");
      case PREPARE:
        File tmp = null;
        if (file != null) {
          tmp = new File(System.getProperty("java.io.tmpdir") + File.separator + file.getOriginalFilename());
          try {
            file.transferTo(tmp);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
        Boolean proposal = this.sendPaxosMessage(new OperationImpl(tmp, path), "/files",
            "create", PROPOSE);
        if (!proposal) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_PROPOSE_FAILED);
        }

        Boolean commit = this.sendPaxosMessage(new OperationImpl(tmp, path), "/files", "create",
            COMMIT);
        if (!commit) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_FAILED);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body("File uploaded");
      case PROPOSE:
        return ResponseEntity.status(HttpStatus.OK).body(TXN_PROPOSE_SUCCESS);
      default:
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_UNKNOWN_MODE);
    }
  }

  @DeleteMapping("/files")
  @ResponseBody
  @Override
  public ResponseEntity<?> deleteFile(@RequestParam(value = "path") String path,
                                      @RequestParam(required = false, value = "step") PaxosStep step) {
    if (step == null) {
      step = PaxosStep.PREPARE;
    }
    switch (step) {
      case COMMIT:
        fileStore.deleteFile(path);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(String.format("File %s deleted", path));
      case PREPARE:

        //assign this node as the leader and start the paxos algorithm
        Boolean proposal = this.sendPaxosMessage(new OperationImpl(null, path), "/files",
            "delete", PROPOSE);

        if (!proposal) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_PROPOSE_FAILED);
        }
        Boolean commit = this.sendPaxosMessage(new OperationImpl(null, path), "/files",
            "delete", COMMIT);
        if (!commit) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_FAILED);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(TXN_SUCCESS);
      case PROPOSE:
        return ResponseEntity.status(HttpStatus.OK).body(TXN_PROPOSE_SUCCESS);
      default:
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_UNKNOWN_MODE);
    }
  }

  @ExceptionHandler(FileNotFoundException.class)
  @Override
  public ResponseEntity<?> handleStorageFileNotFound(FileNotFoundException exc) {

    return ResponseEntity.notFound().build();
  }

  private Boolean sendPaxosMessage(Operation operation, String url, String type, PaxosStep step) {
    if (type == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "need a proposal type");
    }
    if (type.equals("create")) {
      AtomicInteger successes = new AtomicInteger();
      System.out.println("THE VALUE OF STEP is " + step);
      for (String node : this.nodeUrls) {
        Thread t = new Thread(() -> {
          URIBuilder uriBuilder = null;
          try {
            uriBuilder = new URIBuilder(node + url);
          } catch (URISyntaxException e) {
            throw new RuntimeException(e);
          }
          uriBuilder.addParameter("path", operation.getPath());
          uriBuilder.addParameter("step", String.valueOf(step));

          CloseableHttpClient httpclient = HttpClients.createDefault();
          HttpPost httpPost = null;

          try {
            httpPost = new HttpPost(uriBuilder.build());
          } catch (URISyntaxException e) {
            throw new RuntimeException(e);
          }

          MultipartEntityBuilder builder = MultipartEntityBuilder.create();
          if (operation.getFile() != null) {
            builder.addPart("file", new FileBody(operation.getFile()));
            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);
          }

          ClassicHttpResponse response = null;
          try {
            response = (ClassicHttpResponse) httpclient.execute(httpPost);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }

          System.out.println(step + " response is " + response);
          InputStream inputStream = null;
          try {
            inputStream = response.getEntity().getContent();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          String result;
          try {
            result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          if (step.equals(PROPOSE)) {
            if (response.getCode() == 200) {
              successes.addAndGet(1);
            }
          } else if (step.equals(COMMIT)) {
            if (response.getCode() == 201) {
              successes.addAndGet(1);
            }
          }
        });

        t.start();

        try {
          t.join();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      int mid = this.nodeUrls.length / 2 + 1;
      if (successes.get() >= mid) {
        return true;
      } else {
        return false;
      }
    } else if (type.equals("delete")) {
      AtomicInteger successes = new AtomicInteger();
      System.out.println("THE VALUE OF STEP is " + step);
      for (String node : this.nodeUrls) {
        Thread t = new Thread(() -> {
          try {
            URIBuilder uriBuilder = new URIBuilder(node + url);
            uriBuilder.addParameter("path", operation.getPath());
            if (step.equals(PROPOSE)) {
              uriBuilder.addParameter("step", String.valueOf(PROPOSE));
            } else if (step.equals(COMMIT)) {
              uriBuilder.addParameter("step", String.valueOf(COMMIT));
            }
            System.out.println(Request.Delete(uriBuilder.build()).execute().returnContent());
          } catch (URISyntaxException | IOException ex) {
            throw new RuntimeException(ex);
          }
          successes.addAndGet(1);
        });
        t.start();
        try {
          t.join();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      int mid = this.nodeUrls.length / 2 + 1;
      if (successes.get() >= mid) {
        return true;
      } else {
        return false;
      }

    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Give the right type for propose");
    }

  }


}
