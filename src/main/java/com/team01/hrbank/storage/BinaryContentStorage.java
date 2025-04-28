package com.team01.hrbank.storage;

import java.io.InputStream;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface BinaryContentStorage {

    Long save(Long id, byte[] bytes);

    InputStream get(Long id);

    ResponseEntity<Resource> downloadResponse(Long id);
}
