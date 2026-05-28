package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.CatalogClient;
import com.afya.platform.bff.dto.DepartmentRequest;
import com.afya.platform.bff.dto.DepartmentResponse;
import com.afya.platform.bff.support.AuthorizationSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentBffController {

    private final CatalogClient catalogClient;

    public DepartmentBffController(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    @GetMapping
    public List<DepartmentResponse> list(HttpServletRequest request) {
        return catalogClient.listDepartments(AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/{id}")
    public DepartmentResponse get(@PathVariable Long id, HttpServletRequest request) {
        return catalogClient.getDepartment(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse create(
            @Valid @RequestBody DepartmentRequest body,
            HttpServletRequest request
    ) {
        return catalogClient.createDepartment(body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PutMapping("/{id}")
    public DepartmentResponse update(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest body,
            HttpServletRequest request
    ) {
        return catalogClient.updateDepartment(
                id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, HttpServletRequest request) {
        catalogClient.deleteDepartment(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }
}
