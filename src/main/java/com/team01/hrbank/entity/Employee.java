package com.team01.hrbank.entity;

import com.team01.hrbank.enums.EmployeeStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Table(name = "employees")
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends BaseUpdatableEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "emp_number", nullable = false, unique = true, updatable = false)
    private String employeeNumber = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private String position;

    @Column(nullable = false)
    private LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EmployeeStatus status;

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinColumn(name = "profile_image_id")
    private BinaryContent profile;

    public Employee(String name, String email, Department department, String position,
        LocalDate hireDate, EmployeeStatus status, BinaryContent profile) {
        this.name = name;
        this.email = email;
        this.department = department;
        this.position = position;
        this.hireDate = hireDate;
        this.status = status;
        this.profile = profile;
    }

    public void update(String name, String email, Department department, String position, LocalDate hireDate, EmployeeStatus status, BinaryContent profile) {
        this.name = name;
        this.email = email;
        this.department = department;
        this.position = position;
        this.hireDate = hireDate;
        this.status = status;
        this.profile = profile;
    }

    @PrePersist
    private void assignEmployeeNumber() {
        if (this.employeeNumber == null) {
          this.employeeNumber = UUID.randomUUID().toString();
        }
    }
}