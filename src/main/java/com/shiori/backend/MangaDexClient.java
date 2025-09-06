package com.shiori.backend;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

// Feignclient class
@FeignClient(name = "mangadex", url = "https://api.mangadex.org")
public interface MangaDexClient {

    @GetMapping("/manga")
    String search(@RequestParam("title") String title,
                  @RequestParam("limit") int limit,
                  @RequestParam(value = "includes[]", defaultValue = "cover_art") String includes,
                  @RequestParam(value = "contentRating[]", defaultValue = "safe") String contentRating);

    @GetMapping("/manga/{id}/feed")
    String feed(
        @PathVariable("id") String id,
        @RequestParam("translatedLanguage[]") String translatedLanguage,
        @RequestParam("limit") int limit,
        @RequestParam("order[readableAt]") String orderReadableAt,
        @RequestParam("includes[]") String includes,
        @RequestParam("contentRating[]") String contentRating);

    @GetMapping("/manga/{id}/aggregate")
    String aggregate(
        @PathVariable("id") String id,
        @RequestParam("translatedLanguage[]") String translatedLanguage);

    @GetMapping("/chapter")
    String chapterByNumber(
        @RequestParam("manga") String mangaId,
        @RequestParam("chapter") String chapter,
        @RequestParam("translatedLanguage[]") String translatedLanguage,
        @RequestParam("order[readableAt]") String order,
        @RequestParam("limit") int limit,
        @RequestParam("contentRating[]") String contentRating,
        @RequestParam("includes[]") String includes);
    
} 
