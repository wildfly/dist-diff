package org.wildfly.qa.distdiff2.phases.onlychangeditems;

import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.configuration.DistDiff2Context;
import org.wildfly.qa.distdiff2.execution.DistDiff2Execution;
import org.wildfly.qa.distdiff2.phase.ShowOnlyChangedItemsPhase;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jan Martiska
 */
public class ShowOnlyChangedItemsTestCase {

    @Test
    public void doTest() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/onlychanged/a")
                .pathB("src/test/resources/onlychanged/b")
                .processPhase(ShowOnlyChangedItemsPhase.class)
                .build();
        new DistDiff2Execution(ctx).execute();
        final Artifact artifact = ctx.getResults()
                .findArtifactBySimpleName("this-did-not-change");
        Assert.assertNull("Files without changes should not be included in the results", artifact);
    }

}
