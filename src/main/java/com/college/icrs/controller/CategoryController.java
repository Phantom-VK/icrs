package com.college.icrs.controller;

import com.college.icrs.dto.CategoryResponseDTO;
import com.college.icrs.service.CategoryCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryCatalogService categoryCatalogService;

    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> listCategories() {
        return ResponseEntity.ok(categoryCatalogService.listCatalog());
    }
}
