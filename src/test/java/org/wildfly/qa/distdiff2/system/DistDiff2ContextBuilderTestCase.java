package org.wildfly.qa.distdiff2.system;

import org.wildfly.qa.distdiff2.InvalidDistDiffConfigurationException;
import org.wildfly.qa.distdiff2.configuration.DistDiff2Context;
import org.junit.Test;

/**
 * @author Jan Martiska
 */
public class DistDiff2ContextBuilderTestCase {

    @Test(expected = InvalidDistDiffConfigurationException.class)
    public void testInvalidConfiguration() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        builder.pathA("src/test/resources/serverdistributiondetection/wildfly")
               .pathB("src/test/resources/serverdistributiondetection/wildfly")
               .build();
    }
}
