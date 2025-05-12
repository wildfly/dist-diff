package org.wildfly.qa.distdiff2.artifacts;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.wildfly.qa.distdiff2.helpers.AdaptorCDATA;
import org.wildfly.qa.distdiff2.results.Status;

/**
 * Artifact
 * <p>
 * Abstract definition of artifact used as value object of compared files
 */
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class Artifact {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "relative-path")
    private String relativePath;

    @XmlTransient
    private String parentRelativePath;

    @XmlAttribute(name = "status")
    private Status status;

    @XmlTransient
    private Artifact parent;

    @XmlJavaTypeAdapter(value = AdaptorCDATA.class)
    private String permissionDiff;

    @XmlAttribute(name = "pathA")
    private String pathA;

    @XmlAttribute(name = "pathB")
    private String pathB;

    @XmlAttribute(name = "level")
    private int level;

    // Is supposed to represent state when artifact has only permission differences.
    // This one is not propagated to the XML.
    private boolean permissionDiffOnly = false;

    /**
     * Default constructor
     */
    Artifact() {
    }

    Artifact(String name, String relativePath) {
        this.name = name;
        this.relativePath = relativePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Artifact getParent() {
        return parent;
    }

    public void setParent(Artifact parent) {
        this.parent = parent;
    }

    public String getPathA() {
        return pathA;
    }

    public void setPathA(String pathA) {
        this.pathA = pathA;
    }

    public String getPathB() {
        return pathB;
    }

    public void setPathB(String pathB) {
        this.pathB = pathB;
    }

    public String getParentRelativePath() {
        return parentRelativePath;
    }

    public void setParentRelativePath(String parentRelativePath) {
        this.parentRelativePath = parentRelativePath;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getPermissionDiff() {
        return permissionDiff;
    }

    public void setPermissionDiff(String permissionDiff) {
        this.permissionDiff = permissionDiff;
    }

    public boolean isPermissionDiffOnly() {
        return permissionDiffOnly;
    }

    public void setPermissionDiffOnly(boolean permissionDiffOnly) {
        this.permissionDiffOnly = permissionDiffOnly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Artifact artifact = (Artifact) o;

        return name.equals(artifact.name) && relativePath.equals(artifact.relativePath);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (relativePath != null ? relativePath.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "name='" + name + '\'' +
                ", status=" + status +
                ", pathA=" + pathA +
                ", pathB=" + pathB +
                ", relativePath='" + relativePath + "'";
    }
}
