package org.wildfly.qa.distdiff2.serverdistributions;

import java.util.ServiceLoader;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.configuration.DistDiffConfiguration;

/**
 * Used to detect which server distribution we are dealing with.
 * The implementations of org.jboss.qa.distdiff2.serverdistributions.ServerDistribution should be listed in META-INF/services/org.jboss.qa.distdiff2.serverdistributions.ServerDistribution
 * @author Jan Martiska
 */
public class ServerDistributionDetector {

    private static final Logger LOGGER = Logger.getLogger(ServerDistributionDetector.class.getName());

    public ServerDistribution detect(DistDiffConfiguration config) {
        final ServiceLoader<ServerDistribution> serverDistributions = ServiceLoader.load(ServerDistribution.class);
        for (ServerDistribution serverDistribution : serverDistributions) {
            if (serverDistribution.detect(config)) {
                LOGGER.info("Detected server distribution: " + serverDistribution.getName());
                return serverDistribution;
            }
        }
        final ServerDistributionGeneric serverDistributionGeneric = new ServerDistributionGeneric();
        LOGGER.info("Detected server distribution: " + serverDistributionGeneric.getName());
        return serverDistributionGeneric;
    }

}
