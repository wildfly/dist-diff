package org.wildfly.qa.distdiff2.phases.exclusions;


import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.configuration.DistDiff2Context;
import org.wildfly.qa.distdiff2.excludelist.ExclusionPhase;
import org.wildfly.qa.distdiff2.execution.DistDiff2Execution;
import org.wildfly.qa.distdiff2.phase.TextFilesDiffsPhase;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jan Martiska
 */
public class ExclusionsTestCase {

    @Test
    public void testAdded() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/exclusiontest/a")
                .pathB("src/test/resources/exclusiontest/b")
                .addedFilesFile("src/test/resources/exclusiontest/added-files.txt")
                .processPhase(ExclusionPhase.class)
        .build();

        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();

        final String assertionMessage  = "File should not be contained in the report";
        final Artifact added_file = ctx.getResults().findArtifactBySimpleName("added_file");
        Assert.assertNull(assertionMessage, added_file);

        final Artifact glob_added_file = ctx.getResults().findArtifactBySimpleName("glob_added_file");
        Assert.assertNull(assertionMessage, glob_added_file);
    }

    @Test
    public void testRemoved() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/exclusiontest/a")
                .pathB("src/test/resources/exclusiontest/b")
                .removedFilesFile("src/test/resources/exclusiontest/removed-files.txt")
                .processPhase(ExclusionPhase.class)
        .build();

        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();

        final String assertionMessage = "File should not be contained in the report";
        final Artifact removed_file = ctx.getResults().findArtifactBySimpleName("removed_file");
        Assert.assertNull(assertionMessage, removed_file);

        final Artifact glob_removed_file = ctx.getResults().findArtifactBySimpleName("glob_removed_file");
        Assert.assertNull(assertionMessage, glob_removed_file);
    }

    @Test
    public void testListedAsAddedButNotFoundAtAll() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/exclusiontest/a")
                .pathB("src/test/resources/exclusiontest/b")
                .addedFilesFile("src/test/resources/exclusiontest/added-files.txt")
                .processPhase(ExclusionPhase.class)
                .build();

        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();

        Assert.assertEquals(1, ctx.getResults().getErrorMessages().size());
        final String s = ctx.getResults().getErrorMessages().get(0);
        Assert.assertTrue(s, s.contains("listed_as_added_but_not_found") && s.contains("ADDED"));
    }

    @Test
    public void testListedAsRemovedButNotFoundAtAll() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/exclusiontest/a")
                .pathB("src/test/resources/exclusiontest/b")
                .removedFilesFile("src/test/resources/exclusiontest/removed-files.txt")
                .processPhase(ExclusionPhase.class)
                .build();

        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();

        Assert.assertEquals(1, ctx.getResults().getErrorMessages().size());
        final String s = ctx.getResults().getErrorMessages().get(0);
        Assert.assertTrue(s, s.contains("listed_as_removed_but_not_found") && s.contains("REMOVED"));
    }

    @Test
    public void testModified() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/exclusiontest/a")
                .pathB("src/test/resources/exclusiontest/b")
                .modifiedFilesFile("src/test/resources/exclusiontest/modified-files1.txt")
                .processPhase(TextFilesDiffsPhase.class)
                .processPhase(ExclusionPhase.class)
                .build();
        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();

        // assert that this file did not get into the report
        Assert.assertNull(ctx.getResults().findArtifactByRelativePath("modified_file"));
    }

    // the file is listed in modified-files but it's not found at all
    // this should be logged as an error
    @Test
    public void testListedAsModifiedButNotFoundAtAll() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/exclusiontest/a")
                .pathB("src/test/resources/exclusiontest/b")
                .modifiedFilesFile("src/test/resources/exclusiontest/modified-files2.txt")
                .processPhase(TextFilesDiffsPhase.class)
                .processPhase(ExclusionPhase.class)
                .build();
        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();

        Assert.assertEquals(1, ctx.getResults().getErrorMessages().size());
        final String s = ctx.getResults().getErrorMessages().get(0);
        Assert.assertTrue(s, s.contains("listed_as_modified_but_not_found") && s.contains("different"));
    }

    /**
     * The file is listed as modified but it is actually removed.
     * This shouldn't trigger an error, the file should be just ignored altogether.
     */
    @Test
    public void testListedAsModifiedButIsRemoved() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/exclusiontest/a")
                .pathB("src/test/resources/exclusiontest/b")
                .modifiedFilesFile("src/test/resources/exclusiontest/modified-files3.txt")
                .processPhase(TextFilesDiffsPhase.class)
                .processPhase(ExclusionPhase.class)
                .build();
        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();

        Assert.assertEquals(0, ctx.getResults().getErrorMessages().size());
        Assert.assertNull(ctx.getResults().findArtifactBySimpleName("listed_as_modified_but_is_removed"));
    }

    @Test
    public void testDirectoryExclusion_Removed() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/exclusiontest/a")
                .pathB("src/test/resources/exclusiontest/b")
                .removedFilesFile("src/test/resources/exclusiontest/directory-exclusions-removed.txt")
                .processPhase(TextFilesDiffsPhase.class)
                .processPhase(ExclusionPhase.class)
                .build();
        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();

        Assert.assertEquals("There shouldn't be any reported errors", 0, ctx.getResults().getErrorMessages().size());
        Assert.assertNull(ctx.getResults().findArtifactBySimpleName("xx"));
        Assert.assertNull(ctx.getResults().findArtifactBySimpleName("yy"));
        Assert.assertNull(ctx.getResults().findArtifactBySimpleName("ignored1"));
        Assert.assertNull(ctx.getResults().findArtifactBySimpleName("ignored1i"));
    }

    @Test
    public void testDirectoryExclusion_Added() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/exclusiontest/a")
                .pathB("src/test/resources/exclusiontest/b")
                .addedFilesFile("src/test/resources/exclusiontest/directory-exclusions-added.txt")
                .processPhase(TextFilesDiffsPhase.class)
                .processPhase(ExclusionPhase.class)
                .build();
        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();

        Assert.assertEquals("There shouldn't be any reported errors", 0, ctx.getResults().getErrorMessages().size());
        Assert.assertNull(ctx.getResults().findArtifactBySimpleName("xx2"));
        Assert.assertNull(ctx.getResults().findArtifactBySimpleName("yy2"));
        Assert.assertNull(ctx.getResults().findArtifactBySimpleName("ignored2"));
        Assert.assertNull(ctx.getResults().findArtifactBySimpleName("ignored2i"));
    }

    @Test
    public void testDirectoryExclusionPreciseMatching_Added() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/exclusiontest/a")
                .pathB("src/test/resources/exclusiontest/b")
                .addedFilesFile("src/test/resources/exclusiontest/directory-exclusions-added.txt")
                .preciseExclusionMatching()
                .processPhase(TextFilesDiffsPhase.class)
                .processPhase(ExclusionPhase.class)
                .build();
        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();

        Assert.assertEquals("There shouldn't be any reported errors", 0, ctx.getResults().getErrorMessages().size());
        Assert.assertNotNull(ctx.getResults().findArtifactBySimpleName("xx2"));
        Assert.assertNotNull(ctx.getResults().findArtifactBySimpleName("yy2"));
        Assert.assertNull(ctx.getResults().findArtifactBySimpleName("ignored2"));
        Assert.assertNotNull(ctx.getResults().findArtifactBySimpleName("ignored2i"));
    }

    @Test
    public void testDirectoryExclusion_RemovedNested() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/exclusiontest/a")
                .pathB("src/test/resources/exclusiontest/b")
                .removedFilesFile("src/test/resources/exclusiontest/directory-exclusions-removed.txt")
                .processPhase(TextFilesDiffsPhase.class)
                .processPhase(ExclusionPhase.class)
                .build();
        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();

        Assert.assertEquals("There shouldn't be any reported errors", 0, ctx.getResults().getErrorMessages().size());
        Assert.assertNull(ctx.getResults().findArtifactBySimpleName("ignored_inside"));
        Assert.assertNull(ctx.getResults().findArtifactBySimpleName("ignorednested_dir"));
        Assert.assertNotNull(ctx.getResults().findArtifactBySimpleName("notignored"));
    }

    @Test
    public void testDirectoryExclusion_AddedNested() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/exclusiontest/a")
                .pathB("src/test/resources/exclusiontest/b")
                .addedFilesFile("src/test/resources/exclusiontest/directory-exclusions-added.txt")
                .processPhase(TextFilesDiffsPhase.class)
                .processPhase(ExclusionPhase.class)
                .build();
        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();

        Assert.assertEquals("There shouldn't be any reported errors", 0, ctx.getResults().getErrorMessages().size());
        Assert.assertNull(ctx.getResults().findArtifactBySimpleName("ignored_inside2"));
        Assert.assertNull(ctx.getResults().findArtifactBySimpleName("ignorednested_dir2"));
        Assert.assertNotNull(ctx.getResults().findArtifactBySimpleName("notignored2"));
    }

    @Test
    public void testDirectoryExclusionPreciseMatching_AddedNested() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context ctx = builder
                .pathA("src/test/resources/exclusiontest/a")
                .pathB("src/test/resources/exclusiontest/b")
                .addedFilesFile("src/test/resources/exclusiontest/directory-exclusions-added.txt")
                .preciseExclusionMatching()
                .processPhase(TextFilesDiffsPhase.class)
                .processPhase(ExclusionPhase.class)
                .build();
        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();

        Assert.assertEquals("There shouldn't be any reported errors", 0, ctx.getResults().getErrorMessages().size());
        Assert.assertNotNull(ctx.getResults().findArtifactBySimpleName("ignored_inside2"));
        Assert.assertNull(ctx.getResults().findArtifactBySimpleName("ignorednested_dir2"));
        Assert.assertNotNull(ctx.getResults().findArtifactBySimpleName("notignored2"));
    }

}
