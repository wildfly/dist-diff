package org.wildfly.qa.distdiff2.excludelist;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.Platform;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.errors.ErrorEvent;
import org.wildfly.qa.distdiff2.phase.ProcessPhase;
import org.wildfly.qa.distdiff2.results.Status;

/**
 * @author Jan Martiska
 */
public class ExclusionPhase extends ProcessPhase {

    public static final String DEFAULT_FILENAME_EXPECTED_ADDITIONS = "expected-added-files.txt";
    public static final String DEFAULT_FILENAME_EXPECTED_REMOVALS = "expected-removed-files.txt";
    public static final String DEFAULT_FILENAME_EXPECTED_MODIFICATIONS = "expected-modified-files.txt";

    public static final String PERMISSIONS_ONLY_EXPECTED_DIFFERENCES = "PERMISSIONS_ONLY";

    private static final Logger LOGGER = Logger.getLogger(ExclusionPhase.class.getName());


    public void process() {
        String expectedAdditionsFile = distDiffConfiguration.getAddedFilesFile() != null ?
                distDiffConfiguration.getAddedFilesFile() : DEFAULT_FILENAME_EXPECTED_ADDITIONS;

        String expectedRemovalsFile = distDiffConfiguration.getRemovedFilesFile() != null ?
                distDiffConfiguration.getRemovedFilesFile() : DEFAULT_FILENAME_EXPECTED_REMOVALS;

        String expectedModificationsFile = distDiffConfiguration.getModifiedFilesFile() != null ?
                distDiffConfiguration.getModifiedFilesFile() : DEFAULT_FILENAME_EXPECTED_MODIFICATIONS;

        List<String> expectedAdditions = parseExclusionFile(
                expectedAdditionsFile);
        List<String> expectedRemovals = parseExclusionFile(
                expectedRemovalsFile);
        List<String> expectedModifications = parseExclusionFile(
                expectedModificationsFile);

        for (String expectedAddition : expectedAdditions) {
            Iterator<Artifact> artifactIterator = results.getArtifacts().iterator();
            boolean found = false;
            while (artifactIterator.hasNext()) {
                Artifact artifact = artifactIterator.next();
                if (matchArtifactWithExclusionPath(artifact, expectedAddition,
                        distDiffConfiguration.isPreciseExclusionMatching())) {
                    found = true;
                    if (artifact.getStatus() == Status.ADDED) {
                        artifactIterator.remove();
                    } else {
                        if (artifact.getRelativePath().equals(expectedAddition)) {
                            String errorMessage = "File " + expectedAddition
                                    + " was expected to be ADDED but is " + artifact.getStatus().toString();
                            context.handleError(new ErrorEvent(errorMessage, artifact));
                        }
                    }
                }
            }
            if (!found) {
                String errorMessage = "File " + expectedAddition
                        + " was expected to be ADDED but was not found at all!";
                context.handleError(new ErrorEvent(errorMessage));
            }
        }

        for (String expectedRemoval : expectedRemovals) {
            Iterator<Artifact> artifactIterator = results.getArtifacts().iterator();
            boolean found = false;
            while (artifactIterator.hasNext()) {
                Artifact artifact = artifactIterator.next();
                if (matchArtifactWithExclusionPath(artifact, expectedRemoval,
                        distDiffConfiguration.isPreciseExclusionMatching())) {
                    found = true;
                    if (artifact.getStatus() == Status.REMOVED) {
                        artifactIterator.remove();
                    } else {
                        if (artifact.getRelativePath().equals(expectedRemoval)) {
                            String errorMessage = "File " + expectedRemoval
                                    + " was expected to be REMOVED but is " + artifact.getStatus().toString();
                            context.handleError(new ErrorEvent(errorMessage, artifact));
                        }
                    }
                }
            }
            if (!found) {
                String errorMessage = "File " + expectedRemoval
                        + " was expected to be REMOVED but was not found at all!";
                context.handleError(new ErrorEvent(errorMessage));
            }
        }

        for (String expectedModification : expectedModifications) {
            Iterator<Artifact> artifactIterator = results.getArtifacts().iterator();
            boolean found = false;

            boolean permissionOnly = false;
            if (expectedModification.startsWith(PERMISSIONS_ONLY_EXPECTED_DIFFERENCES)) {
                permissionOnly = true;
                expectedModification =
                        expectedModification.substring(PERMISSIONS_ONLY_EXPECTED_DIFFERENCES.length() + 1);
            }
            while (artifactIterator.hasNext()) {
                Artifact artifact = artifactIterator.next();
                if (matchArtifactWithExclusionPath(artifact, expectedModification,
                        distDiffConfiguration.isPreciseExclusionMatching())) {
                    found = true;
                    if (permissionOnly) {
                        artifact.setPermissionDiff(null);
                        // WARNING: this check is quite tricky as it does not have to work in case when
                        // PermissionDiffPhase is not executed as a last of the diffing phases!
                        if (artifact.isPermissionDiffOnly()) {
                            artifact.setStatus(Status.SAME);
                        }
                    } else {
                        artifactIterator.remove();
                    }
                }
            }
            if (!found) {
                String errorMessage = "File " + expectedModification
                        + " was expected to be different but was not found at all!";
                context.handleError(new ErrorEvent(errorMessage));
            }
        }


    }

