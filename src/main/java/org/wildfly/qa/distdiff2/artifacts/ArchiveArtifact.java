package org.wildfly.qa.distdiff2.artifacts;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;

/**
 * ArchiveArtifact
 * <p>
 * Representation of archive in compared distributions
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ArchiveArtifact extends FileArtifact implements ItemWithChild {

    @XmlElementWrapper(name = "items")
    @XmlElements({
            @XmlElement(name = "file", type = FileArtifact.class),
            @XmlElement(name = "folder", type = FolderArtifact.class),
            @XmlElement(name = "class", type = ClassArtifact.class),
            @XmlElement(name = "archive", type = ClassArtifact.class),
            @XmlElement(name = "jar", type = JarArtifact.class)
    })
    private List<Artifact> items;

    public ArchiveArtifact() {
    }

    public ArchiveArtifact(String name, String relativePath, long size) {
        super(name, relativePath, size);
    }

    public List<Artifact> getItems() {
        return items;
    }

    public void setItems(List<Artifact> items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
