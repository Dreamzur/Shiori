package com.shiori.backend.dto;

public record ChapterResult(
    String chapterId,
    String chapter,
    String title,
    String volume,
    String readableAt,
    String groupName
) {}

