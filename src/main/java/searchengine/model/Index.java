package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "\"index\"")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false, cascade = CascadeType.MERGE, targetEntity = Page.class)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @ManyToOne(optional = false, cascade = CascadeType.MERGE, targetEntity = Lemma.class)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;
    @Column(name = "\"rank\"", columnDefinition = "FLOAT", nullable = false)
    private float rank;
}
