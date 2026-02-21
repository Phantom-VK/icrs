package com.college.icrs.repository;

import com.college.icrs.model.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {
    List<Subcategory> findByCategoryId(Long categoryId);
    Optional<Subcategory> findByNameIgnoreCaseAndCategoryId(String name, Long categoryId);
}
