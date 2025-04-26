package com.team01.hrbank.controller;


import com.team01.hrbank.dto.changelog.CursorPageResponseChangeLogDto;
import com.team01.hrbank.dto.changelog.DiffDto;
import com.team01.hrbank.enums.ChangeType;
import com.team01.hrbank.service.ChangeLogService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/change-logs")
public class ChangeLogController {

    private final ChangeLogService changeLogService;

    @GetMapping
    @Operation(summary = "직원 정보 수정 이력 목록 조회")
    public CursorPageResponseChangeLogDto searchChangeLogs(
        // QueryString 방식
        @RequestParam(required = false) String employeeNumber,
        @RequestParam(required = false) ChangeType type,
        @RequestParam(required = false) String memo,
        @RequestParam(required = false) String ipAddress,
        @RequestParam(required = false)
        // ISO-8601형식 시간 쿼리 파싱 지원
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant atFrom,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant atTo,
        @RequestParam(required = false) Long idAfter,
        @RequestParam(defaultValue = "at") String sortField,
        @RequestParam(defaultValue = "desc") String sortDirection,
        @RequestParam(defaultValue = "10") int size
    ) {
        return changeLogService.searchChangeLogs(
            employeeNumber,
            type,
            memo,
            ipAddress,
            atFrom,
            atTo,
            idAfter,
            sortField,
            sortDirection,
            size
        );
    }

    @GetMapping("/{id}/diffs")
    @Operation(summary = "직원 이력 상세 변경 내용 조회")
    public List<DiffDto> findChangeDetails(@PathVariable Long id){
        return changeLogService.findChangeDetails(id);
    }

    @GetMapping("/count")
    @Operation(summary = "직원 정보 수정 이력 건수 조회")
    public Long countChangeLogs(
        @RequestParam(required = false) String employeeNumber,
        @RequestParam(required = false) ChangeType type,
        @RequestParam(required = false) String memo,
        @RequestParam(required = false) String ipAddress,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant atFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant atTo
    ){
        return changeLogService.countChangeLogs(
            employeeNumber,
            type,
            memo,
            ipAddress,
            atFrom,
            atTo
        );
    }
}
