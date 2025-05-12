package org.wildfly.qa.distdiff2.results;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.ClassArtifact;
import org.wildfly.qa.distdiff2.artifacts.FileArtifact;
import org.wildfly.qa.distdiff2.artifacts.FolderArtifact;
import org.wildfly.qa.distdiff2.artifacts.JarArtifact;
import org.wildfly.qa.distdiff2.tools.DistDiff2Version;

/**
 * Results
 * <p>
 * Representation of results
 */
@XmlRootElement(name = "dist-diff2")
@XmlAccessorType(XmlAccessType.FIELD)
public final class Results {

    // Distribution folder A
    private String folderA;

    // Distribution folder B
    private String folderB;

    private String serverDistribution;

    private String version = DistDiff2Version.VERSION;

    @XmlElementWrapper(name = "addedModules")
    @XmlElement(name = "addedModule")
    private List<String> addedModules;

    @XmlElementWrapper(name = "removedModules")
    @XmlElement(name = "removedModule")
    private List<String> removedModules;

    @XmlElementWrapper(name = "artifacts")
    @XmlElements({
            @XmlElement(name = "file", type = FileArtifact.class),
            @XmlElement(name = "folder", type = FolderArtifact.class),
            @XmlElement(name = "class", type = ClassArtifact.class),
            @XmlElement(name = "archive", type = ClassArtifact.class),
            @XmlElement(name = "jar", type = JarArtifact.class)
    })
    private List<Artifact> artifacts;

    @XmlElementWrapper(name = "errorMessages")
    @XmlElement(name = "errorMessage")
    private List<String> errorMessages;

    private String commandLineArguments;
    private String systemHostnameArchitecture = "&lt;unset&gt;";
    private String artifactsNumbers = "<a>unset</a><r>unset</r><d>unset</d><s>unset</s><o>unset</o><t>unset</t>";

    /**
     * Default constructor
     */
    public Results() {
        this.errorMessages = new ArrayList<>();
    }

    public String getFolderA() {
        return folderA;
    }

    public void setFolderA(String folderA) {
        this.folderA = folderA;
    }

    public String getFolderB() {
        return folderB;
    }

    public void setFolderB(String folderB) {
        this.folderB = folderB;
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    public String getServerDistribution() {
        return serverDistribution;
    }

    public void setServerDistributionName(String serverDistribution) {
        this.serverDistribution = serverDistribution;
    }

    public List<String> getAddedModules() {
        return addedModules;
    }

    public void setAddedModules(List<String> addedModules) {
        this.addedModules = addedModules;
    }

    public List<String> getRemovedModules() {
        return removedModules;
    }

    public void setRemovedModules(List<String> removedModules) {
        this.removedModules = removedModules;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public void addErrorMessage(String message) {
        this.errorMessages.add(message);
    }

    public String getCommandLineArguments() {
        return commandLineArguments;
    }

    public void setCommandLineArguments(String commandLineArguments) {
        this.commandLineArguments = commandLineArguments;
    }

    public String getSystemHostnameArchitecture() {
        return systemHostnameArchitecture;
    }

    public void setSystemHostnameArchitecture(String systemHostnameArchitecture) {
        this.systemHostnameArchitecture = systemHostnameArchitecture;
    }

    public void setArtifactsNumbers(int added, int removed, int different, int same, int others, int total) {
        this.artifactsNumbers = "<a>" + added + "</a><r>" + removed + "</r><d>" + different + "</d><s>" + same
                + "</s><o>" + others + "</o><t>" + total + "</t>";
    }

    public String getArtifactsNumbers() {
        return artifactsNumbers;
    }

    public String getVersion() {
        return version;
    }

    /**
     * Search by the filename.
     * If there are multiple files of the same name, it returns only the first one and
     * ignores the others!
     */
    public Artifact findArtifactBySimpleName(String name) {
        for (Artifact artifact : artifacts) {
            if (artifact.getName().equals(name)) return artifact;
        }
        return null;
    }

    /**
     * Search by a relative path
     */
    public Artifact findArtifactByRelativePath(String path) {
        for (Artifact artifact : artifacts) {
            if (artifact.getRelativePath().equals(path)) return artifact;
        }
        return null;
    }

    /**
     * If the result list contains at least one artifact in a state which is considered unexpected,
     * this will return true.
     * <p>
     * This is mainly used for determining whether dist-diff2 process should finish with a non-zero exit value.
     */
    public boolean isThereAnyErrorInResults() {
        return isThereAnyErrorInResults(false);
    }

    /**
     * If the result list contains at least one artifact in a state which is considered unexpected,
     * this will return true.
     * <p>
     * This is mainly used for determining whether dist-diff2 process should finish with a non-zero exit value.
     * @param ignoreLineBreakDifference set to true if differences between files consisting only of line breaks should
     *                                  be ignored (not being evaluated as an error state)
     */
    public boolean isThereAnyErrorInResults(boolean ignoreLineBreakDifference) {
        return filterArtifactsInError(ignoreLineBreakDifference).findAny().isPresent();
    }

    /**
     * Filter all artifacts which are in an unexpected state
     * @param ignoreLineBreakDifference set to true if differences between files consisting only of line breaks should
     *                                  be ignored (not being evaluated as an error state)
     * @return stream of all artifacts in an unexpected state
     */
    public Stream<Artifact> filterArtifactsInError(boolean ignoreLineBreakDifference) {
        final Set<Status> errorStates = EnumSet.of(
                Status.ERROR,
                Status.PATCHED_WRONG,
                Status.ADDED,
                Status.REMOVED,
                Status.DIFFERENT,
                Status.NOT_PATCHED);

        if (!ignoreLineBreakDifference) {
            errorStates.add(Status.DIFFERENT_LINE_BREAKS);
        }

        return this.getArtifacts().stream()
                .filter(artifact -> errorStates.contains(artifact.getStatus()));
    }

    /**
     * Filter all artifacts which are in an unexpected state
     * @return stream of all artifacts in an unexpected state
     */
    public Stream<Artifact> filterArtifactsInError() {
        return filterArtifactsInError(false);
    }

    /**
     * Get all artifacts in an unexpected state
     * @return a list of artifacts in an unexpected state
     */
    public List<Artifact> getArtifactsInError() {
        return getArtifactsInError(false);
    }

    /**
     * Get all artifacts in an unexpected state
     * @param ignoreLineBreakDifference set to true if differences between files consisting only of line breaks should
     *                                  be ignored (not being evaluated as an error state)
     * @return a list of artifacts in an unexpected state
     */
    public List<Artifact> getArtifactsInError(boolean ignoreLineBreakDifference) {
        return filterArtifactsInError(ignoreLineBreakDifference).collect(Collectors.toList());
    }


    /**
     * Get list of unexpected artifacts in a simple String representation.
     * @return a list of strings in '[ARTIFACT_STATUS]: [ARTIFACT_RELATIVE_PATH]' format
     */
    public List<String> getArtifactsStringInError() {
        return getArtifactsStringInError(false);
    }

    /**
     * Get list of unexpected artifacts in a simple String representation.
     * @param ignoreLineBreakDifference set to true if differences between files consisting only of line breaks should
     *                                  be ignored (not being evaluated as an error state)
     * @return a list of strings in '[ARTIFACT_STATUS]: [ARTIFACT_RELATIVE_PATH]' format
     */
    public List<String> getArtifactsStringInError(boolean ignoreLineBreakDifference) {
        return filterArtifactsInError(ignoreLineBreakDifference)
                .map(artifact -> artifact.getStatus() + ": " + artifact.getRelativePath())
                .collect(Collectors.toList());
    }

}
