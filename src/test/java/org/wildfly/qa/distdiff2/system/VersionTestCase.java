package org.wildfly.qa.distdiff2.system;


import org.wildfly.qa.distdiff2.DummyPhase;
import org.wildfly.qa.distdiff2.configuration.DistDiff2Context;
import org.wildfly.qa.distdiff2.execution.DistDiff2Execution;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jan Martiska
 */
public class VersionTestCase {

    @Test
    public void checkVersion() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/empty")
                .pathB("src/test/resources/empty")
                .processPhase(DummyPhase.class)
                .build();

        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();

        String version = ctx.getResults().getVersion();
        Assert.assertNotNull(version);
    }
}
