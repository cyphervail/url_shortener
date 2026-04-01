package com.example.url_shortener.service;

import com.example.url_shortener.dto.ShortUrlRequestDto;
import com.example.url_shortener.dto.ShortUrlResponseDto;

public interface UrlShorteningService {

    ShortUrlResponseDto createShortUrl(ShortUrlRequestDto requestDto);

    String  redirectUrl(String shortUrl);
}
