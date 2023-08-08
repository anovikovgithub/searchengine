package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.exceptions.NothingFoundException;
import searchengine.model.*;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    @Autowired
    SiteRepository siteRepository;
    @Autowired
    PageRepository pageRepository;
    @Autowired
    LemmaRepository lemmaRepository;
    @Autowired
    IndexRepository indexRepository;

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(siteRepository.count());
        total.setPages(pageRepository.count());
        total.setLemmas(lemmaRepository.count());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        Iterable<searchengine.model.Site> siteIterable = siteRepository.findAll();
        Iterator<searchengine.model.Site> siteIterator = siteIterable.iterator();
        while (siteIterator.hasNext()) {
            searchengine.model.Site site = siteIterator.next();
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int pages = pageRepository.countBySite(site);
            item.setPages(pages);
            int lemmas = lemmaRepository.countBySite(site);
            item.setLemmas(lemmas);
            item.setStatus(String.valueOf(site.getStatus()));
            item.setError(site.getLastError());
            LocalDateTime ldt = site.getStatusTime();
            ZonedDateTime zdt = ldt.atZone(ZoneId.of("Europe/Moscow"));
            item.setStatusTime(zdt.toInstant().toEpochMilli());
            detailed.add(item);
        }
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

//    @PersistenceContext
//    private EntityManager entityManager;

    @Override
    public HashMap<String, Object> search(String query, String searchSite, Integer offset, Integer limit) {
        HashMap<String, Object> response = new HashMap<>();
        try {
            LemmaFinder lemmaFinder = LemmaFinder.getInstance();
            Site site = null;
            Long siteId = null;
            if (!((searchSite == null) || (searchSite.equals("")))) {
                Iterable<Site> siteIterable = siteRepository.findByUrl(searchSite);
                Iterator<Site> siteIterator = siteIterable.iterator();
                if (siteIterator.hasNext()) {
                    site = siteIterator.next();
                    siteId = site.getId();
                } else {
                    throw new RuntimeException("указанный сайт не проиндексирован");
                }
            }
            final Long finalSiteId = siteId;
            //найти уникальные леммы в поисковом запросе
            Map<String, Integer> lemmasFromDB = lemmaFinder.collectLemmas(query);

            //исключить леммы, которые встречаются более чем на 100 страницах (для примера)
            List<String> lemmasToExclude = new ArrayList<>();
            lemmasFromDB.forEach((lemma, count) -> {
                Optional<Integer> pagesCnt = lemmaRepository.getPageCountByLemma(lemma, finalSiteId);
                if ((pagesCnt.isPresent()) && (pagesCnt.get() > 100)) {
                    lemmasToExclude.add(lemma);
                }
            });
            lemmasToExclude.forEach(lemma -> lemmasFromDB.remove(lemma));
            if (lemmasFromDB.isEmpty()) {
                throw new NothingFoundException("Не найдено совпадений по поисковому запросу");
            }
            //количество встречающихся лемм в проиндексированных сайтах
            lemmasFromDB.forEach((lemma, count) -> {
                Integer lemmaCnt = lemmaRepository.getLemmaCounts(lemma, finalSiteId);
                if (lemmaCnt == null) lemmaCnt = 0;
                lemmasFromDB.put(lemma, lemmaCnt);
            });
            //отсортируем в порядке возрастания frequency
            Map<String, Integer> lemmasSortedByVal =
                    lemmasFromDB.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue())
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            Iterable<BigInteger> lemmaIds;
            Iterable<BigInteger> pagesIds = null;
            List<BigInteger> pagesIdsList = new ArrayList<>();
            List<BigInteger> lemmaIdsList = new ArrayList<>();
            try {
                for (Map.Entry<String, Integer> entry : lemmasSortedByVal.entrySet()) {
                    if (entry.getValue() == 0) {
                        throw new NothingFoundException("Не найдено совпадений по поисковому запросу");
                    } else {
                        lemmaIds = (finalSiteId == null)
                                ? lemmaRepository.getLemmaIdsByName(entry.getKey())
                                : lemmaRepository.getLemmaIdsByNameAndSite(entry.getKey(), finalSiteId);
                        lemmaIds.forEach(lemmaIdsList::add);
                        if (pagesIds == null) {
                            pagesIds = indexRepository.getPagesByLemmaIds(lemmaIdsList);
                        } else {
                            pagesIds.forEach(pagesIdsList::add);
                            pagesIds = indexRepository.getPagesByLemmaAndPageIds(lemmaIdsList, pagesIdsList);
                        }
                        if (!pagesIds.iterator().hasNext()) {
                            throw new NothingFoundException("Не найдено совпадений по поисковому запросу");
                        }
                    }
                }
            } catch (Exception e) {
                response.put("result", false);
                response.put("error", e.getMessage());
                return response;
            }
            pagesIdsList.clear();
            pagesIds.forEach(pagesIdsList::add);
            Iterable<Page> pageIterable = pageRepository.getPagesByIds(pagesIdsList);
            Iterable<Object[]> pagesRank = pageRepository.getPagesRank(pagesIdsList, lemmaIdsList);
            HashMap<BigInteger, Double> pagesRankMap = new HashMap<>();
            Iterator<Object[]> pagesRankIteraror = pagesRank.iterator();
            if (pagesRankIteraror.hasNext()) {
                while (pagesRankIteraror.hasNext()) {
                    Object[] pageRank = pagesRankIteraror.next();
                    pagesRankMap.put((BigInteger) pageRank[0], (Double) pageRank[1]);
                }
            }
            Optional<Object> maxRankOptional = pageRepository.getMaxRank(pagesIdsList, lemmaIdsList);
            Double maxRelevance = (Double) maxRankOptional.get();
            int cnt = 0;
            Iterator<Page> pageIterator = pageIterable.iterator();
            int offset_ = offset;
            if (offset_ > 0) {
                offset++;
                if (pageIterator.hasNext()) {
                    while ((offset_ >= 0) && (pageIterator.hasNext())) {
                        offset_--;
                        cnt++;
                        pageIterator.next();
                    }
                }
            }
            if (!pageIterator.hasNext()) {
                response.put("result", false);
                response.put("error", "Задан пустой поисковый запрос");
            } else {
                ArrayList<HashMap<String, ?>> details = new ArrayList<>();
                response.put("result", true);
                while (pageIterator.hasNext()) {
                    cnt++;
                    Page page = pageIterator.next();
                    if ((limit > 0) && (cnt > limit + offset )) {
                        continue;
                    }
                    Site s = page.getSite();
                    HashMap<String, Object> item = new HashMap<>();
                    item.put("site", s.getUrl());
                    item.put("siteName", s.getName());
                    URI uri = URI.create(page.getPath());
                    item.put("uri", uri.getPath());
                    Document doc = Jsoup.parse(page.getContent());
                    //item.put("title", doc.title());
                    item.put("title", page.getPath());
                    String fragment = getDocumentFragment(doc, query);
                    item.put("snippet", fragment);
                    Double relevance = 0.0D;
                    BigInteger pageId = BigInteger.valueOf(page.getId());
                    Double absoluteRelevance = pagesRankMap.get(pageId);
                    if (absoluteRelevance != null) {
                        relevance = absoluteRelevance / maxRelevance;
                    }
                    item.put("relevance", relevance);
                    details.add(item);
                    response.put("data", details);
                }
            }
            response.put("count", cnt);
            return response;
        } catch (Exception e) {
            response.put("result", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    private String insertBoldTagByText(String result, String text, int maxLength) {
        int startIndex = result.toLowerCase().indexOf(text.toLowerCase());
        if (startIndex >= 0) {
            result = result.substring(0, startIndex) + "<b>" + result.substring(startIndex);
            startIndex = startIndex + 3;
            int stopIndex = startIndex + text.length();
            if (stopIndex > maxLength) {
                stopIndex = maxLength;

            }
            result = result.substring(0, startIndex + text.length()) + "</b>" + result.substring(stopIndex);
            if (!result.startsWith("... ")) {
                result = "... " + result;
            }
            if (!result.endsWith("... ")) {
                result += "... ";
            }
        }
        return result;
    }

    private String getFragmentPart(Document doc, String text, int maxLength) {
        Pattern pattern = Pattern.compile(text.toLowerCase(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Elements e = doc.getElementsMatchingOwnText(pattern);
        Iterator<Element> eIterator = e.iterator();
        if (eIterator.hasNext()) {
            String result = String.valueOf(eIterator.next().text());
            if (result.length() > maxLength) {

                int startIndex = result.indexOf(text);
                if (startIndex >= 0) {
                    if ((maxLength - text.length()) > 0) {
                        startIndex = (maxLength - text.length()) / 2;
                    } else {
                        startIndex = 0;
                    }
                    result = result.substring(startIndex, maxLength);
                }
            }
            result = insertBoldTagByText(result, text, maxLength);
            return result;
        } else {
            return "";
        }
    }

    public String getDocumentFragment(Document doc, String text) {
        int maxLength = 512;
        //поиск по вхождению запроса целиком
        String result = getFragmentPart(doc, text, maxLength);
        if (result.equals("")) {
            // поиск по вхождению каждого слова
            List<String> words = Arrays.stream(text.split(" ")).collect(Collectors.toList());
            maxLength = maxLength / words.size();
            for (String word : words) {
                if (result.toLowerCase().contains(word.toLowerCase())) {
                    result = insertBoldTagByText(result, word, maxLength);
                } else {
                    result += getFragmentPart(doc, word, maxLength);
                }
            }
        }
        return result;
    }
}
