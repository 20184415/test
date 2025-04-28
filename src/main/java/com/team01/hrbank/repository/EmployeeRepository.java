package com.team01.hrbank.repository;

import com.team01.hrbank.entity.Employee;

import com.team01.hrbank.repository.custom.EmployeeQueryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, EmployeeQueryRepository {

    boolean existsByEmail(String email);

    boolean existsByDepartmentId(Long departmentId); // 단순히 존재 여부만 판단(조회X)

    long countByDepartmentId(Long departmentId);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("SELECT e FROM Employee e " +
        "LEFT JOIN FETCH e.department " +
        "LEFT JOIN FETCH e.profile " +
        "WHERE e.id = :id")
    Optional<Employee> findWithDetailsById(@Param("id") Long id);
}
