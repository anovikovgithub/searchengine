package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface IndexRepository extends JpaRepository<Index, Long> {
    @Transactional
    @Modifying
    void deleteIndexByPageIn(Optional<List<Page>> pages);

    @Query(value = "SELECT page_id FROM search_engine.index i WHERE i.lemma_id in (:lemma_ids)", nativeQuery = true)
    Iterable<BigInteger> getPagesByLemmaIds(List<BigInteger> lemma_ids);

    @Query(value = "SELECT page_id FROM search_engine.index i WHERE i.lemma_id in (:lemma_ids) and i.page_id in (:page_ids)", nativeQuery = true)
    Iterable<BigInteger> getPagesByLemmaAndPageIds(List<BigInteger> lemma_ids, List<BigInteger> page_ids);

}
