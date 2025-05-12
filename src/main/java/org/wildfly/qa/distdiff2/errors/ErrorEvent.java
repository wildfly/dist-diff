package org.wildfly.qa.distdiff2.errors;

import org.wildfly.qa.distdiff2.artifacts.Artifact;

/**
 * @author Jan Martiska
 */
public class ErrorEvent {

    private String message;
    private Artifact artifact;

    public ErrorEvent(String message, Artifact artifact) {
        this.message = message;
        this.artifact = artifact;
    }

    public ErrorEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

}
