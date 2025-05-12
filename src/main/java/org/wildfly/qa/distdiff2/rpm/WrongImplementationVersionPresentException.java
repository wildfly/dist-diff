package org.wildfly.qa.distdiff2.rpm;

/**
 * @author Jan Martiska
 */
public class WrongImplementationVersionPresentException extends Exception {

    private final String version;

    public WrongImplementationVersionPresentException(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
