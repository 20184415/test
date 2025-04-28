package com.team01.hrbank.service.impl;


import com.team01.hrbank.dto.binarycontent.BinaryContentCreateRequest;
import com.team01.hrbank.dto.binarycontent.BinaryContentDto;
import com.team01.hrbank.entity.BinaryContent;

import com.team01.hrbank.exception.MismatchedValueException;
import com.team01.hrbank.mapper.BinaryContentMapper;
import com.team01.hrbank.repository.BinaryContentRepository;
import com.team01.hrbank.storage.BinaryContentStorage;
import com.team01.hrbank.service.BinaryContentService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BinaryContentServiceImpl implements BinaryContentService {

    private final BinaryContentRepository binaryContentRepository;
    private final BinaryContentMapper binaryContentMapper;


    @Transactional(readOnly = true)
    @Override
    public BinaryContentDto find(Long binaryContentId) {
        return binaryContentRepository.findById(binaryContentId).map(binaryContentMapper::toDto)
            .orElseThrow(() -> new NoSuchElementException("프로필 아이디가 존재하지 않습니다."));
    }


    @Transactional
    @Override
    public void delete(Long binaryContentId) {
        if (!binaryContentRepository.existsById(binaryContentId)) {
            throw new NoSuchElementException("요청한 프로필 사진이 존재하지 않습니다");
        }
        binaryContentRepository.deleteById(binaryContentId);
    }


}


