package com.example.url_shortener.service;

import com.example.url_shortener.encoding.Base62Encoding;
import com.example.url_shortener.entity.Url;
import com.example.url_shortener.exception.ResourceNotFoundException;
import com.example.url_shortener.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class UrlService {
    private Base62Encoding base62Encoding;
    private UrlRepository urlRepository;
    private final RedisTemplate<String, String> redisTemplate;
    UrlService(Base62Encoding base62Encoding, UrlRepository urlRepository, RedisTemplate<String, String> redisTemplate){
        this.base62Encoding=base62Encoding;
        this.urlRepository=urlRepository;
        this.redisTemplate = redisTemplate;
    }
    @Value("${app.base-url}")
    private String baseUrl;
    public String shortenUrl(String originalUrl){
        Url url=new Url();
        url.setOriginalUrl(originalUrl);
        url.setCreatedAt(LocalDateTime.now());
        url.setClickCount(Long.valueOf(0));
        Url savedUrl=urlRepository.save(url);
        String shortcode=base62Encoding.encode(savedUrl.getId());
        savedUrl.setShortCode(shortcode);
        urlRepository.save(savedUrl);
        return baseUrl+"/"+shortcode;
    }
    @Cacheable(value = "urls", key="#shortcode")
    public String redirect(String shortcode){
        Url url = urlRepository.findByShortCode(shortcode)
                .orElseThrow(() -> new ResourceNotFoundException("ShortCode not found: " + shortcode));
        return url.getOriginalUrl();
    }
    public void incrementClickCount(String shortCode) {
        redisTemplate.opsForValue().increment("clickcount:" + shortCode);
    }

    @Scheduled(fixedRate = 300000) // runs every 5 minutes
    public void flushClickCounts() {
        Set<String> keys = redisTemplate.keys("clickcount:*");
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            String shortCode = key.replace("clickcount:", "");
            String count = redisTemplate.opsForValue().get(key);
            if (count == null) continue;

            urlRepository.findByShortCode(shortCode).ifPresent(url -> {
                url.setClickCount(url.getClickCount() + Long.parseLong(count));
                urlRepository.save(url);
            });

            redisTemplate.delete(key);
        }
    }
}
