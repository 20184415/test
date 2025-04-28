package com.team01.hrbank.controller;

import com.team01.hrbank.dto.department.CursorPageResponseDepartmentDto;
import com.team01.hrbank.dto.department.DepartmentCreateRequest;
import com.team01.hrbank.dto.department.DepartmentDto;
import com.team01.hrbank.dto.department.DepartmentUpdateRequest;
import com.team01.hrbank.service.DepartmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<CursorPageResponseDepartmentDto> getDepartments(
        @RequestParam(required = false) String nameOrDescription,
        @RequestParam(required = false) Long idAfter,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "establishedDate") String sortField,
        @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        CursorPageResponseDepartmentDto response = departmentService.getDepartments(
            nameOrDescription, idAfter, cursor, size, sortField, sortDirection
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<DepartmentDto> createDepartment(
        @Valid @RequestBody DepartmentCreateRequest request) {

        DepartmentDto response = departmentService.createDepartment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDto> getDepartment(@PathVariable Long id) {
        DepartmentDto response = departmentService.getDepartment(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DepartmentDto> updateDepartment(
        @PathVariable Long id,
        @Valid @RequestBody DepartmentUpdateRequest request) {

        DepartmentDto response = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(response);
    }

}
