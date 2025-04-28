package com.team01.hrbank.entity;

import com.team01.hrbank.enums.ChangeType;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(name = "change_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class ChangeLog extends BaseUpdatableEntity {

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", nullable = false, columnDefinition = "change_type")
    private ChangeType type; // CREATED, UPDATED, DELETED

    @Column(name = "employee_number", nullable = false)
    private String employeeNumber; // 사번

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    // `updatedAt`을 Swagger의 "at" 필드로 사용 (BaseUpdatableEntity에서 상속됨)
    // changelogServiceImpl 파일에서 detailEntities.forEach(changeLog::addDetail);의 역할을 @OneTomany가 해주고 있음
    @OneToMany(mappedBy = "changeLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChangeLogDetail> details = new ArrayList<>();

    public ChangeLog(ChangeType type, String employeeNumber, String memo, String ipAddress) {
        this.type = type;
        this.employeeNumber = employeeNumber;
        this.memo = memo;
        this.ipAddress = ipAddress;
    }

    public void addDetail(ChangeLogDetail detail) {
        this.details.add(detail);
        detail.setChangeLog(this);
    }

    // BaseUpdatableEntity에 Getter를 추가하는 대신 여기에 updatedAt get함수를 추가
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
