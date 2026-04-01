package com.example.url_shortener.service.impl;

import com.example.url_shortener.dto.ShortUrlRequestDto;
import com.example.url_shortener.dto.ShortUrlResponseDto;
import com.example.url_shortener.entity.UrlMapping;
import com.example.url_shortener.exception.UrlNotFoundException;
import com.example.url_shortener.repository.UrlMappingRepository;
import com.example.url_shortener.service.helper.Encode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UrlServiceTest{

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @InjectMocks
    private UrlShorteningServiceImpl service;

    private static final String DOMAIN      = "http://localhost:8080";
    private static final String SHORT_CODE  = "abc123";
    private static final String LONG_URL    = "https://www.example.com/some/very/long/path?q=test";

    @BeforeEach
    void injectDomain() {
        // @Value fields are not injected by Mockito — set manually
        ReflectionTestUtils.setField(service, "domain", DOMAIN);
    }

    // ─────────────────────────────────────────
    // createShortUrl
    // ─────────────────────────────────────────

    @Nested
    @DisplayName("createShortUrl")
    class CreateShortUrl {

        @Test
        @DisplayName("returns full short URL using domain + encoded id")
        void returnsFullShortUrl() {
            // Arrange
            ShortUrlRequestDto request = requestWithNoExpiry();

            UrlMapping firstSave  = mappingWithId(1L);
            UrlMapping secondSave = mappingWithId(1L);
            secondSave.setShortUrl(SHORT_CODE);

            when(urlMappingRepository.save(any())).thenReturn(firstSave, secondSave);

            try (MockedStatic<Encode> encode = mockStatic(Encode.class)) {
                encode.when(() -> Encode.encode(1L)).thenReturn(SHORT_CODE);

                // Act
                ShortUrlResponseDto response = service.createShortUrl(request);

                // Assert
                assertThat(response.getShortUrl())
                        .isEqualTo(DOMAIN + "/" + SHORT_CODE);
            }
        }

        @Test
        @DisplayName("sets default expiry of 7 days when expiredAt is null")
        void setsDefaultExpiryWhenNotProvided() {
            ShortUrlRequestDto request = requestWithNoExpiry();

            UrlMapping saved = mappingWithId(1L);
            when(urlMappingRepository.save(any())).thenReturn(saved, saved);

            LocalDateTime before = LocalDateTime.now().plusDays(7).minusSeconds(2);
            LocalDateTime after  = LocalDateTime.now().plusDays(7).plusSeconds(2);

            try (MockedStatic<Encode> encode = mockStatic(Encode.class)) {
                encode.when(() -> Encode.encode(anyLong())).thenReturn(SHORT_CODE);
                service.createShortUrl(request);
            }

            // Capture what was passed to the first save()
            ArgumentCaptor<UrlMapping> captor = ArgumentCaptor.forClass(UrlMapping.class);
            verify(urlMappingRepository, times(2)).save(captor.capture());

            LocalDateTime actualExpiry = captor.getAllValues().get(0).getExpiredAt();
            assertThat(actualExpiry).isBetween(before, after);
        }

        @Test
        @DisplayName("uses provided expiredAt when set in request")
        void usesProvidedExpiry() {
            LocalDateTime customExpiry = LocalDateTime.now().plusDays(30);
            ShortUrlRequestDto request = requestWithExpiry(customExpiry);

            UrlMapping saved = mappingWithId(1L);
            when(urlMappingRepository.save(any())).thenReturn(saved, saved);

            try (MockedStatic<Encode> encode = mockStatic(Encode.class)) {
                encode.when(() -> Encode.encode(anyLong())).thenReturn(SHORT_CODE);
                service.createShortUrl(request);
            }

            ArgumentCaptor<UrlMapping> captor = ArgumentCaptor.forClass(UrlMapping.class);
            verify(urlMappingRepository, times(2)).save(captor.capture());

            assertThat(captor.getAllValues().get(0).getExpiredAt()).isEqualTo(customExpiry);
        }

        @Test
        @DisplayName("persists longUrl on the entity")
        void persistsLongUrl() {
            ShortUrlRequestDto request = requestWithNoExpiry();

            UrlMapping saved = mappingWithId(1L);
            when(urlMappingRepository.save(any())).thenReturn(saved, saved);

            try (MockedStatic<Encode> encode = mockStatic(Encode.class)) {
                encode.when(() -> Encode.encode(anyLong())).thenReturn(SHORT_CODE);
                service.createShortUrl(request);
            }

            ArgumentCaptor<UrlMapping> captor = ArgumentCaptor.forClass(UrlMapping.class);
            verify(urlMappingRepository, times(2)).save(captor.capture());

            assertThat(captor.getAllValues().get(0).getLongUrl()).isEqualTo(LONG_URL);
        }

        @Test
        @DisplayName("sets the short code on the entity before the second save")
        void setsShortCodeBeforeSecondSave() {
            ShortUrlRequestDto request = requestWithNoExpiry();

            UrlMapping saved = mappingWithId(42L);
            when(urlMappingRepository.save(any())).thenReturn(saved, saved);

            try (MockedStatic<Encode> encode = mockStatic(Encode.class)) {
                encode.when(() -> Encode.encode(42L)).thenReturn("xyz789");
                service.createShortUrl(request);
            }

            ArgumentCaptor<UrlMapping> captor = ArgumentCaptor.forClass(UrlMapping.class);
            verify(urlMappingRepository, times(2)).save(captor.capture());

            // Second save must carry the short code
            assertThat(captor.getAllValues().get(1).getShortUrl()).isEqualTo("xyz789");
        }

        @Test
        @DisplayName("calls save exactly twice — once for id, once for short code")
        void saveCalledTwice() {
            ShortUrlRequestDto request = requestWithNoExpiry();
            UrlMapping saved = mappingWithId(1L);
            when(urlMappingRepository.save(any())).thenReturn(saved, saved);

            try (MockedStatic<Encode> encode = mockStatic(Encode.class)) {
                encode.when(() -> Encode.encode(anyLong())).thenReturn(SHORT_CODE);
                service.createShortUrl(request);
            }

            verify(urlMappingRepository, times(2)).save(any(UrlMapping.class));
        }
    }

    // ─────────────────────────────────────────
    // redirectUrl
    // ─────────────────────────────────────────

    @Nested
    @DisplayName("redirectUrl")
    class RedirectUrl {

        @Test
        @DisplayName("returns longUrl when shortCode exists")
        void returnsLongUrl() {
            UrlMapping mapping = new UrlMapping();
            mapping.setLongUrl(LONG_URL);
            when(urlMappingRepository.findByShortUrl(SHORT_CODE))
                    .thenReturn(Optional.of(mapping));

            String result = service.redirectUrl(SHORT_CODE);

            assertThat(result).isEqualTo(LONG_URL);
        }

        @Test
        @DisplayName("throws UrlNotFoundException when shortCode does not exist")
        void throwsWhenNotFound() {
            when(urlMappingRepository.findByShortUrl("missing"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.redirectUrl("missing"))
                    .isInstanceOf(UrlNotFoundException.class)
                    .hasMessageContaining("missing");
        }

        @Test
        @DisplayName("queries repository with the exact shortCode passed in")
        void queriesWithCorrectCode() {
            UrlMapping mapping = new UrlMapping();
            mapping.setLongUrl(LONG_URL);
            when(urlMappingRepository.findByShortUrl(SHORT_CODE))
                    .thenReturn(Optional.of(mapping));

            service.redirectUrl(SHORT_CODE);

            verify(urlMappingRepository).findByShortUrl(SHORT_CODE);
        }
    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────

    private ShortUrlRequestDto requestWithNoExpiry() {
        ShortUrlRequestDto dto = new ShortUrlRequestDto();
        dto.setLongUrl(LONG_URL);
        dto.setExpiredAt(null);
        return dto;
    }

    private ShortUrlRequestDto requestWithExpiry(LocalDateTime expiredAt) {
        ShortUrlRequestDto dto = new ShortUrlRequestDto();
        dto.setLongUrl(LONG_URL);
        dto.setExpiredAt(expiredAt);
        return dto;
    }

    private UrlMapping mappingWithId(Long id) {
        UrlMapping m = new UrlMapping();
        m.setId(id);
        m.setLongUrl(LONG_URL);
        return m;
    }
}