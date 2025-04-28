package com.team01.hrbank.repository.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team01.hrbank.dto.employee.CursorPageRequestEmployeeDto;
import com.team01.hrbank.dto.employee.CursorPageResponseEmployeeDto;
import com.team01.hrbank.dto.employee.EmployeeDistributionDto;
import com.team01.hrbank.dto.employee.EmployeeDto;
import com.team01.hrbank.dto.employee.EmployeeTrendDto;
import com.team01.hrbank.entity.QEmployee;
import com.team01.hrbank.enums.EmployeeStatus;
import com.team01.hrbank.enums.TimeUnit;
import com.team01.hrbank.repository.custom.EmployeeQueryRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EmployeeRepositoryImpl implements EmployeeQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final QEmployee employee = QEmployee.employee;

    @Override
    public CursorPageResponseEmployeeDto<EmployeeDto> findEmployeeByCursor(
        CursorPageRequestEmployeeDto request
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        // 필터 조건
        if (request.nameOrEmail() != null && !request.nameOrEmail().isEmpty()) {
            builder.and(employee.name.containsIgnoreCase(request.nameOrEmail())
                .or(employee.email.containsIgnoreCase(request.nameOrEmail())));
        }
        if (request.employeeNumber() != null && !request.employeeNumber().isBlank()) {
            builder.and(employee.employeeNumber.contains(request.employeeNumber()));
        }
        if (request.departmentName() != null && !request.departmentName().isBlank()) {
            builder.and(employee.department.name.contains(request.departmentName()));
        }
        if (request.position() != null && !request.position().isBlank()) {
            builder.and(employee.position.contains(request.position()));
        }
        if (request.hireDateFrom() != null) {
            builder.and(employee.hireDate.goe(request.hireDateFrom()));
        }
        if (request.hireDateTo() != null) {
            builder.and(employee.hireDate.loe(request.hireDateTo()));
        }
        if (request.status() != null) {
            builder.and(employee.status.eq(request.status()));
        }

        long total = Optional.ofNullable(
            queryFactory.select(employee.count())
                .from(employee)
                .where(builder)
                .fetchOne()
        ).orElse(0L);

        // 커서 조건 (정렬 필드 + cursor + idAfter 기준)
        if (request.cursor() != null) {
            builder.and(buildSortCondition(request.cursor(), request.sortField(),
                request.sortDirection(), request.idAfter()));
        }

        // 정렬
        OrderSpecifier<?> orderSpecifier = getOrderSpecifier(request.sortField(),
            request.sortDirection());

        // 데이터 조회
        List<EmployeeDto> results = queryFactory
            .select(Projections.constructor(EmployeeDto.class,
                employee.id,
                employee.name,
                employee.email,
                employee.employeeNumber,
                employee.department.name,
                employee.position,
                employee.hireDate,
                employee.status.stringValue(),
                employee.profile.id
            ))
            .from(employee)
            .where(builder)
            .orderBy(orderSpecifier, employee.id.asc()) // 정렬 필드 다음에 id 정렬
            .limit(request.size() + 1)
            .fetch();

        boolean hasNext = results.size() > request.size();
        List<EmployeeDto> content = hasNext ? results.subList(0, request.size()) : results;

        String nextCursor = null;
        Long nextId = null;
        if (hasNext) {
            EmployeeDto last = content.get(content.size() - 1);
            nextCursor = switch (request.sortField()) {
                case "employeeNumber" -> last.employeeNumber();
                case "hireDate" -> last.hireDate().toString();
                default -> last.name();
            };
            nextId = last.id();
        }

        return new CursorPageResponseEmployeeDto<>(
            content, nextCursor, nextId, request.size(), total, hasNext
        );
    }

    @Override
    public List<EmployeeDistributionDto> findDistributionBy(String groupBy, EmployeeStatus status) {
        Expression<String> groupExpression = groupBy.equals("position")
            ? employee.position
            : employee.department.name;

        // 전체 수 (percent 계산용)
        long totalCount = Optional.ofNullable(
            queryFactory.select(employee.count())
                .from(employee)
                .where(employee.status.eq(status))
                .fetchOne()
        ).orElse(0L);

        if (totalCount == 0) return List.of();

        return queryFactory
            .select(Projections.constructor(
                EmployeeDistributionDto.class,
                groupExpression,
                employee.count(),
                employee.count().doubleValue().divide((double) totalCount).multiply(100)
            ))
            .from(employee)
            .where(employee.status.eq(status))
            .groupBy(groupExpression)
            .orderBy(employee.count().desc())
            .fetch();
    }

    @Override
    public List<EmployeeTrendDto> findEmployeeTrend(LocalDate from, LocalDate to, String unit) {
        List<LocalDate> periods = generatePeriods(from, to, unit);

        // 누적 직원 수 (첫 번째 데이터 포인트)
        Long initialCount = queryFactory
            .select(employee.count())
            .from(employee)
            .where(
                employee.hireDate.lt(from),
                employee.status.ne(EmployeeStatus.RESIGNED)
                    .or(employee.updatedAt.isNull())
                    .or(employee.updatedAt.gt(from.atStartOfDay(ZoneId.systemDefault()).toInstant()))
            )
            .fetchOne();

        List<EmployeeTrendDto> trends = new ArrayList<>();

        Long previousCount = initialCount;
        trends.add(new EmployeeTrendDto(
            periods.get(0),
            previousCount,
            0L,
            0.0
        ));

        for (int i = 1; i < periods.size(); i++) {
            LocalDate end = periods.get(i);

            Long currentCount = queryFactory
                .select(employee.count())
                .from(employee)
                .where(
                    employee.hireDate.loe(end),
                    employee.status.ne(EmployeeStatus.RESIGNED)
                        .or(employee.updatedAt.isNull())
                        .or(employee.updatedAt.gt(end.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()))
                )
                .fetchOne();

            Long change = currentCount - previousCount;
            Double changeRate = previousCount == 0 ? 0.0 : (change.doubleValue() / previousCount) * 100;

            trends.add(new EmployeeTrendDto(
                end,
                currentCount,
                change,
                Math.round(changeRate * 100) / 100.0
            ));

            previousCount = currentCount;
        }

        return trends;
    }

    @Override
    public Long employeeCountBy(EmployeeStatus status, LocalDate fromDate, LocalDate toDate) {
        return queryFactory
            .select(employee.count())
            .from(employee)
            .where(
                status != null ? employee.status.eq(status) : null,
                fromDate != null ? employee.hireDate.goe(fromDate) : null,
                toDate != null ? employee.hireDate.loe(toDate) : null
            )
            .fetchOne();
    }

    private OrderSpecifier<?> getOrderSpecifier(String sortField, String sortDirection) {
        Order order = "asc".equalsIgnoreCase(sortDirection) ? Order.ASC : Order.DESC;

        return switch (sortField) {
            case "employeeNumber" -> new OrderSpecifier<>(order, employee.employeeNumber);
            case "hireDate" -> new OrderSpecifier<>(order, employee.hireDate);
            default -> new OrderSpecifier<>(order, employee.name);
        };
    }

    private List<LocalDate> generatePeriods(LocalDate from, LocalDate to, String unit) {
        List<LocalDate> periods = new ArrayList<>();
        LocalDate current = from;

        while (!current.isAfter(to)) {
            periods.add(current);
            current = switch (unit.toLowerCase()) {
                case "day" -> current.plusDays(1);
                case "week" -> current.plusWeeks(1);
                case "quarter" -> current.plusMonths(3);
                case "year" -> current.plusYears(1);
                default -> current.plusMonths(1); // month 기본값
            };
        }

        return periods;
    }

    private BooleanExpression buildSortCondition(String cursor, String sortField, String sortDirection, Long idAfter) {
        if (cursor == null) {
            return null;
        }

      return switch (sortField) {
        case "employeeNumber" -> buildEmployeeNumberCondition(cursor, sortDirection, idAfter);
        case "hireDate" -> buildHireDateCondition(cursor, sortDirection, idAfter);
        default -> buildNameCondition(cursor, sortDirection, idAfter);
      };
    }

    private BooleanExpression buildEmployeeNumberCondition(String cursor, String sortDirection, Long idAfter) {
        if ("desc".equalsIgnoreCase(sortDirection)) {
            return employee.employeeNumber.lt(cursor)
                .or(employee.employeeNumber.eq(cursor).and(employee.id.lt(idAfter)));
        } else {
            return employee.employeeNumber.gt(cursor)
                .or(employee.employeeNumber.eq(cursor).and(employee.id.gt(idAfter)));
        }
    }

    private BooleanExpression buildHireDateCondition(String cursor, String sortDirection, Long idAfter) {
        LocalDate hireCursor = LocalDate.parse(cursor);
        if ("desc".equalsIgnoreCase(sortDirection)) {
            return employee.hireDate.lt(hireCursor)
                .or(employee.hireDate.eq(hireCursor).and(employee.id.lt(idAfter)));
        } else {
            return employee.hireDate.gt(hireCursor)
                .or(employee.hireDate.eq(hireCursor).and(employee.id.gt(idAfter)));
        }
    }

    private BooleanExpression buildNameCondition(String cursor, String sortDirection, Long idAfter) {
        if ("desc".equalsIgnoreCase(sortDirection)) {
            return employee.name.lt(cursor)
                .or(employee.name.eq(cursor).and(employee.id.lt(idAfter)));
        } else {
            return employee.name.gt(cursor)
                .or(employee.name.eq(cursor).and(employee.id.gt(idAfter)));
        }
    }
}