    private List<String> parseExclusionFile(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            LOGGER.info("Looking into file '" + filename + "' for file exclusions...");
            List<String> expectedItems = new ArrayList<>();
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    LOGGER.info("Registering excluded path: '" + line + "'");
                    expectedItems.add(line);
                }
                line = reader.readLine();
            }
            return expectedItems;
        } catch (FileNotFoundException fnf) {
            LOGGER.warn("Couldn't find file: " + filename);
            return Collections.emptyList();
        } catch (IOException e) {
            LOGGER.warn("Cannot read file " + filename, e);
            return Collections.emptyList();
        }
    }

    private boolean matchArtifactWithExclusionPath(Artifact artifact, String exclusionPath,
                                                   final boolean usePreciseExclusionMatching) {
        LOGGER.debug("Matching file " + artifact.getRelativePath() + " to exclusion path " + exclusionPath);

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + exclusionPath);
        if (matcher.matches(Paths.get(artifact.getRelativePath()))) {
            LOGGER.debug("Excluding artifact " + artifact.getRelativePath());
            return true;
        } else if (Platform.isWindows() && exclusionPath.matches(".*[*?:><].*")) {
            //If this was a globbing pattern on Windows which didn't match we must finish with matching now.
            //Continuing may cause raising java.nio.file.InvalidPathException.
            //see reserved characters at https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file
            return false;
        }

        // if the exclusion path is a directory in at least one of the distributions
        // AND if the artifact's path is a sibling of exclusion path relative to at least one of the distributions
        // then the artifact is excluded

        final Path distroA = context.getConfiguration().getFolderA().toPath();
        final Path distroB = context.getConfiguration().getFolderB().toPath();

        final Path exclusionPathInA = Paths.get(distroA.toString(), exclusionPath).toAbsolutePath();
        final Path exclusionPathInB = Paths.get(distroB.toString(), exclusionPath).toAbsolutePath();

        Path artifactInA = null;   // this might not be defined
        if (artifact.getPathA() != null) {
            artifactInA = Paths.get(artifact.getPathA());
        }
        Path artifactInB = null;   // this might not be defined
        if (artifact.getPathB() != null) {
            artifactInB = Paths.get(artifact.getPathB());
        }

        if (!usePreciseExclusionMatching &&
                (exclusionPathInA.toFile().isDirectory() || exclusionPathInB.toFile().isDirectory())) {
            if (artifactInA != null && artifactInA.startsWith(exclusionPathInA)) {
                LOGGER.debug("Excluding artifact: " + artifact.getPathA() + " because it's a child of " + exclusionPathInA);
                return true;
            }
            if (artifactInB != null && artifactInB.startsWith(exclusionPathInB)) {
                LOGGER.debug("Excluding artifact: " + artifact.getPathB() + " because it's a child of " + exclusionPathInB);
                return true;
            }
        }
        LOGGER.debug("NOT excluding artifact: " + artifact.getRelativePath());
        return false;
    }


}
