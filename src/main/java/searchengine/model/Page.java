package searchengine.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "page"//, indexes = {@Index( name = "path_idx", columnList = "path")}
)
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.MERGE, targetEntity = Site.class)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(name = "path", columnDefinition = "TEXT", nullable = false)
    private String path;

    @Column(name = "code", columnDefinition = "INT", nullable = false)
    private int code;

    @Column (name = "content", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;


}
