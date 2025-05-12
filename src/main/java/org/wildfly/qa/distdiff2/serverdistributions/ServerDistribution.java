package org.wildfly.qa.distdiff2.serverdistributions;

import java.util.List;

import org.wildfly.qa.distdiff2.configuration.DistDiffConfiguration;
import org.wildfly.qa.distdiff2.phase.ProcessPhase;

/**
 * Represents a server distribution - an entity denoting specific phases to be run within the comparison
 * @author Jan Martiska
 */
public interface ServerDistribution {

    /**
     * Get list of phases configured to run
     * List will be composed by the specific implementation
     */
    List<Class<? extends ProcessPhase>> getPhases(DistDiffConfiguration distDiffConfiguration);

    /**
     * Try to detect whether we are dealing with this server distribution based on the supplied configuration. In most
     * cases looking into paths of compared server distribution is enough to make the detection.
     * Note that most implementations require both compared entities to be the same server distribution kind.
     */
    boolean detect(DistDiffConfiguration distDiffConfiguration);

    /**
     * Should return a simple readable name, e.g. 'WildFly'
     */
    String getName();

}
