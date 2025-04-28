package com.team01.hrbank.controller;

import com.team01.hrbank.entity.BinaryContent;
import com.team01.hrbank.repository.BinaryContentRepository;
import com.team01.hrbank.service.BackupService;
import com.team01.hrbank.storage.BinaryContentStorage;
import com.team01.hrbank.storage.CsvBackupStorage;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class BinaryContentController {

    private final CsvBackupStorage csvBackupStorage;
    private final BackupService backupService;
  
    private final BinaryContentStorage binaryContentStorage;

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        if (id < 10000L) {
            backupService.validateBackupId(id);
            return csvBackupStorage.downloadResponse(id);
        } else {
            return binaryContentStorage.downloadResponse(id);
        }
    }
}
