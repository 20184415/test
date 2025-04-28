package com.team01.hrbank.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "change_log_details")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChangeLogDetail extends BaseUpdatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "change_log_id", nullable = false)
    private ChangeLog changeLog;

    @Column(nullable = false)
    private String propertyName;

    @Column(name = "before_value")
    private String before;

    @Column(name = "after_value")
    private String after;

    public ChangeLogDetail(ChangeLog changeLog, String propertyName, String before, String after) {
        this.changeLog = changeLog;
        this.propertyName = propertyName;
        this.before = before;
        this.after = after;
    }

    public void setChangeLog(ChangeLog changeLog) {
        this.changeLog = changeLog;
    }
}
