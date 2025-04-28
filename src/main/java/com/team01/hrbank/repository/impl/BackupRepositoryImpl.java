package com.team01.hrbank.repository.impl;
import static com.team01.hrbank.entity.QBackup.backup;
import static com.team01.hrbank.entity.QBinaryContent.binaryContent;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;


import com.team01.hrbank.dto.backup.CursorRequest;
import com.team01.hrbank.entity.Backup;
import com.team01.hrbank.enums.BackupStatus;
import com.team01.hrbank.repository.custom.BackupCustomRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class BackupRepositoryImpl implements BackupCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public long countBackups(String worker, BackupStatus status, Instant startFrom,
        Instant startTo) {
        Long count = jpaQueryFactory.select(backup.count()).from(backup)
            .where(buildWhereClause(worker, status, startFrom, startTo)).fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public List<Backup> searchWithCursor(String worker, BackupStatus status, Instant startedAtFrom,
        Instant startedAtTo, CursorRequest cursorRequest) {

        Long lastId = null;
        if (cursorRequest.cursor() != null && !cursorRequest.cursor().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(cursorRequest.cursor());
                String decodedJson = new String(decodedBytes,
                    java.nio.charset.StandardCharsets.UTF_8);

                String idStr = decodedJson.replaceAll("[^0-9]", "");
                if (!idStr.isEmpty()) {
                    lastId = Long.parseLong(idStr);
                }
            } catch (IllegalArgumentException e) {
                lastId = null;
                e.printStackTrace();
            }
        }

        int pageSize = cursorRequest.size();
        List<OrderSpecifier<?>> orderSpecifiers = getOrderSpecifiers(cursorRequest.sortField(),
            cursorRequest.direction());

        List<Long> ids = jpaQueryFactory.select(backup.id).from(backup)
            .where(buildWhereClause(worker, status, startedAtFrom, startedAtTo),
                idBasedPagination(lastId, cursorRequest.direction()))
            .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0])).limit(pageSize + 1).fetch();

        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        return jpaQueryFactory.selectDistinct(backup).from(backup)
            .leftJoin(backup.empProfiles, binaryContent).fetchJoin().where(backup.id.in(ids))
            .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0])).fetch();
    }

    // 동적 쿼리 사용 (여러 조건을 하나로 묶음)
    private Predicate buildWhereClause(String worker, BackupStatus status, Instant from,
        Instant to) {

        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(worker)) {
            builder.and(backup.worker.containsIgnoreCase(worker));
        }
        if (status != null) {
            builder.and(backup.status.eq(status));
        }
        if (from != null && to != null) {
            builder.and(backup.startedAt.between(from, to));
        } else if (from != null) {
            builder.and(backup.startedAt.goe(from));
        } else if (to != null) {
            builder.and(backup.startedAt.loe(to));
        }

        return builder;
    }

    private BooleanExpression idBasedPagination(Long lastId, Sort.Direction direction) {
        if (lastId == null) {
            return null;
        }
        return direction == Sort.Direction.DESC ? backup.id.lt(lastId) : backup.id.gt(lastId);
    }

    // oreSpecifier<-정렬 조건 표현 (정렬순서,경로)
    private List<OrderSpecifier<?>> getOrderSpecifiers(String sortField, Sort.Direction direction) {

        List<OrderSpecifier<?>> orders = new ArrayList<>();

        Sort.Direction resolvedDirection;
        resolvedDirection = Objects.requireNonNullElse(direction, Direction.DESC);
        Order dslOrder;
        if (resolvedDirection == Sort.Direction.ASC) {
            dslOrder = Order.ASC;
        } else {
            dslOrder = Order.DESC;
        }

        String validatedSortField;

        if (StringUtils.hasText(sortField) && (sortField.equalsIgnoreCase("startedAt")
            || sortField.equalsIgnoreCase("endedAt"))) {
            validatedSortField = sortField;
        } else {
            validatedSortField = "startedAt";
        }

        PathBuilder<Backup> backupPath = new PathBuilder<>(Backup.class, "backup");

        orders.add(
            new OrderSpecifier<>(dslOrder, backupPath.get(validatedSortField, Comparable.class)));

        return orders;
    }
}