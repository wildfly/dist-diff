package org.wildfly.qa.distdiff2.phases.modulexml;

import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.configuration.DistDiff2Context;
import org.wildfly.qa.distdiff2.execution.DistDiff2Execution;
import org.wildfly.qa.distdiff2.results.Results;
import org.wildfly.qa.distdiff2.results.Status;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

/**
 * @author Jan Martiska
 */
public class ModuleXmlExpectedDifferencesTestCase {

    private static DistDiff2Context ctx;
    private static Results results;
    private static final String FS = File.separator;

    @BeforeClass
    public static void prepareResults() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        ctx = builder
                .pathA("src/test/resources/module_xml_differences/a")
                .pathB("src/test/resources/module_xml_differences/b")
                .detectServerDistributionAndRegisterPhases()
                .isFromSources(true)
                .build();
        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();
        results = ctx.getResults();
    }

    @Test
    public void different() {
        final Artifact artifact = results.findArtifactByRelativePath("different" + FS + "module.xml");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals(Status.DIFFERENT, artifact.getStatus());
    }

    @Test
    public void equal() {
        final Artifact artifact = results.findArtifactByRelativePath("equal" + FS + "module.xml");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals(Status.SAME, artifact.getStatus());
    }

    @Test
    public void expectedDifferences() {
        final Artifact artifact = results.findArtifactByRelativePath("expected" + FS + "module.xml");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals(Status.EXPECTED_DIFFERENCES, artifact.getStatus());
    }

    @Test
    public void expectedDifferences2() {
        final Artifact artifact = results.findArtifactByRelativePath("expected2" + FS + "module.xml");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals(Status.EXPECTED_DIFFERENCES, artifact.getStatus());
    }

    @Test
    public void expectedDifferences3() {
        final Artifact artifact = results.findArtifactByRelativePath("expected3" + FS + "module.xml");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals(Status.EXPECTED_DIFFERENCES, artifact.getStatus());
    }
}
