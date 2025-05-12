package org.wildfly.qa.distdiff2.artifacts;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.jar.Manifest;

import org.wildfly.qa.distdiff2.jardiff.JarDiff;

/**
 * JarArtifact
 * <p>
 * Representation of jar archive in compared distributions
 */
@XmlRootElement(name = "jar")
@XmlAccessorType(XmlAccessType.FIELD)
public class JarArtifact extends ArchiveArtifact {

    /**
     * Holds detailed information about jar version and build
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class BuildInformation {
        @XmlAttribute(name = "name")
        private String name;
        @XmlAttribute(name = "major")
        private String majorVersion;
        @XmlAttribute(name = "minor")
        private String minorVersion;
        @XmlAttribute(name = "micro")
        private String microVersion;
        @XmlAttribute(name = "suffix")
        private String suffix;
        @XmlAttribute(name = "build")
        private String build;
        @XmlAttribute(name = "full-name")
        private String fullName;

        public BuildInformation() {
        }

        public BuildInformation(String name) {
            this.name = name;
        }

        public String getMajorVersion() {
            return majorVersion;
        }

        public void setMajorVersion(String majorVersion) {
            this.majorVersion = majorVersion;
        }

        public String getMinorVersion() {
            return minorVersion;
        }

        public void setMinorVersion(String minorVersion) {
            this.minorVersion = minorVersion;
        }

        public String getMicroVersion() {
            return microVersion;
        }

        public void setMicroVersion(String microVersion) {
            this.microVersion = microVersion;
        }

        public String getSuffix() {
            return suffix;
        }

        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }

        public String getBuild() {
            return build;
        }

        public void setBuild(String build) {
            this.build = build;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
    }

    // Manifest information
    private Manifest manifestA;
    private Manifest manifestB;

    @XmlElement(name = "build-information-a")
    private BuildInformation buildInformationA;
    @XmlElement(name = "build-information-b")
    private BuildInformation buildInformationB;

    @XmlElement(name = "jarDiff")
    private JarDiff jarDiff;

    public JarArtifact() {
    }

    public JarArtifact(String name, String relativePath, long size) {
        super(name, relativePath, size);
    }

    public Manifest getManifestA() {
        return manifestA;
    }

    public void setManifestA(Manifest manifestA) {
        this.manifestA = manifestA;
    }

    public Manifest getManifestB() {
        return manifestB;
    }

    public void setManifestB(Manifest manifestB) {
        this.manifestB = manifestB;
    }

    public BuildInformation getBuildInformationA() {
        return buildInformationA;
    }

    public void setBuildInformationA(BuildInformation buildInformationA) {
        this.buildInformationA = buildInformationA;
    }

    public BuildInformation getBuildInformationB() {
        return buildInformationB;
    }

    public void setBuildInformationB(BuildInformation buildInformationB) {
        this.buildInformationB = buildInformationB;
    }

    public JarDiff getJarDiff() {
        return jarDiff;
    }

    public void setJarDiff(JarDiff jarDiff) {
        this.jarDiff = jarDiff;
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
