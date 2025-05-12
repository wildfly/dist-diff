package org.wildfly.qa.distdiff2.phases.textFilesDiffsPhase;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.FileArtifact;
import org.wildfly.qa.distdiff2.configuration.DistDiff2Context;
import org.wildfly.qa.distdiff2.execution.DistDiff2Execution;
import org.wildfly.qa.distdiff2.phase.MD5SumsPhase;
import org.wildfly.qa.distdiff2.phase.TextFilesDiffsPhase;
import org.wildfly.qa.distdiff2.results.Results;
import org.wildfly.qa.distdiff2.results.Status;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cases for comparing text files - {@link TextFilesDiffsPhase}.
 * <p>
 *
 * @author Jan Stourac
 */
public class TextFilesDiffsPhaseTestCase {

    private static Results results;

    private Artifact artifact;

    private static Path rootA;
    private static Path rootB;

    @BeforeClass
    public static void prepareResults() throws IOException {
        rootA = Files.createTempDirectory("rootA");
        rootB = Files.createTempDirectory("rootB");

        try {
            // same files
            Path fileAA = Files.createFile(Paths.get(rootA.toAbsolutePath().toString(), "fileA"));
            Path fileAB = Files.createFile(Paths.get(rootB.toAbsolutePath().toString(), "fileA"));
            Files.write(fileAA, ("simple content").getBytes());
            Files.write(fileAB, ("simple content").getBytes());

            // same files, multiple lines
            Path fileBA = Files.createFile(Paths.get(rootA.toAbsolutePath().toString(), "fileB"));
            Path fileBB = Files.createFile(Paths.get(rootB.toAbsolutePath().toString(), "fileB"));
            Files.write(fileBA, ("simple content\nline2\n").getBytes());
            Files.write(fileBB, ("simple content\nline2\n").getBytes());

            // different files in words
            Path fileCA = Files.createFile(Paths.get(rootA.toAbsolutePath().toString(), "fileC"));
            Path fileCB = Files.createFile(Paths.get(rootB.toAbsolutePath().toString(), "fileC"));
            Files.write(fileCA, ("simple content").getBytes());
            Files.write(fileCB, ("different content").getBytes());

            // different files - same words but no endline
            Path fileDA = Files.createFile(Paths.get(rootA.toAbsolutePath().toString(), "fileD"));
            Path fileDB = Files.createFile(Paths.get(rootB.toAbsolutePath().toString(), "fileD"));
            Files.write(fileDA, ("simple content\n").getBytes());
            Files.write(fileDB, ("simple content").getBytes());

            // different files - same words but no endline between lines
            Path fileEA = Files.createFile(Paths.get(rootA.toAbsolutePath().toString(), "fileE"));
            Path fileEB = Files.createFile(Paths.get(rootB.toAbsolutePath().toString(), "fileE"));
            Files.write(fileEA, ("simple content\ncontinue").getBytes());
            Files.write(fileEB, ("simple contentcontinue").getBytes());

            // different files - same words but no endline again
            Path fileFA = Files.createFile(Paths.get(rootA.toAbsolutePath().toString(), "fileF"));
            Path fileFB = Files.createFile(Paths.get(rootB.toAbsolutePath().toString(), "fileF"));
            Files.write(fileFA, ("simple content\ncontinue").getBytes());
            Files.write(fileFB, ("simple content\ncontinue\n").getBytes());

            // different files - both words and endline
            Path fileGA = Files.createFile(Paths.get(rootA.toAbsolutePath().toString(), "fileG"));
            Path fileGB = Files.createFile(Paths.get(rootB.toAbsolutePath().toString(), "fileG"));
            Files.write(fileGA, ("simple content\ncontinue").getBytes());
            Files.write(fileGB, ("simple contentand something else").getBytes());

            // different files - just line breaks
            Path fileHA = Files.createFile(Paths.get(rootA.toAbsolutePath().toString(), "fileH"));
            Path fileHB = Files.createFile(Paths.get(rootB.toAbsolutePath().toString(), "fileH"));
            Files.write(fileHA, ("fsimple content\nfnext line\n").getBytes(StandardCharsets.UTF_8));
            Files.write(fileHB, ("fsimple content\r\nfnext line\r\n").getBytes(StandardCharsets.UTF_8));

            // different files - just line breaks, reversed
            Path fileIA = Files.createFile(Paths.get(rootA.toAbsolutePath().toString(), "fileI"));
            Path fileIB = Files.createFile(Paths.get(rootB.toAbsolutePath().toString(), "fileI"));
            Files.write(fileIA, ("fsimple content\r\nfnext line\r\n").getBytes(StandardCharsets.UTF_8));
            Files.write(fileIB, ("fsimple content\nfnext line\n").getBytes(StandardCharsets.UTF_8));


            // Okay, resources are created, now execute dist-diff itself
            DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
            DistDiff2Context ctx = builder
                    .pathA(rootA.toAbsolutePath().toString())
                    .pathB(rootB.toAbsolutePath().toString())
                    // We need to first check files for differences so XmlFilesComparePhase is executed.
                    .processPhase(MD5SumsPhase.class)
                    .processPhase(TextFilesDiffsPhase.class)
                    .build();
            DistDiff2Execution execution = new DistDiff2Execution(ctx);
            execution.execute();
            results = ctx.getResults();
        } finally {
            // We don't need created resources anymore as we check right against data in 'results' variable.
            if (Files.exists(rootA)) {
                Files.walk(rootA).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
            if (Files.exists(rootB)) {
                Files.walk(rootB).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    @Test
    public void sameFiles() {
        artifact = results.findArtifactByRelativePath("fileA");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals("Files are same but report states they are NOT!", Status.SAME, artifact.getStatus());
    }

    @Test
    public void sameFilesMultipleLines() {
        artifact = results.findArtifactByRelativePath("fileB");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals("Files are same but report states they are NOT!", Status.SAME, artifact.getStatus());
    }

    @Test
    public void differentFilesInWords() {
        artifact = results.findArtifactByRelativePath("fileC");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals("Files are different but report states they are NOT!", Status.DIFFERENT,
                artifact.getStatus());
        final String expectedDiff = "<del style=\"background:#ffe6e6;\">simple</del><ins style=\"background:#e6ffe6;" +
                "\">different</ins><span> content</span>";
        Assert.assertEquals(expectedDiff, ((FileArtifact) artifact).getTextDiff());
    }

    @Test
    public void differentFilesSameWordsMissingEndline() {
        artifact = results.findArtifactByRelativePath("fileD");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals("Files are different but report states they are NOT!", Status.DIFFERENT,
                artifact.getStatus());
        final String expectedDiff = "<span>simple content</span><del style=\"background:#ffe6e6;\">\\ NEWLINE \\</del>";
        Assert.assertEquals(expectedDiff, ((FileArtifact) artifact).getTextDiff());
    }

    @Test
    public void differentFilesSameWordsMissingEndlineBetweenLines() {
        artifact = results.findArtifactByRelativePath("fileE");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals("Files are different but report states they are NOT!", Status.DIFFERENT,
                artifact.getStatus());
        final String expectedDiff = "<span>simple content</span><del style=\"background:#ffe6e6;\">\\ NEWLINE " +
                "\\</del><span>continue</span>";
        Assert.assertEquals(expectedDiff, ((FileArtifact) artifact).getTextDiff());
    }

    @Test
    public void differentFilesSameWordsMissingEndlineAgain() {
        artifact = results.findArtifactByRelativePath("fileF");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals("Files are different but report states they are NOT!", Status.DIFFERENT,
                artifact.getStatus());
        final String expectedDiff = "<span>simple content<br>continue</span><ins style=\"background:#e6ffe6;\"><br>\\" +
                " NEWLINE \\</ins>";
        Assert.assertEquals(expectedDiff, ((FileArtifact) artifact).getTextDiff());
    }

    @Test
    public void differentFilesBothWordsAndNeline() {
        artifact = results.findArtifactByRelativePath("fileG");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals("Files are different but report states they are NOT!", Status.DIFFERENT,
                artifact.getStatus());
        final String expectedDiff = "<span>simple content</span><del style=\"background:#ffe6e6;\">\\ NEWLINE " +
                "\\continu</del><ins style=\"background:#e6ffe6;\">and something els</ins><span>e</span>";
        Assert.assertEquals(expectedDiff, ((FileArtifact) artifact).getTextDiff());
    }

    @Test
    public void lineBreaksDetectionStatus() {
        artifact = results.findArtifactByRelativePath("fileH");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals("Files are different but report states they are NOT!", Status.DIFFERENT_LINE_BREAKS,
                artifact.getStatus());
        final String expectedDiff = "Artifact in A uses UNIX style line endings while B uses WINDOWS<br/><br/><span>fsimple content</span><ins style=\"background:#e6ffe6;\">\r" +
                "</ins><span><br>fnext line</span><ins style=\"background:#e6ffe6;\">\r" +
                "</ins><span><br></span>";
        Assert.assertEquals(expectedDiff, ((FileArtifact) artifact).getTextDiff());
    }

    @Test
    public void lineBreaksDetectionStatusReversed() throws IOException {
        artifact = results.findArtifactByRelativePath("fileI");
        Assert.assertNotNull("Expected a file in the report, but it isn't there", artifact);
        Assert.assertEquals("Files are different but report states they are NOT!", Status.DIFFERENT_LINE_BREAKS,
                artifact.getStatus());
        final String expectedDiff = "Artifact in A uses WINDOWS style line endings while B uses UNIX<br/><br/><span>fsimple content</span><del style=\"background:#ffe6e6;\">\r" +
                "</del><span><br>fnext line</span><del style=\"background:#ffe6e6;\">\r" +
                "</del><span><br></span>";
        Assert.assertEquals(expectedDiff, ((FileArtifact) artifact).getTextDiff());
    }
}
