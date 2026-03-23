package com.purpleclay.jewelry.service;

import com.purpleclay.jewelry.exception.BadRequestException;
import com.purpleclay.jewelry.exception.ResourceNotFoundException;
import com.purpleclay.jewelry.model.dto.ProductDTOs;
import com.purpleclay.jewelry.model.entity.Category;
import com.purpleclay.jewelry.repository.CategoryRepository;
import com.purpleclay.jewelry.util.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Cacheable(value = "categories")
    public List<ProductDTOs.CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
            .stream()
            .map(productMapper::toCategoryResponse)
            .toList();
    }

    public ProductDTOs.CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return productMapper.toCategoryResponse(category);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ProductDTOs.CategoryResponse createCategory(ProductDTOs.CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new BadRequestException("Category already exists: " + request.name());
        }

        Category category = Category.builder()
            .name(request.name())
            .description(request.description())
            .imageUrl(request.imageUrl())
            .build();

        category = categoryRepository.save(category);
        log.info("Category created: {}", category.getName());
        return productMapper.toCategoryResponse(category);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ProductDTOs.CategoryResponse updateCategory(Long id, ProductDTOs.CategoryRequest request) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        category.setName(request.name());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());

        return productMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category", id);
        }
        categoryRepository.deleteById(id);
        log.info("Category deleted: {}", id);
    }
}
