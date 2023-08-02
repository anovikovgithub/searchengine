package searchengine.services;

import java.util.HashMap;

public interface IndexingService {
    HashMap<String, String> beginIndexing();
    HashMap<String, String> endIndexing();
    HashMap<String, String> addIndexing(String url);
}
