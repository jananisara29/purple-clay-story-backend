package com.purpleclay.jewelry.controller;

import com.purpleclay.jewelry.ai.AICustomizationService;
import com.purpleclay.jewelry.model.dto.AIDTOs;
import com.purpleclay.jewelry.model.dto.ProductDTOs;
import com.purpleclay.jewelry.service.ProductService;
import com.purpleclay.jewelry.util.ProductMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/customize")
@RequiredArgsConstructor
@Tag(name = "AI - Customization", description = "Customer product customization with DALL-E preview")
public class AICustomizationController {

    private final AICustomizationService aiCustomizationService;
    private final ProductService productService;

    @PostMapping("/preview")
    @Operation(summary = "Generate AI customization preview — DALL-E image + overlay",
               description = """
                   Customer selects color, shape, hook type.
                   Returns:
                   - generatedImageUrl: raw DALL-E 3 image URL (expires in 1 hour)
                   - overlayImageUrl: base64 PNG with AI preview composited on product base image
                   - description: AI-written description of the customized piece
                   Results are cached in Redis for 6 hours per unique combination.
                   """)
    public ResponseEntity<AIDTOs.AICustomizationResponse> generatePreview(
        @RequestBody AIDTOs.AICustomizationRequest request
    ) {
        return ResponseEntity.ok(aiCustomizationService.generatePreview(request));
    }

    @GetMapping("/options/{productId}")
    @Operation(summary = "Get available customization options for a product")
    public ResponseEntity<CustomizationOptionsResponse> getOptions(@PathVariable Long productId) {
        ProductDTOs.ProductResponse product = productService.getProductById(productId);

        if (!product.customizable()) {
            return ResponseEntity.ok(new CustomizationOptionsResponse(
                productId, product.name(), false,
                java.util.List.of(), java.util.List.of(), java.util.List.of()
            ));
        }

        return ResponseEntity.ok(new CustomizationOptionsResponse(
            productId,
            product.name(),
            true,
            product.availableColors(),
            product.availableShapes(),
            product.availableHookTypes()
        ));
    }

    public record CustomizationOptionsResponse(
        Long productId,
        String productName,
        boolean customizable,
        java.util.List<String> availableColors,
        java.util.List<String> availableShapes,
        java.util.List<String> availableHookTypes
    ) {}
}
