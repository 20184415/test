package com.team01.hrbank.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.springframework.data.annotation.LastModifiedDate;

@MappedSuperclass
    public abstract class BaseUpdatableEntity extends BaseEntity {

        @LastModifiedDate
        @Column
        protected Instant updatedAt;
    }


