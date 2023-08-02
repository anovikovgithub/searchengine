package searchengine.model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;

@Repository
public interface SiteRepository extends CrudRepository<Site, Long> {

    Iterable<Site> findByUrl(String url);
    @Query(value = "SELECT * FROM site WHERE id IN (SELECT DISTINCT site_id FROM page p WHERE p.id IN (:pages))", nativeQuery = true)
    Iterable<Site> getSitesfromPages(List<BigInteger> pages);


}
