package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private ConcurrentHashMap<Site, ForkJoinPool> forkJoinPoolMap = new ConcurrentHashMap<>();
    public static volatile Boolean indexingCanceled = false;

    @Autowired
    SiteRepository siteRepository;
    @Autowired
    PageRepository pageRepository;
    @Autowired
    LemmaRepository lemmaRepository;
    @Autowired
    IndexRepository indexRepository;

    @Override
    public HashMap<String, String> beginIndexing() {

        HashMap<String, String> result = new HashMap<>();
        if (forkJoinPoolMap.size() > 0) {
            result.put("result", "false");
            result.put("error", "Индексация уже запущена");
        } else {
            startIndexing("");
            result.put("result", "true");
        }
        return result;
    }

    @Override
    public HashMap<String, String> endIndexing() {
        HashMap<String, String> result = new HashMap<>();
        if (forkJoinPoolMap.size() > 0) {
            stopIndexing();
            result.put("result", "true");
        } else {
            result.put("result", "false");
            result.put("error", "Индексация не запущена");
        }
        return result;
    }

    @Override
    public HashMap<String, String> addIndexing(String url) {
        HashMap<String, String> result = new HashMap<>();
        String[] keyVals = url.split("=");
        url = keyVals[1];
        try {
            url = new URI(url).getPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        List<searchengine.config.Site> sitesList = sites.getSites();
        String finalUrl = url.trim();
        if (sitesList.stream().filter(s -> s.getUrl().equalsIgnoreCase(finalUrl)).count() == 0) {
            result.put("result", "false");
            result.put("error", "Данная страница находится за пределами сайтов, \n" +
                    "указанных в конфигурационном файле");
        } else {
            if (forkJoinPoolMap.size() > 0) {
                result.put("result", "true");
            } else {
                startIndexing(url);
                result.put("result", "true");
            }
        }
        return result;
    }

    private void stopIndexing() {
        indexingCanceled = true;
        Enumeration<Site> siteEnumeration = forkJoinPoolMap.keys();
        while (siteEnumeration.hasMoreElements()) {
            Site site = siteEnumeration.nextElement();
            ForkJoinPool pool = forkJoinPoolMap.get(site);
            if (pool.isShutdown()) {
                forkJoinPoolMap.remove(site);
            } else {
                pool.shutdownNow();
                while (pool.isTerminating()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (site.getStatus() != SiteStatus.INDEXED) {
                    site.setStatus(SiteStatus.FAILED);
                }
                site.setStatusTime(LocalDateTime.now(ZoneId.of("Europe/Moscow")));
                site.setLastError("Операция прервана пользователем");
                siteRepository.save(site);
            }
        }
    }

    private Site createNewSite(searchengine.config.Site site) {
        Site mSite = new Site();
        mSite.setName(site.getName());
        mSite.setStatus(SiteStatus.INDEXING);
        mSite.setStatusTime(LocalDateTime.now(ZoneId.of("Europe/Moscow")));
        mSite.setUrl(site.getUrl());
        Site savedSite = siteRepository.save(mSite);
        return savedSite;
    }

    private void startIndexing(String url) {
        indexingCanceled = false;
        if (!url.equals("")) {
            List<Site> sites = Collections.list(forkJoinPoolMap.keys());
            for (Site site : sites) {
                //если добавляем страницу, и она уже индексируется, то ничего не делаем
                if (site.getUrl().equals(url)) {
                    return;
                }
            }
        }
        for (searchengine.config.Site site : sites.getSites()) {
            if (url.equals("") || (site.getUrl().equals(url))) {
                new Thread(() -> {
                    deleteSiteEntries(site.getUrl());
                    Site savedSite = createNewSite(site);
                    ReentrantLock jsoupLock = new ReentrantLock();
                    ReentrantLock readWriteLinksLock = new ReentrantLock();
                            ForkJoinPool pool = new ForkJoinPool();
                    forkJoinPoolMap.put(savedSite, pool);
                    pool.invoke(
                            new SiteParser(
                                    savedSite,
                                    site.getUrl(),
                                    siteRepository,
                                    pageRepository,
                                    lemmaRepository,
                                    indexRepository,
                                    jsoupLock,
                                    readWriteLinksLock
                            ));
                    pool.shutdown();
                    forkJoinPoolMap.remove(savedSite);
                    savedSite.setStatus(SiteStatus.INDEXED);
                    savedSite.setStatusTime(LocalDateTime.now(ZoneId.of("Europe/Moscow")));
                    siteRepository.save(savedSite);
                }).start();
            }
        }
    }

    private void deleteSiteEntries(String url) {
        Iterable<Site> sites = siteRepository.findByUrl(url);
        Iterator<Site> iterator = sites.iterator();
        while (iterator.hasNext()) {
            Site site = iterator.next();
            Optional<List<Page>> pages = pageRepository.findBySite(site);
            if (pages.isPresent()) {
                indexRepository.deleteIndexByPageIn(pages);
            }
            lemmaRepository.deleteBySite(site);
            pageRepository.deleteBySite(site);
            siteRepository.delete(site);
        }
    }
}
