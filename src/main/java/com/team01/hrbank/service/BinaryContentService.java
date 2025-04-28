package com.team01.hrbank.service;

import com.team01.hrbank.dto.binarycontent.BinaryContentDto;
import org.springframework.transaction.annotation.Transactional;

public interface BinaryContentService {


    @Transactional(readOnly = true)
    BinaryContentDto find(Long binaryContentId);

    @Transactional
    void delete(Long binaryContentId);


}
