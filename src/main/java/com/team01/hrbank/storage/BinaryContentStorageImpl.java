package com.team01.hrbank.storage;

import com.team01.hrbank.exception.FileOperationException;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class BinaryContentStorageImpl implements BinaryContentStorage {

    private final Path root;

    public BinaryContentStorageImpl() {
        this.root = Paths.get("./images").toAbsolutePath().normalize();
        this.storagePath = this.root.toString();
    }
    private String storagePath;

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(storagePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new FileOperationException("스토리지 디렉토리를 생성할 수 없습니다.");
        }
    }

    @Override
    public Long save(Long id, byte[] bytes) {
        try {
            if (id == null) {
                throw new FileOperationException("ID가 필요합니다.");
            }
            
            String filename = id + ".bin";
            Path filePath = Paths.get(storagePath, filename);
            Files.write(filePath, bytes);
            return id;
        } catch (IOException e) {
            throw new FileOperationException("파일을 저장할 수 없습니다.");
        }
    }

    @Override
    public InputStream get(Long id) {
        try {
            Path path = Paths.get(storagePath, id + ".bin");
            return new FileInputStream(path.toFile());
        } catch (IOException e) {
            throw new FileOperationException("파일을 찾을 수 없습니다. ID: " + id);
        }
    }

    @Override
    public ResponseEntity<Resource> downloadResponse(Long id) {
        try {
            Path path = Paths.get(storagePath, id + ".bin");
            File file = path.toFile();
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = Files.readAllBytes(path);
            ByteArrayResource resource = new ByteArrayResource(fileContent);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + id + ".bin");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileContent.length)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IOException e) {
            throw new FileOperationException("파일을 다운로드할 수 없습니다. ID: " + id);
        }
    }
}
