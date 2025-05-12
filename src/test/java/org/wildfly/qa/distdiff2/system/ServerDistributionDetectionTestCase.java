package org.wildfly.qa.distdiff2.system;

import org.wildfly.qa.distdiff2.DummyPhase;
import org.wildfly.qa.distdiff2.configuration.DistDiff2Context;
import org.wildfly.qa.distdiff2.serverdistributions.ServerDistribution;
import org.wildfly.qa.distdiff2.serverdistributions.ServerDistributionDetector;
import org.wildfly.qa.distdiff2.serverdistributions.ServerDistributionWildfly;
import org.wildfly.qa.distdiff2.serverdistributions.ServerDistributionGeneric;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jan Martiska
 */
public class ServerDistributionDetectionTestCase {

    @Test
    public void detectWildFly() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/serverdistributiondetection/wildfly")
                .pathB("src/test/resources/serverdistributiondetection/wildfly")
                .processPhase(DummyPhase.class)
                .build();
        final ServerDistributionDetector detector = new ServerDistributionDetector();
        final ServerDistribution serverDistribution = detector.detect(ctx.getConfiguration());
        Assert.assertNotNull(serverDistribution);
        Assert.assertTrue("WildFly expected, but was: " + serverDistribution.getName(), serverDistribution instanceof ServerDistributionWildfly);
    }

    @Test
    public void detectGeneric() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/empty")
                .pathB("src/test/resources/empty")
                .processPhase(DummyPhase.class)
                .build();
        final ServerDistributionDetector detector = new ServerDistributionDetector();
        final ServerDistribution serverDistribution = detector.detect(ctx.getConfiguration());
        Assert.assertNotNull(serverDistribution);
        Assert.assertTrue("Generic server distribution expected, but was: " + serverDistribution.getName(), serverDistribution instanceof ServerDistributionGeneric);
    }
}
