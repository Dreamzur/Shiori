package com.shiori.backend;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shiori.backend.dto.ChapterResult;
import com.shiori.backend.dto.MangaSearchResult;

@RestController
@RequestMapping("/api/md")
public class MangaDexController {
    private final MangaDexService mdService;

    public MangaDexController(MangaDexService mdService) {
        this.mdService = mdService;
    }

    @GetMapping("/search")
    public List<MangaSearchResult> search(@RequestParam String title, @RequestParam(defaultValue = "5") int limit) {
        return mdService.searchResults(title, limit);
    }

    @GetMapping("/manga/{id}/feed")
    public List<ChapterResult> feed(@PathVariable String id, @RequestParam(defaultValue = "10") int limit, @RequestParam(defaultValue = "en") String lang) {
        return mdService.getFeed(id, limit, lang);
    }

    @GetMapping("/manga/{id}/latest")
    public ChapterResult latest(
        @PathVariable String id,
        @RequestParam(defaultValue = "en") String lang
    ) {
        return mdService.getLatestNumberedChapter(id, lang);
    }
}
