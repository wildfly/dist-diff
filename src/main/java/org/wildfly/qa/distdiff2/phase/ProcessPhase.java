package org.wildfly.qa.distdiff2.phase;

import org.wildfly.qa.distdiff2.configuration.DistDiffConfiguration;
import org.wildfly.qa.distdiff2.configuration.DistDiff2Context;
import org.wildfly.qa.distdiff2.results.Results;

/**
 * ProcessPhase interface
 * <p>
 * Definition of API for phase of the distributions comparison process
 *
 * A ProcessPhase implementation MUST have a public default constructor!!!
 */
public abstract class ProcessPhase {

    protected DistDiff2Context context;
    protected DistDiffConfiguration distDiffConfiguration;
    protected Results results;

    /**
     * Represents one phase of process - e.g. check if files were removed between builds
     */
    public abstract void process();

    public void setContext(DistDiff2Context context) {
        this.context = context;
        this.distDiffConfiguration = context.getConfiguration();
        this.results = context.getResults();
    }

}
