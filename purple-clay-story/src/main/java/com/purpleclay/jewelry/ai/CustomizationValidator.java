package com.purpleclay.jewelry.ai;

import com.purpleclay.jewelry.exception.BadRequestException;
import com.purpleclay.jewelry.model.dto.AIDTOs;
import com.purpleclay.jewelry.model.entity.Product;
import com.purpleclay.jewelry.util.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomizationValidator {

    private final ProductMapper productMapper;

    public void validate(Product product, AIDTOs.AICustomizationRequest request) {
        if (!product.isCustomizable()) {
            throw new BadRequestException("Product '" + product.getName() + "' is not customizable");
        }

        if (request.color() != null) {
            List<String> allowed = productMapper.parseJsonList(product.getAvailableColors());
            if (!allowed.isEmpty() && !allowed.contains(request.color())) {
                throw new BadRequestException(
                    "Color '" + request.color() + "' not available. Choose from: " + allowed
                );
            }
        }

        if (request.shape() != null) {
            List<String> allowed = productMapper.parseJsonList(product.getAvailableShapes());
            if (!allowed.isEmpty() && !allowed.contains(request.shape())) {
                throw new BadRequestException(
                    "Shape '" + request.shape() + "' not available. Choose from: " + allowed
                );
            }
        }

        if (request.hookType() != null) {
            List<String> allowed = productMapper.parseJsonList(product.getAvailableHookTypes());
            if (!allowed.isEmpty() && !allowed.contains(request.hookType())) {
                throw new BadRequestException(
                    "Hook type '" + request.hookType() + "' not available. Choose from: " + allowed
                );
            }
        }
    }
}
