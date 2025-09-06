package com.shiori.backend;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

@Service
public class MangaService {
    
    private final MangaRepository mangaRepo;

    public MangaService(MangaRepository mangaRepo) {
        this.mangaRepo = mangaRepo;
    }

    public Manga create(String title, String mangadexId, Integer year, String coverUrl, Manga.MangaStatus status) {
        Manga manga = new Manga(title, mangadexId, year, coverUrl, status);
        return mangaRepo.save(manga);
    }

    public Manga getById(Long id) {
        return mangaRepo.findById(id).orElseThrow(() -> new RuntimeException("Manga with id " + id + " not found"));
    }

    public List<Manga> getAll() {
        return mangaRepo.findAll();
    }

    public void deleteById(Long id) {
        if (!mangaRepo.existsById(id)) {
            throw new EntityNotFoundException("Manga with id " + id + " not found");
        }
        mangaRepo.deleteById(id);
    }

    public Optional<Manga> findByMangadexId(String mangadexId) {
        return mangaRepo.findByMangadexId(mangadexId);
    }

    public Manga updateManga(Long id, Manga mangaUpdate) {
        Manga manga = mangaRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Manga with id " + id + " not found."));

            manga.setTitle(mangaUpdate.getTitle());
            manga.setMangadexId(mangaUpdate.getMangadexId());
            manga.setYear(mangaUpdate.getYear());
            manga.setCoverImageUrl(mangaUpdate.getCoverImageUrl());
            manga.setStatus(mangaUpdate.getStatus());

            return mangaRepo.save(manga);
    }
}
