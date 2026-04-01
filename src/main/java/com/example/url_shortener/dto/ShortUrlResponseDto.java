package com.example.url_shortener.dto;

import com.example.url_shortener.entity.UrlMapping;
import lombok.Data;

import java.time.Instant;

@Data
public class ShortUrlResponseDto {
    private String shortUrl;

    public ShortUrlResponseDto(String shortUrl){
        this.shortUrl=shortUrl;
    }
}
