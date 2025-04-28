package com.team01.hrbank.repository;

import com.team01.hrbank.entity.ChangeLogDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// 특정 ChangeLog에 해당하는 ChangeLogDetail을 모두 조회할 때 필요
// 따라서 단독으로 쿼리가 가능해야하므로 별도 repository를 생성해서 메서드로 구현해야함.
// 1:N 구조에서 N이 독립적으로 자주 조회되는 경우 Repository 분리 필요
public interface ChangeLogDetailRepository extends JpaRepository<ChangeLogDetail, Long> {
    // Spring Data JPA 메서드 이름 기반 쿼리 작성 규칙 활용
    // SELECT * FROM change_log_detail WHERE change_log_id = ?
    List<ChangeLogDetail> findAllByChangeLogId(Long changeLogId);
}
