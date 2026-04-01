package com.example.url_shortener.service.impl;

import com.example.url_shortener.dto.ShortUrlRequestDto;
import com.example.url_shortener.dto.ShortUrlResponseDto;
import com.example.url_shortener.entity.UrlMapping;
import com.example.url_shortener.exception.UrlNotFoundException;
import com.example.url_shortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShorteningServiceImplTest {

    @Mock
    private UrlMappingRepository repository;

    @InjectMocks
    private UrlShorteningServiceImpl shorteningService;

    private UrlMapping urlMapping;

    @BeforeEach
    void setup(){
        urlMapping=new UrlMapping(1L,"abc","www.google.com", LocalDateTime.now(),LocalDateTime.now().plus(7,ChronoUnit.DAYS));
    }

    @Test
    void redirectUrl_should_return_longUrl() {
        when(repository.findByShortUrl("abc")).thenReturn(Optional.of(urlMapping));

        String result=shorteningService.redirectUrl("abc");

        assertEquals(urlMapping.getLongUrl(),result);
    }
    @Test
    void redirectUrl_should_throw_UrlNotFoundException(){
        when(repository.findByShortUrl("bcd")).thenReturn(Optional.empty());

        Exception ex=assertThrows(UrlNotFoundException.class,()->shorteningService.redirectUrl("bcd"));

        assertEquals("Url doesnt exist with URI : "+"bcd",ex.getMessage());
    }

    //Create short url

    @Test
    void createShortUrl_should_return_shortUrl(){
        UrlMapping mapping1=new UrlMapping(1L,null,"www.google.com", LocalDateTime.now(),LocalDateTime.now().plus(7,ChronoUnit.DAYS));;

        UrlMapping mapping2=new UrlMapping(1L,"abc","www.google.com", LocalDateTime.now(),LocalDateTime.now().plus(7,ChronoUnit.DAYS));

        ShortUrlRequestDto requestDto=new ShortUrlRequestDto();
        requestDto.setLongUrl("www.google.com");
        requestDto.setExpiredAt(LocalDateTime.now().plus(7,ChronoUnit.DAYS));


        when(repository.save(any(UrlMapping.class))).thenReturn(mapping1)
                .thenReturn(mapping2);

        ShortUrlResponseDto result=shorteningService.createShortUrl(requestDto);

        verify(repository, times(2)).save(any(UrlMapping.class));

        assertEquals(new ShortUrlResponseDto("null/abc"),result);
    }

}