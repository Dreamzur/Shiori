package com.shiori.backend.dto;

public record MangaSearchResult(
    String id,
    String title,
    Integer year,
    String coverUrl) {}
