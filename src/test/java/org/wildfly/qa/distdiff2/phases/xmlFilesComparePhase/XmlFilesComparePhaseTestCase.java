package org.wildfly.qa.distdiff2.phases.xmlFilesComparePhase;

import java.util.LinkedList;

import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.configuration.DistDiff2Context;
import org.wildfly.qa.distdiff2.execution.DistDiff2Execution;
import org.wildfly.qa.distdiff2.phase.MD5SumsPhase;
import org.wildfly.qa.distdiff2.phase.XmlFilesComparePhase;
import org.wildfly.qa.distdiff2.results.Results;
import org.wildfly.qa.distdiff2.results.Status;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.qa.distdiff2.phases.modulexml.ModuleXmlExpectedDifferencesTestCase;

/**
 * Test cases for comparing XML files.
 * <p>
 * NOTE: There is already {@link ModuleXmlExpectedDifferencesTestCase} which
 * covers some XML cases too, although that one is more aimed on 'module.xml' checks.
 *
 * @author Jan Stourac
 */
public class XmlFilesComparePhaseTestCase {

    private static DistDiff2Context ctx;
    private static LinkedList<Results> resultsList = new LinkedList<>();

    private Artifact artifact;

    @BeforeClass
    public static void prepareResults() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        ctx = builder
                .pathA("src/test/resources/xmlComparison/a")
                .pathB("src/test/resources/xmlComparison/b")
                // We need to first check files for differences so XmlFilesComparePhase is executed.
                .processPhase(MD5SumsPhase.class)
                .processPhase(XmlFilesComparePhase.class)
                .build();
        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();
        resultsList.add(ctx.getResults());

        // Let's perform second execution with xml lenient comparison.
        builder = new DistDiff2Context.Builder();
        ctx = builder
                .pathA("src/test/resources/xmlComparison/a")
                .pathB("src/test/resources/xmlComparison/b")
                // We need to first check files for differences so XmlFilesComparePhase is executed.
                .processPhase(MD5SumsPhase.class)
                .processPhase(XmlFilesComparePhase.class)
                .xmlCompareLenient(true)
                .build();
        execution = new DistDiff2Execution(ctx);
        execution.execute();
        resultsList.add(ctx.getResults());
    }

    @Test
    public void equal() {
        for (Results results : resultsList) {
            artifact = results.findArtifactByRelativePath("file_a.xml");
            Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
            Assert.assertEquals("Files are same but report states they are NOT!", Status.SAME, artifact.getStatus());
        }
    }

    @Test
    public void differentComment() {
        for (Results results : resultsList) {
            artifact = results.findArtifactByRelativePath("file_b.xml");
            Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
            Assert.assertEquals("Files have different comments, expect to be marked as SAME", Status.SAME,
                    artifact.getStatus());
        }
    }

    @Test
    public void differentElementContent() {
        for (Results results : resultsList) {
            artifact = results.findArtifactByRelativePath("file_c.xml");
            Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
            Assert.assertEquals("Files have different content in element(s), expect to be marked as DIFFERENT",
                    Status.DIFFERENT, artifact.getStatus());
        }
    }

    @Test
    public void sameContentDifferentElementOrder() {
        artifact = resultsList.getFirst().findArtifactByRelativePath("file_d.xml");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals("Files have same content in element(s) but different order of some elements, expect to be" +
                " marked as DIFFERENT", Status.DIFFERENT, artifact.getStatus());

        // Lenient execution should mark these files as same.
        artifact = resultsList.getLast().findArtifactByRelativePath("file_d.xml");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals("Files have same content in element(s) but different order of some elements, expect to be" +
                " marked as SAME", Status.SAME, artifact.getStatus());
    }

    @Test
    public void sameContentDifferentElementWithParametersOrder() {
        artifact = resultsList.getFirst().findArtifactByRelativePath("file_e.xml");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals("Files have same content in element(s) but different order of some elements (with parameter), expect to be" +
                " marked as DIFFERENT", Status.DIFFERENT, artifact.getStatus());

        // Lenient execution should mark these files as same.
        artifact = resultsList.getLast().findArtifactByRelativePath("file_e.xml");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals("Files have same content in element(s) but different order of some elements (with parameter), expect to be" +
                " marked as SAME", Status.SAME, artifact.getStatus());
    }

    @Test
    public void differentContentDifferentElementOrder() {
        for (Results results : resultsList) {
            artifact = results.findArtifactByRelativePath("file_f.xml");
            Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
            Assert.assertEquals("Files have different content in element(s) and also different order of some elements, " +
                    "expect to be marked as SAME", Status.DIFFERENT, artifact.getStatus());
        }
    }
}
