package com.team01.hrbank.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

    @MappedSuperclass
    @EntityListeners(AuditingEntityListener.class)
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public abstract class BaseEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(nullable = false, updatable = false)
        protected Long id;

        @CreatedDate
        @Column(nullable = false, updatable = false)
        protected Instant createdAt;
    }

