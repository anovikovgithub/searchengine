package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import java.util.HashMap;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.indexingService = indexingService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<HashMap<String, String>> beginIndexing() {
        return ResponseEntity.ok(indexingService.beginIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<HashMap<String, String>> endIndexing() {
        return ResponseEntity.ok(indexingService.endIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<HashMap<String, String>> addIndexing(@RequestBody String url) {
        return ResponseEntity.ok(indexingService.addIndexing(url));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(name = "query", required = true) String query,
                                    @RequestParam(name = "site", required = false) String searchSite,
                                    @RequestParam(name = "offset", required = false) int offset,
                                    @RequestParam(name = "limit", required = false) int limit) {
        return ResponseEntity.ok(statisticsService.search(query, searchSite, offset, limit));
    }
}

