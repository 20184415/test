package com.team01.hrbank.repository.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team01.hrbank.dto.department.DepartmentDto;
import com.team01.hrbank.entity.Department;
import com.team01.hrbank.entity.QDepartment;
import com.team01.hrbank.mapper.DepartmentMapper;
import com.team01.hrbank.repository.EmployeeRepository;
import com.team01.hrbank.repository.custom.DepartmentQueryRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DepartmentRepositoryImpl implements DepartmentQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final QDepartment department = QDepartment.department;
    private final EmployeeRepository employeeRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    public List<DepartmentDto> findDepartmentsWithConditions(
        String nameOrDescription, String cursor, Long idAfter,
        String sortField, String sortDirection, int size
    ) {

        BooleanBuilder whereClause = new BooleanBuilder(); // 동적 where 조건 (처음에 비어있는 조건 생성)

        // 검색 조건 처리: name 또는 description 부분 일치 검색
        if (nameOrDescription != null && !nameOrDescription.isEmpty()) {
            whereClause.and(department.name.containsIgnoreCase(nameOrDescription)
                .or(department.description.containsIgnoreCase(nameOrDescription)));
        }

        // 정렬 필드 지정
        OrderSpecifier<?> orderSpecifier;
        if ("name".equalsIgnoreCase(sortField)) {
            orderSpecifier = "desc".equalsIgnoreCase(sortDirection)
                ? department.name.desc()
                : department.name.asc();
        } else {
            orderSpecifier = "desc".equalsIgnoreCase(sortDirection)
                ? department.establishedDate.desc()
                : department.establishedDate.asc();
        }

        // 커서 기반 페이지 네이션
        if (cursor != null && idAfter != null) {
            // 설립일 기준 정렬일 때
            if ("establishedDate".equalsIgnoreCase(sortField)) {
                LocalDate cursorDate = LocalDate.parse(cursor);

                if ("desc".equalsIgnoreCase(sortDirection)) {
                    // 내림 차순
                    whereClause.and(
                        department.establishedDate.lt(cursorDate)
                            .or(department.establishedDate.eq(cursorDate)
                                .and(department.id.lt(idAfter)))
                    );
                } else {
                    // 오름 차순
                    whereClause.and(
                        department.establishedDate.gt(cursorDate)
                            .or(department.establishedDate.eq(cursorDate)
                                .and(department.id.gt(idAfter)))
                    );
                }
            // 이름 기준 정렬일 때
            } else if ("name".equalsIgnoreCase(sortField)) {
                if ("desc".equalsIgnoreCase(sortDirection)) {
                    // 내림 차순
                    whereClause.and(
                        department.name.lt(cursor)
                            .or(department.name.eq(cursor)
                                .and(department.id.lt(idAfter)))
                    );
                } else {
                    // 오름 차순
                    whereClause.and(
                        department.name.gt(cursor)
                            .or(department.name.eq(cursor)
                                .and(department.id.gt(idAfter)))
                    );
                }
            }
        }

        // QueryDSL 쿼리 실행: 부서 목록 조회
        List<Department> departments = queryFactory
            .selectFrom(department)
            .where(whereClause)
            .orderBy(orderSpecifier)
            .limit(size + 1) // 다음 페이지 존재 여부 확인 (size + 1개일 때 hasNext = true, 그렇지 않으면 false)
            .fetch();

        // 직원 수 계산 및 DTO 변환
        return departments.stream()
            .map(dep -> {
                Long employeeCount = employeeRepository.countByDepartmentId(dep.getId());
                return departmentMapper.toDto(dep, employeeCount);
            })
            .toList();
    }
}
