package com.shiori.backend;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiori.backend.dto.ChapterResult;
import com.shiori.backend.dto.MangaSearchResult;

@Service
public class MangaDexService {
    private final MangaDexClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    private String pickTitle(JsonNode attributes) {
        // main title is always prefered
        JsonNode title = attributes.path("title");
        String en = title.path("en").asText(null);
        if (en != null && !en.isBlank()) return en;

        String ja = title.path("ja").asText(null);
        if (ja != null && !ja.isBlank()) return ja;

        // romanized japanese
        String jaRo = title.path("ja-ro").asText(null);
        if (jaRo != null && !jaRo.isBlank()) return jaRo;

        // if no main title, use alt
        JsonNode alts = attributes.path("altTitles");
        if (alts.isArray()) {
            for (JsonNode alt : alts) {
                String v = alt.path("en").asText(null);
                if (v != null && !v.isBlank()) return v;
            }
            for (JsonNode alt : alts) {
                String v = alt.path("ja").asText(null);
                if (v != null && !v.isBlank()) return v;
            }
            for (JsonNode alt : alts) {
                for (var f : alt.properties()) {
                    if (f.getValue().isTextual() && !f.getValue().asText().isBlank()) {
                        return f.getValue().asText();
                    }
                }
            }
            
        }
        // nothing
        return null;
    }

    // translates Json mess and turns it into obj with the specified fields
    private ChapterResult toChapterResult(JsonNode chapter) {
        String chapterId = chapter.path("id").asText();
        JsonNode attribute = chapter.path("attributes");
        String chapterStr = attribute.path("chapter").asText(null);
        String title = attribute.path("title").asText(null);
        String volume = attribute.path("volume").asText(null);

        String readableAt = attribute.path("readableAt").asText(null);
        if (readableAt == null || readableAt.isBlank()) {
            readableAt = attribute.path("createdAt").asText(null);
        }

        String groupName = null;
        for (JsonNode relationship : chapter.path("relationships")) {
            if ("scanlation_group".equals(relationship.path("type").asText())) {
                JsonNode gAttributes = relationship.path("attributes");
                if (gAttributes.hasNonNull("name")) {
                    groupName = gAttributes.get("name").asText();
                }
                break;
            }
        }
        return new ChapterResult(chapterId, chapterStr, title, volume, readableAt, groupName);
    }

    public MangaDexService(MangaDexClient client) {
        this.client = client;
    }

    // translates raw mangadex json into clean java object
    public List<MangaSearchResult> searchResults(String title, int limit) {
        try {
            String json = client.search(title, limit, "cover_art", "safe");
            JsonNode root = mapper.readTree(json);
            JsonNode data = root.path("data");
            List<MangaSearchResult> output = new ArrayList<>();
            if (data.isArray()) {
                for (JsonNode item : data) {
                    String id = item.path("id").asText();
                    JsonNode attributes = item.path("attributes");
                    String titlePicked = pickTitle(attributes);
                    Integer year = attributes.hasNonNull("year") ? attributes.get("year").asInt() : null;


                    String coverFile = null;
                    for (JsonNode rel : item.path("relationships")) {
                        if ("cover_art".equals(rel.path("type").asText())) {
                            JsonNode coverAtt = rel.path("attributes");
                            if (coverAtt.hasNonNull("fileName")) {
                                coverFile = coverAtt.get("fileName").asText();
                            }
                            break;
                        }
                    }

                    String coverUrl = (coverFile != null) ? "https://uploads.mangadex.org/covers/" + id + "/" + coverFile : null;

                    output.add(new MangaSearchResult(id, titlePicked, year, coverUrl));
                }
            }
            return output;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse", e);
        }
    }

