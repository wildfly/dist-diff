package org.wildfly.qa.distdiff2;

/**
 * Thrown when supplied configuration to the dist diff contains invalid values. For example when supplied distribution
 * directories don't exist.
 * @author Jan Martiska
 */
public class InvalidDistDiffConfigurationException extends RuntimeException {

    public InvalidDistDiffConfigurationException(Throwable cause) {
        super(cause);
    }

    public InvalidDistDiffConfigurationException(String message) {
        super(message);
    }
}
