package org.wildfly.qa.distdiff2.results;

/**
 * Status
 * <p>
 * Status for artifact in distribution
 */
public enum Status {

    PATCHED_WRONG(120),
    ERROR(100),
    NOT_PATCHED(60),
    ADDED(50),
    REMOVED(45),
    DIFFERENT(40),
    DIFFERENT_LINE_BREAKS(35),
    PATCHED_UNNECESSARILY(30),
    VERSION(20),
    BUILD(18),
    EXPECTED_DIFFERENCES(15),
    PATCHED(10),
    SAME(1);

    // higher number = appears earlier in the report
    private final int priority;

    Status(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
