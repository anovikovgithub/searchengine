package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.model.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.locks.ReentrantLock;

import static searchengine.services.IndexingServiceImpl.indexingCanceled;


@Service
public class SiteParser extends RecursiveAction {
    private String rootUrl;
    private Site site;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private ReentrantLock jsoupLock;
    private ReentrantLock readWriteLinksLock;
    private  static LemmaFinder lemmaFinder;

    public SiteParser(
            Site site,
            String rootUrl,
            SiteRepository siteRepository,
            PageRepository pageRepository,
            LemmaRepository lemmaRepository,
            IndexRepository indexRepository,
            ReentrantLock jsoupLock,
            ReentrantLock readWriteLinksLock) {
        this.rootUrl = rootUrl;
        this.site = site;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.jsoupLock = jsoupLock;
        this.readWriteLinksLock = readWriteLinksLock;
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SiteParser() {
    }

    private boolean isLinkExists(String link) {
        Optional<Page> list = pageRepository.findByPath(link);
        return list.isEmpty()? false : true;
    }

    private boolean checkLink(String link) {
        readWriteLinksLock.lock();
        try {
            URL url = new URL(link);
            URL siteUrl = new URL(site.getUrl());
            String path = url.getPath();
            return (path != null) &&
                    !indexingCanceled &&
//                    !path.contains(".") &&
                    !link.contains("#") &&
                    url.getHost().equalsIgnoreCase(siteUrl.getHost()) &&
                    link.contains(site.getUrl()) &&
                    !isLinkExists(link);
        } catch (MalformedURLException e) {
            return false;
        } finally {
            readWriteLinksLock.unlock();
        }
    }

    @Override
    protected void compute() {
        try {
            Connection.Response response = null;
            Document doc = null;
            int statusCode;
            if (!indexingCanceled) {
                jsoupLock.lock();
                try {
                    Thread.sleep(130);
                    response = Jsoup.connect(rootUrl)
                            .timeout(0) //infinite
                            .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                            .ignoreHttpErrors(true)
                            .ignoreContentType(true)
                            .referrer("https://www.google.ru")
                            .followRedirects(true)
                            .execute();
                    statusCode = response.statusCode();
                    if (statusCode < 400) {
                        //обработать возможный редирект
                        String siteHost = new URL(site.getUrl()).getHost();
                        String JsoupHost = response.url().getHost();
                        if (!siteHost.equalsIgnoreCase(JsoupHost)) {
                            site.setUrl(response.url().getProtocol() + "://" + JsoupHost);
                            siteRepository.save(site);
                        }
                    }
                    doc = response.parse();
                } finally {
                    jsoupLock.unlock();
                }
                Elements links = doc.select("a[href]");
                HashSet<SiteParser> tasks = new HashSet<>();
                for (Element link : links) {
                    String absLink = link.attr("abs:href");
                    if (absLink.endsWith("/")) {
                        absLink = absLink.replaceFirst(".$", "");
                    }
                    if (checkLink(absLink)) {
                        if (!indexingCanceled) {
                            readWriteLinksLock.lock();
                            try {
                                String pageText = doc.html();
                                Page page = new Page();
                                page.setSite(site);
                                page.setPath(absLink);
                                page.setCode(statusCode);
                                page.setContent(pageText);
                                pageRepository.save(page);
                                if ((!indexingCanceled) && (statusCode < 400)) {
                                    Map<String, Integer> map;
                                    synchronized (lemmaFinder) {
                                        map = lemmaFinder.collectLemmas(doc.text());
                                    }
                                    map.forEach((name,freq) -> {
                                        Lemma lemma;
                                        Optional<Lemma> optionalLemma = lemmaRepository.findByLemmaAndSite(name, site);
                                        if (optionalLemma.isPresent()) {
                                            lemma = optionalLemma.get();
                                            lemma.setFrequency(lemma.getFrequency() + freq);
                                        } else {
                                            lemma = new Lemma();
                                            lemma.setLemma(name);
                                            lemma.setSite(site);
                                            lemma.setFrequency(freq);
                                        }
                                        lemmaRepository.save(lemma);
                                        Index index = new Index();
                                        index.setLemma(lemma);
                                        index.setPage(page);
                                        index.setRank(freq);
                                        indexRepository.save(index);
                                    });
                                }
                                site.setStatusTime(LocalDateTime.now(ZoneId.of("Europe/Moscow")));
                                siteRepository.save(site);
                            } finally {
                                readWriteLinksLock.unlock();
                            }
                        }
                        SiteParser task = new SiteParser(
                                site,
                                absLink,
                                siteRepository,
                                pageRepository,
                                lemmaRepository,
                                indexRepository,
                                jsoupLock,
                                readWriteLinksLock);
                        task.fork();
                        tasks.add(task);
                    }
                }
                for (SiteParser task : tasks) {
                    task.join();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
