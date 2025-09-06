package com.shiori.backend;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/manga")
public class MangaController {

    private final MangaService mService;

    MangaController(MangaService mService) {
        this.mService = mService;
    }

    @GetMapping
    public ResponseEntity<List<Manga>> getAll() {
        return ResponseEntity.ok(mService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Manga> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mService.getById(id));
    }

    @GetMapping(params = "mangadexId")
    public ResponseEntity<Manga> getByMangadexId(@RequestParam String mangadexId) {
        return mService.findByMangadexId(mangadexId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Manga> create(@RequestBody Manga request) {
        Manga saved = mService.create(
                request.getTitle(),
                request.getMangadexId(),
                request.getYear(), 
                request.getCoverImageUrl(),
                request.getStatus()
                );
        URI location = URI.create("/api/manga/" + saved.getId());
        return ResponseEntity.created(location).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        mService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Manga> updateManga(@PathVariable Long id, @RequestBody Manga manga) {
        Manga updated = mService.updateManga(id, manga);
        return ResponseEntity.ok(updated);
    }
    
}
