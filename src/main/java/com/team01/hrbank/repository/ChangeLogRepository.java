package com.team01.hrbank.repository;

import com.team01.hrbank.entity.ChangeLog;
import com.team01.hrbank.repository.custom.ChangeLogQueryRepository;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

// Query상속하는 이유 = findByConditions, countByConditions 사용위하여
public interface ChangeLogRepository extends JpaRepository<ChangeLog, Long>, ChangeLogQueryRepository {

    boolean existsByCreatedAtAfter(Instant createdAtAfter);
}

