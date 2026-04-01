package com.example.url_shortener.repository;

import com.example.url_shortener.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface UrlMappingRepository extends JpaRepository<UrlMapping,Long> {
    Optional<UrlMapping> findByShortUrl(String shortUrl);

    List<UrlMapping> findByExpiredAtBefore(LocalDateTime expiredAt);
}
