package com.example.url_shortener.service.impl;

import com.example.url_shortener.dto.ShortUrlRequestDto;
import com.example.url_shortener.dto.ShortUrlResponseDto;
import com.example.url_shortener.entity.UrlMapping;
import com.example.url_shortener.exception.UrlNotFoundException;
import com.example.url_shortener.repository.UrlMappingRepository;
import com.example.url_shortener.service.UrlShorteningService;
import com.example.url_shortener.service.helper.Encode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class UrlShorteningServiceImpl implements UrlShorteningService {

    private final UrlMappingRepository  urlMappingRepository;

    @Value("${app.domain}")
    private String domain;


    @Override
    @Transactional
    public ShortUrlResponseDto createShortUrl(ShortUrlRequestDto requestDto) {
        UrlMapping urlMapping=new UrlMapping();
        urlMapping.setLongUrl(requestDto.getLongUrl());
        if(requestDto.getExpiredAt()!=null){
            urlMapping.setExpiredAt(requestDto.getExpiredAt());
        }else{
            urlMapping.setExpiredAt(LocalDateTime.now().plus(7, ChronoUnit.DAYS));
        }

        UrlMapping savedUrl=urlMappingRepository.save(urlMapping);

        String shortCode= Encode.encode(savedUrl.getId());
        savedUrl.setShortUrl(shortCode);

        UrlMapping updatedUrl=urlMappingRepository.save(savedUrl);

        String shortUrl=domain+"/"+updatedUrl.getShortUrl();


        log.info("Created shortUrl={} for longUrl={}", shortUrl, requestDto.getLongUrl());

        return new ShortUrlResponseDto(shortUrl);
    }

    @Override
    @Cacheable(value = "shortUrl", unless = "#result==null")
    public String redirectUrl(String shortUrl) {
        UrlMapping urlMapping=urlMappingRepository.findByShortUrl(shortUrl)
                .orElseThrow(()-> new UrlNotFoundException("Url doesnt exist with URI : "+shortUrl));

        return urlMapping.getLongUrl();
    }
}
