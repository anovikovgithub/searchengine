package searchengine.services;

import searchengine.dto.statistics.StatisticsResponse;

import java.util.HashMap;

public interface StatisticsService {
    StatisticsResponse getStatistics();
    HashMap<String, Object> search(String query, String searchSite, Integer offser, Integer limit);
}
