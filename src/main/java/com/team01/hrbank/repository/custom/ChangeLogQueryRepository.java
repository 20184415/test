package com.team01.hrbank.repository.custom;

import com.team01.hrbank.entity.ChangeLog;
import com.team01.hrbank.enums.ChangeType;
import java.time.Instant;
import java.util.List;


public interface ChangeLogQueryRepository {
    List<ChangeLog> findByConditions(
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

    long countByConditions(
        String employeeNumber,
        ChangeType type,
        String memo,
        String ipAddress,
        Instant atFrom,
        Instant atTo
    );
}
