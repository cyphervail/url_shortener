package com.example.url_shortener.controller;


import com.example.url_shortener.dto.ShortUrlRequestDto;
import com.example.url_shortener.dto.ShortUrlResponseDto;
import com.example.url_shortener.service.UrlShorteningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ShortUrlController {

    private final UrlShorteningService urlShorteningService;

    @PostMapping("/shortUrl")
    public ResponseEntity<ShortUrlResponseDto> createShortUrl(@RequestBody @Valid ShortUrlRequestDto requestDto){
        log.info("Short url request for shortening longUrl :"+ requestDto.getLongUrl());
        ShortUrlResponseDto urlResponseDto=urlShorteningService.createShortUrl(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(urlResponseDto);
    }

    @GetMapping("/{shortCode:[a-zA-Z0-9]+}")
    public ResponseEntity<Void> redirectUrl(@PathVariable String shortCode){

        String redirectUrl= urlShorteningService.redirectUrl(shortCode);

        log.info("Redirecting shortUrl={} to longUrl={}", shortCode,redirectUrl);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

}
