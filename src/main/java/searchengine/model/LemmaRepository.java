package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);
    @Transactional
    @Modifying
    void deleteBySite(Site site);
    Integer countBySite(Site site);
    @Query(value = "SELECT  sum(l.frequency) FROM lemma l  WHERE l.lemma=:lemma and (site_id=:site_id or :site_id is null)", nativeQuery = true)
    Integer getLemmaCounts(String lemma, Long site_id);

    @Query(value = "select count(page_id) from search_engine.index i where i.lemma_id in\n" +
            " (select l.id from lemma l WHERE l.lemma =:lemma and (l.site_id=:site_id or :site_id is null))", nativeQuery = true)
    Optional<Integer> getPageCountByLemma(String lemma, Long site_id);


    @Query(value = "SELECT id FROM lemma l WHERE l.lemma=:lemma", nativeQuery = true)
    Iterable<BigInteger> getLemmaIdsByName(String lemma);

    @Query(value = "SELECT id FROM lemma l WHERE l.lemma=:lemma and site_id=:site_id", nativeQuery = true)
    Iterable<BigInteger> getLemmaIdsByNameAndSite(String lemma, Long site_id);

}