    public List<ChapterResult> getFeed(String mangaId, int limit, String lang) {
        try {
            String json = client.feed(
                mangaId,
                lang,               // translatedLanguage[]
                limit,      
                "desc",             // order[readableAt]
                "scanlation_group", // includes[]
                "safe"              // contentRating[]
            );

            JsonNode root = mapper.readTree(json);
            JsonNode data = root.path("data");
            List<ChapterResult> output = new ArrayList<>();

            if (data.isArray()) {
                for (JsonNode ch : data) {
                    String chapterId = ch.path("id").asText();
                    JsonNode attribute = ch.path("attributes");
                    String chapter = ch.path("chapter").isMissingNode() ? null : attribute.path("chapter").asText(null);
                    String title = ch.path("title").isMissingNode() ? null : attribute.path("title").asText(null);
                    String volume = ch.path("volume").isMissingNode() ? null : attribute.path("volume").asText(null);

                    // use readableAt if available else createdAt
                    String readableAt = attribute.path("readableAt").asText(null);
                    if (readableAt == null || readableAt.isBlank()) {
                        readableAt = attribute.path("createdAt").asText(null);
                    }

                    // pulling scanlation if its available
                    String groupName = null;
                    for (JsonNode relationship : ch.path("relationships")) {
                        if ("scanlation_group".equals(relationship.path("type").asText())) {
                            JsonNode gAttributes = relationship.path("attributes");
                            if (gAttributes.hasNonNull("name")) {
                                groupName = gAttributes.get("name").asText();
                            }
                            break;
                        }
                    }
                    output.add(new ChapterResult(chapterId, chapter, title, volume, readableAt, groupName));
                }
            }
            return output;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse feed", e);
        }
    }

    public ChapterResult getLatestNumberedChapter(String mangaId, String lang) {
        try {
            // get manga volumes highest chapter
            String aggJson = client.aggregate(mangaId, lang);
            JsonNode aggRoot = mapper.readTree(aggJson);
            JsonNode volumes = aggRoot.path("volumes");

            String latestChapterString = null;
            BigDecimal latestChapterNum = null;

            var volumeIterator = volumes.properties().iterator();
            while (volumeIterator.hasNext()) {
                var volumeEntry = volumeIterator.next();
                JsonNode chapters = volumeEntry.getValue().path("chapters");
                for (var chEntry : chapters.properties()) {
                    String key = chEntry.getKey();
                    try {
                        BigDecimal num = new BigDecimal(key);
                        if (latestChapterNum == null || num.compareTo(latestChapterNum) > 0) {
                            latestChapterNum = num;
                            latestChapterString = key;
                        }
                    } catch (NumberFormatException ignore) {
                        // no need to parse values like "Extra", "Special", etc..
                    }
                }
            }
            
            // If a numeric chapter key is found, then fetch it
            if (latestChapterString != null) {
                String chapterJson = client.chapterByNumber(
                    mangaId,
                    latestChapterString,
                    lang,
                    "desc",
                    1,
                    "safe",
                    "scanlation_group"
                    );
                    JsonNode chapterRoot = mapper.readTree(chapterJson);
                    JsonNode data = chapterRoot.path("data");
                    if (data.isArray() && data.size() > 0) {
                        return toChapterResult(data.get(0));
                    }
            }

            // last resort, use newest first feed and pick a non-blank chapter
            String feedJson = client.feed(
                mangaId,
                lang,
                50,
                "desc",
                "scanlation_group",
                "safe"
                );
                JsonNode feedRoot = mapper.readTree(feedJson);
                JsonNode feedData = feedRoot.path("data");
                if (feedData.isArray()) {
                    for (JsonNode chapter : feedData) {
                        String chapterString = chapter.path("attributes").path("chapter").asText(null);
                        if (chapterString != null && !chapterString.isBlank()) {
                            return toChapterResult(chapter);
                        }
                    }

                    // If theres no chapter number then just return newest item
                    if (feedData.size() > 0) {
                        return toChapterResult(feedData.get(0));
                    }
                }

                // absolutely nothing is found
                return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch latest chapter", e);
        }
    }    
}
