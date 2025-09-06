package com.shiori.backend;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
public class Manga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(unique = true, length = 36)
    private String mangadexId;
    private Integer year;
    private String coverImageUrl;
    private Instant createdAt, updatedAt;
    @Enumerated(EnumType.STRING)
    private MangaStatus status;
    
    public enum MangaStatus {
        ONGOING,
        COMPLETED,
        HIATUS,
        CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }


    public Manga() {}
    
    public Manga(String title, String mangadexId, Integer year, String coverImageUrl, MangaStatus status) {
        this.title = title;
        this.mangadexId = mangadexId;
        this.year = year;
        this.coverImageUrl = coverImageUrl;
        this.status = status;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getMangadexId() {
        return mangadexId;
    }
    public void setMangadexId(String mangadexId) {
        this.mangadexId = mangadexId;
    }
    public Integer getYear() {
        return year;
    }
    public void setYear(Integer year) {
        this.year = year;
    }
    public String getCoverImageUrl() {
        return coverImageUrl;
    }
    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    public MangaStatus getStatus() {
        return status;
    }
    public void setStatus(MangaStatus status) {
        this.status = status;
    }

    
    
}
