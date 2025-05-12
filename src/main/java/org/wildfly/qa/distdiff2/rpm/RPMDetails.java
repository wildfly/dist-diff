package org.wildfly.qa.distdiff2.rpm;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Jan Martiska
 */
@XmlRootElement(name = "rpm-details")
@XmlAccessorType(XmlAccessType.FIELD)
public class RPMDetails {

    @XmlAttribute
    private String packageName;

    @XmlAttribute
    private String packageShortName;

    @XmlElementWrapper(name = "available-package-versions")
    @XmlElement(name = "version")
    private List<String> availablePackageVersions;


    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageShortName() {
        return packageShortName;
    }

    public void setPackageShortName(String packageShortName) {
        this.packageShortName = packageShortName;
    }

    public List<String> getAvailablePackageVersions() {
        return availablePackageVersions;
    }

    public void setAvailablePackageVersions(List<String> availablePackageVersions) {
        this.availablePackageVersions = availablePackageVersions;
    }
}

