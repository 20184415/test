package com.team01.hrbank.service;

import com.team01.hrbank.dto.changelog.ChangeLogDto;
import com.team01.hrbank.dto.changelog.CursorPageResponseChangeLogDto;
import com.team01.hrbank.dto.changelog.DiffDto;
import com.team01.hrbank.enums.ChangeType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ChangeLogService {
    /**
     * 변경 이력을 저장합니다.
     *
     * @param type 변경 유형 (직원 CREATED, UPDATED, DELETED)
     * @param employeeNumber 대상 직원 사번
     * @param details 변경 상세 리스트 (DiffDto)
     * @param memo 선택적 메모
     * @param ipAddress 요청자의 IP 주소
     */

    // 이력 등록
    void save(
        ChangeType type,
        String employeeNumber,
        List<DiffDto> details,
        String memo,
        String ipAddress
    );

    CursorPageResponseChangeLogDto searchChangeLogs(
        String employeeNumber,
        ChangeType type,
        String memo,
        String ipAddress,
        Instant atFrom,
        Instant atTo,
        Long idAfter,
        String sortField,
        String sortDirection,
        int size
    );

    // 상세 변경 내역 조회
    List<DiffDto> findChangeDetails(Long changeLogId);

    // 수정 이력 건수 조회
    long countChangeLogs(
        String employeeNumber,
        ChangeType type,
        String memo,
        String ipAddress,
        Instant atFrom,
        Instant atTo
    );
}
