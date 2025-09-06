package com.shiori.backend;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MangaRepository extends JpaRepository<Manga, Long>{
    Optional<Manga> findByMangadexId(String mangadexId);
}
