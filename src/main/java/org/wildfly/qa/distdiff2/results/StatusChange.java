package org.wildfly.qa.distdiff2.results;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * StatusChange - Represents a single status change event in an artifact's lifecycle.
 *
 * <p>Each StatusChange captures what happened, when, by which phase, and why. This provides
 * a complete audit trail showing how an artifact's status evolved through the comparison pipeline.
 *
 * <h3>Usage Example</h3>
 * <pre>
 * StatusChange change = new StatusChange(
 *     Status.SAME,
 *     Status.DIFFERENT,
 *     "MD5SumsPhase",
 *     System.currentTimeMillis(),
 *     "MD5 checksums differ (A=abc123, B=def456)"
 * );
 * </pre>
 *
 * <h3>XML Serialization</h3>
 * <p>StatusChange instances are serialized into dist-diff2 XML reports:
 * <pre>{@code
 * <status-change from="SAME" to="DIFFERENT" phase="MD5SumsPhase"
 *                timestamp="1234567890" reason="MD5 checksums differ..."/>
 * }</pre>
 *
 * @see Status
 * @see org.wildfly.qa.distdiff2.artifacts.Artifact
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class StatusChange {

    /**
     * The status before this change.
     */
    @XmlAttribute(name = "from")
    private Status fromStatus;

    /**
     * The status after this change.
     */
    @XmlAttribute(name = "to")
    private Status toStatus;

    /**
     * The name of the phase that made this change (e.g., "MD5SumsPhase", "JarVersionComparePhase").
     */
    @XmlAttribute(name = "phase")
    private String phaseName;

    /**
     * Timestamp when this change occurred (milliseconds since epoch).
     */
    @XmlAttribute(name = "timestamp")
    private long timestamp;

    /**
     * Human-readable explanation of why the status changed.
     * Should include relevant details like MD5 values, version numbers, etc.
     */
    @XmlAttribute(name = "reason")
    private String reason;

    /**
     * Default constructor for JAXB.
     */
    public StatusChange() {
    }

    /**
     * Creates a new status change record.
     *
     * @param fromStatus The previous status
     * @param toStatus The new status
     * @param phaseName The name of the phase making the change
     * @param timestamp When the change occurred (milliseconds since epoch)
     * @param reason Human-readable explanation of the change
     */
    public StatusChange(Status fromStatus, Status toStatus, String phaseName, long timestamp, String reason) {
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.phaseName = phaseName;
        this.timestamp = timestamp;
        this.reason = reason;
    }

    /**
     * Gets the status before this change.
     *
     * @return The previous status
     */
    public Status getFromStatus() {
        return fromStatus;
    }

    /**
     * Gets the status after this change.
     *
     * @return The new status
     */
    public Status getToStatus() {
        return toStatus;
    }

    /**
     * Gets the name of the phase that made this change.
     *
     * @return The phase name (e.g., "MD5SumsPhase")
     */
    public String getPhaseName() {
        return phaseName;
    }

    /**
     * Gets the timestamp when this change occurred.
     *
     * @return Milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the human-readable explanation of why the status changed.
     *
     * @return The reason string
     */
    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return phaseName + ": " + fromStatus + " → " + toStatus + " (" + reason + ")";
    }
}
