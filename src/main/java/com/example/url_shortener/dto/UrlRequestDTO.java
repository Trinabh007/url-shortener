package com.example.url_shortener.dto;

import jakarta.validation.constraints.NotBlank;

public class UrlRequestDTO {
    @org.hibernate.validator.constraints.URL
    @NotBlank
    String originalUrl;

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }
}
