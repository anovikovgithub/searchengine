package searchengine.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface PageRepository extends CrudRepository<Page, Long> {
    Optional<Page> findByPath(String path);
    @Transactional
    @Modifying
    void deleteBySite(Site site);
     Optional<List<Page>> findBySite(Site site);
     int countBySite(Site site);

    @Query(value = "SELECT * FROM page p WHERE p.id IN (:ids)", nativeQuery = true)
     Iterable<Page> getPagesByIds(List<BigInteger> ids);

    @Query(value = "SELECT i.page_id, SUM(i.rank) absolute_rank FROM search_engine.index i" +
            " WHERE i.page_id IN (:page_ids) AND i.lemma_id IN (:lemma_ids) GROUP BY i.page_id", nativeQuery = true)
    Iterable<Object[]> getPagesRank(List<BigInteger> page_ids, List<BigInteger> lemma_ids);

    @Query (value = "SELECT MAX(absolute_rank) max_rank \n" +
            "  FROM (SELECT i.page_id, SUM(i.rank) absolute_rank \n" +
            "      FROM search_engine.index i\n" +
            "        WHERE i.page_id IN (:page_ids) AND i.lemma_id IN (:lemma_ids) GROUP BY i.page_id) t", nativeQuery = true)
    Optional<Object> getMaxRank(List<BigInteger> page_ids, List<BigInteger> lemma_ids);
}
