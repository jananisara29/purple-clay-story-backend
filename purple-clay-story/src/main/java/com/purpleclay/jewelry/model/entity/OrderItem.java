package com.purpleclay.jewelry.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // Customization selections (if product was customized)
    @Column(name = "selected_color")
    private String selectedColor;

    @Column(name = "selected_shape")
    private String selectedShape;

    @Column(name = "selected_hook_type")
    private String selectedHookType;

    // AI-generated preview image URL for this customization
    @Column(name = "custom_preview_url")
    private String customPreviewUrl;
}
