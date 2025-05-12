package org.wildfly.qa.distdiff2.jardiff;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.helpers.AdaptorCDATA;


/**
 * Represents the difference between two JARs.
 *
 * @author Jan Martiska
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JarDiff {

    @XmlTransient
    private static final Logger LOGGER = Logger.getLogger(JarDiff.class.getName());

    @XmlElementWrapper(name = "addedClasses")
    @XmlElement(name = "addedClass")
    private final Set<String> addedClasses;

    @XmlElementWrapper(name = "removedClasses")
    @XmlElement(name = "removedClass")
    private final Set<String> removedClasses;

    @XmlElementWrapper(name = "classDiffs")
    @XmlElement(name = "classDiff")
    private final Map<String, ClassDiff> classDiffs;

    @XmlElementWrapper(name = "addedFiles")
    @XmlElement(name = "addedFile")
    private final Set<String> addedFiles;

    @XmlElementWrapper(name = "removedFiles")
    @XmlElement(name = "removedFile")
    private final Set<String> removedFiles;

    @XmlElement(name = "manifestDiff")
    @XmlJavaTypeAdapter(AdaptorCDATA.class)
    private String manifestDiff;

    @XmlElement(name = "manifestHasOnlyExpectedChanges")
    private boolean manifestHasOnlyExpectedChanges = true;

    public JarDiff() {
        addedClasses = new HashSet<>();
        removedClasses = new HashSet<>();
        classDiffs = new HashMap<>();
        addedFiles = new HashSet<>();
        removedFiles = new HashSet<>();
    }

    public void removedClass(String className) {
        removedClasses.add(className);
    }

    public void addedClass(String className) {
        addedClasses.add(className);
    }

    public void classDiff(String className, ClassDiff diff) {
        classDiffs.put(className, diff);
    }

    public boolean isEmpty() {
        return addedClasses.isEmpty() &&
                removedClasses.isEmpty() &&
                classDiffs.isEmpty() &&
                addedFiles.isEmpty() &&
                removedFiles.isEmpty() &&
                (manifestDiff == null || manifestDiff.isEmpty());
    }

    public boolean isEmptyExceptChangesInManifest() {
        LOGGER.trace("added classes: " + addedClasses.size());
        LOGGER.trace("removed classes: " + removedClasses.size());
        LOGGER.trace("class diffs: " + classDiffs.size());
        LOGGER.trace("added files: " + addedFiles.size());
        LOGGER.trace("removed files: " + removedFiles.size());
        return addedClasses.isEmpty() &&
                removedClasses.isEmpty() &&
                classDiffs.isEmpty() &&
                addedFiles.isEmpty() &&
                removedFiles.isEmpty();
    }

    public void addedFile(String path) {
        addedFiles.add(path);
    }

    public void removedFile(String path) {
        removedFiles.add(path);
    }

    // conversion to HTML is now taken care of by the XSLT template
    @Deprecated
    public String toHTMLString() {
        StringBuilder builder = new StringBuilder();

        if (!addedFiles.isEmpty()) {
            builder.append("<b>Added files: </b><br /><ul>");
            for (String addedFile : addedFiles) {
                builder.append("<li>").append(addedFile).append("</li>");
            }
            builder.append("</ul>");
        }

        if (!removedFiles.isEmpty()) {
            builder.append("<b>Removed files: </b><br /><ul>");
            for (String removedFile : removedFiles) {
                builder.append("<li>").append(removedFile).append("</li>");
            }
            builder.append("</ul>");
        }

        if (!addedClasses.isEmpty()) {
            builder.append("<b>Added classes: </b><br /><ul>");
            for (String addedClass : addedClasses) {
                builder.append("<li>").append(addedClass).append("</li>");
            }
            builder.append("</ul>");
        }

        if (!removedClasses.isEmpty()) {
            builder.append("<b>Removed classes: </b><br /><ul>");
            for (String removedClass : removedClasses) {
                builder.append("<li>").append(removedClass).append("</li>");
            }
            builder.append("</ul>");
        }

        if (manifestDiff != null) {
            builder.append("<br/><b>MANIFEST.MF diff:").append("</b><br />").append(manifestDiff);
        }

        if (!classDiffs.isEmpty()) {
            for (Map.Entry<String, ClassDiff> diffEntry : classDiffs.entrySet()) {
                if (!diffEntry.getValue().isEmpty()) {
                    builder.append("<b>Class ").append(diffEntry.getKey()).append(": </b><br />");
                    builder.append(diffEntry.getValue().toString());
                }
            }
        }

        return builder.toString();
    }

    public String toSimpleString() {
        StringBuilder builder = new StringBuilder();
        if (!addedClasses.isEmpty()) {
            for (String addedClass : addedClasses) {
                builder.append("+ ").append(addedClass).append("\n");
            }
        }

        if (!removedClasses.isEmpty()) {
            for (String removedClass : removedClasses) {
                builder.append("- ").append(removedClass).append("\n");
            }
        }
        return builder.toString();
    }


    public void setManifestDiff(String manifestDiff) {
        this.manifestDiff = manifestDiff;
    }

    public boolean manifestHasOnlyExpectedChangesIfAny() {
        return manifestHasOnlyExpectedChanges;
    }

    public void setManifestHasOnlyExpectedChanges(boolean manifestHasOnlyExpectedChanges) {
        this.manifestHasOnlyExpectedChanges = manifestHasOnlyExpectedChanges;
    }

    public Set<String> getAddedClasses() {
        return addedClasses;
    }

    public Set<String> getRemovedClasses() {
        return removedClasses;
    }

    public Map<String, ClassDiff> getClassDiffs() {
        return classDiffs;
    }

    public Set<String> getAddedFiles() {
        return addedFiles;
    }

    public Set<String> getRemovedFiles() {
        return removedFiles;
    }
}
