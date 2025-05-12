package org.wildfly.qa.distdiff2.phases.jardiff;

import org.wildfly.qa.distdiff2.artifacts.JarArtifact;
import org.wildfly.qa.distdiff2.configuration.DistDiff2Context;
import org.wildfly.qa.distdiff2.execution.DistDiff2Execution;
import org.wildfly.qa.distdiff2.jardiff.ClassDiff;
import org.wildfly.qa.distdiff2.jardiff.JarDiffPhase;
import org.wildfly.qa.distdiff2.phase.MD5SumsPhase;
import org.wildfly.qa.distdiff2.results.Results;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test that lambda expressions (compiled into synthetic methods)
 * are ignored when comparing APIs of classes
 * @author Jan Martiska
 */
public class LambdaExpressionJarDiffTestCase {

    private static DistDiff2Context ctx;
    private static Results results;
    private static JarArtifact theJar;

    @BeforeClass
    public static void prepareResults() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        ctx = builder
                .pathA("src/test/resources/jardiff/a")
                .pathB("src/test/resources/jardiff/b")
                .processPhase(MD5SumsPhase.class)
                .processPhase(JarDiffPhase.class)
                .build();
        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();
        results = ctx.getResults();
        theJar = (JarArtifact)results.findArtifactBySimpleName("thejar.jar");
        Assert.assertNotNull(theJar);
    }

    @Test
    public void testThatThereAreNoLambdaExpressionsInPublicApiDifferences() {
        final ClassDiff classDiff = theJar.getJarDiff().getClassDiffs().get("ClassWithLambdas");
        Assert.assertEquals(1, classDiff.getAddedMethods().size());
        Assert.assertTrue(classDiff.getRemovedMethods().isEmpty());
    }
}
