package com.example.url_shortener.controller;

import com.example.url_shortener.dto.UrlRequestDTO;
import com.example.url_shortener.dto.UrlResponseDTO;
import com.example.url_shortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api")
public class UrlController {
    private UrlService urlService;
    UrlController(UrlService urlService){
        this.urlService=urlService;
    }
    @PostMapping("/shorten")
    public ResponseEntity<UrlResponseDTO> generateShorCode(@Valid @RequestBody UrlRequestDTO url) {
        String fullUrl=urlService.shortenUrl(url.getOriginalUrl());
        String shortcode=fullUrl.substring(fullUrl.lastIndexOf("/")+1);
        UrlResponseDTO dt=new UrlResponseDTO();
        dt.setOriginalUrl(url.getOriginalUrl());
        dt.setShortCode(shortcode);
        dt.setShortUrl(fullUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(dt);
    }
    @GetMapping("/{shortcode}")
    public ResponseEntity<Void> getOriginalUrl(@PathVariable String shortcode) {
        String originalUrl = urlService.redirect(shortcode);
        urlService.incrementClickCount(shortcode);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
