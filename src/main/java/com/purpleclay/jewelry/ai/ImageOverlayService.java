package com.purpleclay.jewelry.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Base64;

@Service
@Slf4j
public class ImageOverlayService {

    /**
     * Composites DALL-E generated image over the product base image.
     * Strategy: place AI image as a styled preview inset on the base product image.
     * Returns base64 encoded PNG.
     */
    public String overlayImages(String baseImageUrl, String aiGeneratedImageUrl) {
        try {
            BufferedImage baseImage = loadImageFromUrl(baseImageUrl);
            BufferedImage aiImage = loadImageFromUrl(aiGeneratedImageUrl);

            int width = baseImage.getWidth();
            int height = baseImage.getHeight();

            // Create canvas using base image dimensions
            BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = canvas.createGraphics();

            // Enable anti-aliasing for quality
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            // Draw base product image as full background
            g2d.drawImage(baseImage, 0, 0, width, height, null);

            // Draw semi-transparent dark overlay on bottom half (for preview label area)
            g2d.setColor(new Color(0, 0, 0, 80));
            g2d.fillRect(0, height - 60, width, 60);

            // Inset AI preview: bottom-right corner, 40% size with rounded border
            int previewW = (int) (width * 0.42);
            int previewH = (int) (height * 0.42);
            int previewX = width - previewW - 16;
            int previewY = height - previewH - 70;

            // White rounded border around preview
            g2d.setColor(Color.WHITE);
            g2d.fillRoundRect(previewX - 4, previewY - 4, previewW + 8, previewH + 8, 16, 16);

            // Draw scaled AI image into inset
            BufferedImage scaledAI = scaleImage(aiImage, previewW, previewH);
            g2d.drawImage(scaledAI, previewX, previewY, null);

            // "AI Preview" label inside the dark footer strip
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2d.drawString("✦ AI Customization Preview", 16, height - 20);

            g2d.dispose();

            return toBase64Png(canvas);

        } catch (Exception e) {
            log.error("Image overlay failed: {}", e.getMessage());
            // Fallback: return the AI-generated image URL as-is (no overlay)
            return null;
        }
    }

    /**
     * Fallback: if no base image, just return DALL-E url directly as base64 isn't possible.
     * Caller should check if overlay returned null and use generatedImageUrl directly.
     */
    public boolean canOverlay(String baseImageUrl) {
        return baseImageUrl != null && !baseImageUrl.isBlank();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private BufferedImage loadImageFromUrl(String imageUrl) throws IOException {
        URL url = URI.create(imageUrl).toURL();
        BufferedImage image = ImageIO.read(url);
        if (image == null) throw new IOException("Could not read image from URL: " + imageUrl);
        return image;
    }

    private BufferedImage scaleImage(BufferedImage original, int targetW, int targetH) {
        BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(original, 0, 0, targetW, targetH, null);
        g.dispose();
        return scaled;
    }

    private String toBase64Png(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
