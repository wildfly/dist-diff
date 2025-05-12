package org.wildfly.qa.distdiff2.artifacts;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.LinkedList;
import java.util.List;

/**
 * FolderArtifact
 * <p>
 * Representation of folder in compared distributions
 */
@XmlRootElement(name = "folder")
@XmlAccessorType(XmlAccessType.FIELD)
public class FolderArtifact extends Artifact implements ItemWithChild {

    @XmlElementWrapper(name = "items")
    @XmlElements({
            @XmlElement(name = "file", type = FileArtifact.class),
            @XmlElement(name = "folder", type = FolderArtifact.class),
            @XmlElement(name = "class", type = ClassArtifact.class),
            @XmlElement(name = "archive", type = ClassArtifact.class),
            @XmlElement(name = "jar", type = JarArtifact.class)
    })
    private List<Artifact> items;

    public FolderArtifact() {
    }

    public FolderArtifact(String name, String relativePath) {
        super(name, relativePath);
    }

    public FolderArtifact(String name) {
        super(name, null);
    }

    public List<Artifact> getItems() {
        if (this.items == null) {
            this.items = new LinkedList<>();
        }
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

    @Override
    public String toString() {
        return "FolderArtifact{" + super.toString() + ", level=" + getLevel() + "}";
    }
}
