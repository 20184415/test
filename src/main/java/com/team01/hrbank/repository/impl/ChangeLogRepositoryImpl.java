package com.team01.hrbank.repository.impl;

import static com.querydsl.core.types.dsl.Expressions.stringTemplate;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team01.hrbank.entity.ChangeLog;
import com.team01.hrbank.entity.QChangeLog;
import com.team01.hrbank.enums.ChangeType;
import com.team01.hrbank.mapper.ChangeLogMapper;
import com.team01.hrbank.repository.custom.ChangeLogQueryRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor // JPAQueryFactory 주입
public class ChangeLogRepositoryImpl implements ChangeLogQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final ChangeLogMapper changeLogMapper;
    private QChangeLog qChangeLog = QChangeLog.changeLog;

    @Override
    public List<ChangeLog> findByConditions(
        String employeeNumber, ChangeType type, String memo,
        String ipAddress, Instant atFrom, Instant atTo, Long idAfter,
        String sortField, String sortDirection,
        int size
    ) {
        // 조건은 동적으로 누적해나갈 수 있다.
        // 사용자가 입력했을 수도 안 했을 수도 있기 때문에
        BooleanBuilder where = new BooleanBuilder();

        if (employeeNumber != null && !employeeNumber.isBlank()) {
            where.and(qChangeLog.employeeNumber.containsIgnoreCase(employeeNumber));
        }
        // 비교를 위해 enum을 string으로 변환
        if (type != null) {
            where.and(
                stringTemplate("cast({0} as string)", qChangeLog.type)
                    .eq(type.name())
            );
        }
        // 부분 검색이 가능
        // employeeNumber LIKE %xxx% 와 비슷
        if (memo != null && !memo.isBlank()) {
            where.and(qChangeLog.memo.containsIgnoreCase(memo));
        }
        if (ipAddress != null && !ipAddress.isBlank()) {
            where.and(qChangeLog.ipAddress.containsIgnoreCase(ipAddress));
        }
        if (atFrom != null) {
            where.and(qChangeLog.updatedAt.goe(atFrom));
        }
        if (atTo != null) {
            where.and(qChangeLog.updatedAt.loe(atTo));
        }
        // 커서 기반 페이지네이션을 위해 필요하다.
        // gt, lt 두가지를 활용하여 ID를 기준으로 오름, 내림차순 모두 정렬 가능하도록 한다.
        if (idAfter != null) {
            if ("desc".equalsIgnoreCase(sortDirection)) {
                where.and(qChangeLog.id.lt(idAfter)); // 내림차순 → 작은 ID를 기준으로
            } else {
                where.and(qChangeLog.id.gt(idAfter)); // 오름차순 → 큰 ID를 기준으로
            }
        }

        // 정렬 조건
        // 프론트에서 오는 요청에 따라 asc, desc 모두 정렬 가능하다.
        OrderSpecifier<?> order = buildOrderSpecifier(sortField, sortDirection);

        return queryFactory
            .selectFrom(qChangeLog)
            .where(where)
            .orderBy(order)
            .limit(size+1) // 다음 페이지 여부 판단용
            .fetch();
    }

    @Override
    public long countByConditions(
        String employeeNumber, ChangeType type, String memo,
        String ipAddress, Instant atFrom, Instant atTo
    ) {
        BooleanBuilder where = new BooleanBuilder();

        if (employeeNumber != null && !employeeNumber.isBlank()) {
            where.and(qChangeLog.employeeNumber.containsIgnoreCase(employeeNumber));
        }
        // enum type을 string으로 변환하여 비교
        if (type != null) {
            where.and(
                stringTemplate("cast({0} as string)", qChangeLog.type)
                    .eq(type.name())
            );
        }
        if (memo != null && !memo.isBlank()) {
            where.and(qChangeLog.memo.containsIgnoreCase(memo));
        }
        if (ipAddress != null && !ipAddress.isBlank()) {
            where.and(qChangeLog.ipAddress.containsIgnoreCase(ipAddress));
        }
        if (atFrom != null) {
            where.and(qChangeLog.updatedAt.goe(atFrom));
        }
        if (atTo != null) {
            where.and(qChangeLog.updatedAt.loe(atTo));
        }

        return queryFactory
            .select(qChangeLog.count())
            .from(qChangeLog)
            .where(where)
            .fetchOne();
    }

    // 정렬 기준과 방향을 받아서 QueryDSL의 OrderSpecifier 객체를 만드는 역할
    private OrderSpecifier<?> buildOrderSpecifier(String sortField, String sortDirection) {
        // null 또는 asc일 경우 오름차순
        boolean asc = sortDirection == null || sortDirection.equalsIgnoreCase("asc");

        // sortField가 null이면 기본값 "at"으로 대체
        if(sortField == null || sortField.isBlank()){
            sortField = "at";
        }

        // sortField가 지원하지 않는 필드여도 동적 정렬이 가능한 것을 throw로 방지
        return switch (sortField) {
            case "ipAddress" -> asc ? qChangeLog.ipAddress.asc() : qChangeLog.ipAddress.desc();
            case "updatedAt", "at", "" -> asc ? qChangeLog.updatedAt.asc() : qChangeLog.updatedAt.desc();
            default -> throw new IllegalArgumentException("지원하지 않는 정렬 필드입니다: " + sortField);
        };
    }
}
