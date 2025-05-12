package org.wildfly.qa.distdiff2.artifacts;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * ClassArtifact
 * <p>
 * Representation of class file in compared distributions
 */
@XmlRootElement(name = "class")
@XmlAccessorType(XmlAccessType.FIELD)
public class ClassArtifact extends FileArtifact {

    // TODO add specific information for class file


    public ClassArtifact() {
    }

    public ClassArtifact(String name, String relativePath, long size) {
        super(name, relativePath, size);
    }



}
