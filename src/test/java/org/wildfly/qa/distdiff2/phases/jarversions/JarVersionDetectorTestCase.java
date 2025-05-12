package org.wildfly.qa.distdiff2.phases.jarversions;

import java.io.File;

import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.configuration.DistDiff2Context;
import org.wildfly.qa.distdiff2.execution.DistDiff2Execution;
import org.wildfly.qa.distdiff2.phase.JarVersionComparePhase;
import org.wildfly.qa.distdiff2.results.Results;
import org.wildfly.qa.distdiff2.results.Status;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jan Martiska
 */
public class JarVersionDetectorTestCase {

    /**
     * The Implementation-Version attribute in some JARs contains some trash
     * in addition to the version. Dist-diff2 should ignore everything after the first whitespace
     * following the version string.
     * <p>
     * In this test, one artifact-1.7.jar and artifact.jar contain
     * Implementation-Version: 1.7 blabla nonsense
     * <p>
     * This should not be treated as error and the two jars should be paired successfully even without
     * the need to create a manual mapping between these files. In addition, these files have the same md5 sum
     * so their status should be SAME.
     */
    @Test
    public void testIgnoreTrashInImplementationVersionAttribute() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        DistDiff2Context ctx = builder
                .pathA("src/test/resources/jarversions/a")
                .pathB("src/test/resources/jarversions/b")
                .rpmAware(true)
                .processPhase(JarVersionComparePhase.class)
                .build();
        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();
        final Results results = ctx.getResults();
        final Artifact artifact = results.findArtifactBySimpleName("artifact-1.7.jar");
        Assert.assertNotNull(artifact);
        Assert.assertTrue(artifact.getPathA().contains("a" + File.separator + "artifact-1.7.jar"));
        Assert.assertTrue(artifact.getPathB().contains("b" + File.separator + "artifact.jar"));
        Assert.assertEquals(Status.SAME, artifact.getStatus());
    }

}
