package com.example.url_shortener.service.impl;


import com.example.url_shortener.entity.UrlMapping;
import com.example.url_shortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlCleanupScheduler {

    private final UrlMappingRepository mappingRepository;
    private final CacheManager cacheManager;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredUrls(){

        List<UrlMapping> expiredUrls=mappingRepository.findByExpiredAtBefore(LocalDateTime.now());

        if(expiredUrls.isEmpty()){
            log.debug("No expired Url found");
            return ;
        }

        log.info("Cleaning up {} expired urls ",expiredUrls.size());

        Cache cache=cacheManager.getCache("shortUrl");

        if(cache!=null){
            for(UrlMapping url:expiredUrls){
                cache.evict(url.getShortUrl());
            }
        }

        mappingRepository.deleteAll(expiredUrls);

    }
}
