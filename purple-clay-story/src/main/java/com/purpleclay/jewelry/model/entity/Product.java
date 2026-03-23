package com.purpleclay.jewelry.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_category", columnList = "category_id"),
    @Index(name = "idx_product_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // AI-generated description stored separately
    @Column(name = "ai_description", columnDefinition = "TEXT")
    private String aiDescription;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity")
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(name = "image_url")
    private String imageUrl;

    // Base image used for AI customization overlay
    @Column(name = "base_image_url")
    private String baseImageUrl;

    // Customization options stored as JSON string
    @Column(name = "available_colors", columnDefinition = "TEXT")
    private String availableColors; // JSON array: ["gold","silver","rose-gold"]

    @Column(name = "available_shapes", columnDefinition = "TEXT")
    private String availableShapes; // JSON array: ["round","teardrop","square"]

    @Column(name = "available_hook_types", columnDefinition = "TEXT")
    private String availableHookTypes; // JSON array: ["stud","hook","clip"]

    @Column(name = "is_customizable")
    @Builder.Default
    private boolean customizable = false;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();
}
