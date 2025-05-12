package org.wildfly.qa.distdiff2.artifacts;

import org.wildfly.qa.distdiff2.helpers.AdaptorCDATA;
import org.wildfly.qa.distdiff2.rpm.RPMDetails;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * FileArtifact
 * <p>
 * Representation of file in compared distributions
 */
@XmlRootElement(name = "file")
@XmlAccessorType(XmlAccessType.FIELD)
public class FileArtifact extends Artifact {

    @XmlAttribute(name = "mime-type")
    private String mimeType;

    @XmlAttribute(name = "md5sum")
    private String md5sum;

    @XmlAttribute(name = "md5sumA")
    private String md5sumA;

    @XmlAttribute(name = "md5sumB")
    private String md5sumB;

    @XmlAttribute(name = "size")
    private long size;

    @XmlJavaTypeAdapter(value = AdaptorCDATA.class)
    private String textDiff;

    @XmlElement(name = "rpm-details")
    private RPMDetails rpmDetails;

    public FileArtifact() {
    }

    public FileArtifact(String name, String relativePath, long size) {
        super(name, relativePath);
        this.size = size;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMd5sumA() {
        return md5sumA;
    }

    public void setMd5sumA(String md5sumA) {
        this.md5sumA = md5sumA;
    }

    public String getMd5sumB() {
        return md5sumB;
    }

    public void setMd5sumB(String md5sumB) {
        this.md5sumB = md5sumB;
    }

    public String getTextDiff() {
        return textDiff;
    }

    public void setTextDiff(String textDiff) {
        this.textDiff = textDiff;
    }

    public RPMDetails getRpmDetails() {
        return rpmDetails;
    }

    public void setRpmDetails(RPMDetails rpmDetails) {
        this.rpmDetails = rpmDetails;
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
        return this.getClass().getSimpleName() + "{" + super.toString() + ", " +
                "mimeType='" + mimeType + '\'' +
                ", md5sum='" + md5sum + '\'' +
                ", size=" + size +
                '}';
    }
}
