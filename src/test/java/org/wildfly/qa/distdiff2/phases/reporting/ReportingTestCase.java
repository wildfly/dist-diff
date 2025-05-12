package org.wildfly.qa.distdiff2.phases.reporting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.wildfly.qa.distdiff2.configuration.DistDiff2Context;
import org.wildfly.qa.distdiff2.execution.DistDiff2Execution;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * This testsuite performs whole dist-diff operation and then should check generated reports.
 * 
 * @author jstourac
 *
 */
public class ReportingTestCase {

    private static final String PATH_A = "src/test/resources/reporting/a";
    private static final String PATH_B = "src/test/resources/reporting/b";
    private static final String EXPECTED_ARTIFACTS_NUMBERS_RECORD = "<artifactsNumbers><a>3</a><r>2</r><d>1</d><s>2</s><o>0</o><t>8</t></artifactsNumbers>";

    private static String PATH_OUTPUT;
    private static String OUTPUT_XML;

    @Before
    public void performDistDiffRun() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context context = builder
                .pathA(PATH_A)
                .pathB(PATH_B)
                .detectServerDistributionAndRegisterPhases()
                .build();

        PATH_OUTPUT = context.getConfiguration().getOutput().getPath();
        OUTPUT_XML = PATH_OUTPUT + "/dist-diff2-output.xml";

        new DistDiff2Execution(context).execute();
    }

    @Test
    public void overalArtefactSummaryTest() throws IOException, SecurityException {
        List<String> lines = Files.readAllLines(Paths.get(OUTPUT_XML), StandardCharsets.UTF_8);

        // Find artifacts number line...
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("<artifactsNumbers")) {
                Assert.assertEquals("Overal artefact summary in generated .xml file differs from expected,",
                        EXPECTED_ARTIFACTS_NUMBERS_RECORD, line);
            }
        }
    }

}
