package com.team01.hrbank.repository.custom;

import com.team01.hrbank.dto.backup.CursorRequest;

import com.team01.hrbank.entity.Backup;
import com.team01.hrbank.enums.BackupStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.repository.query.Param;

public interface BackupCustomRepository {


    long countBackups(@Param("worker") String worker, @Param("status") BackupStatus status,
        @Param("startFrom") Instant startFrom, @Param("startTo") Instant startTo);

    List<Backup> searchWithCursor(String worker, BackupStatus status, Instant startedAtFrom,
        Instant startedAtTo, CursorRequest cursorRequest);
}
