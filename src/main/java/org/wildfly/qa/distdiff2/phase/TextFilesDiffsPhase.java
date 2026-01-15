package org.wildfly.qa.distdiff2.phase;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

import com.sksamuel.diffpatch.DiffMatchPatch;
import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.FileArtifact;
import org.wildfly.qa.distdiff2.errors.ErrorEvent;
import org.wildfly.qa.distdiff2.results.Status;
import org.wildfly.qa.distdiff2.tools.LineBreakStyle;
import org.wildfly.qa.distdiff2.tools.Tools;

/**
 * TextFilesDiffsPhase - Text File Content Comparison Phase
 *
 * <h3>Purpose</h3>
 * This phase performs detailed content comparison of text files that have been
 * identified as different by MD5 comparison. It generates human-readable diffs
 * and detects special cases like line-ending differences or expected changes.
 *
 * <h3>Processing Logic</h3>
 * <ol>
 *   <li><b>Eligible Artifacts</b>: Only processes {@link FileArtifact} instances that:
 *     <ul>
 *       <li>Are text files (detected via {@link Tools#isTextFile(String)})</li>
 *       <li>Have status: EXPECTED_DIFFERENCES, DIFFERENT, PATCHED_WRONG, or VERSION</li>
 *       <li>Exist in both distributions (pathA and pathB not null)</li>
 *     </ul>
 *   </li>
 *   <li><b>Diff Generation</b>: Uses DiffMatchPatch algorithm to:
 *     <ul>
 *       <li>Calculate character-level differences between files</li>
 *       <li>Apply semantic cleanup to merge related changes</li>
 *       <li>Generate HTML-formatted diff for report</li>
 *     </ul>
 *   </li>
 *   <li><b>Line Break Detection</b>: Detects CRLF vs LF differences:
 *     <ul>
 *       <li>If files differ ONLY in line endings → {@link Status#DIFFERENT_LINE_BREAKS}</li>
 *       <li>Helps distinguish platform differences from real content changes</li>
 *     </ul>
 *   </li>
 *   <li><b>Special Handling for module.xml</b>:
 *     <ul>
 *       <li><b>RPM Mode</b>: Validates version stripping (e.g., "4.2.4.Final-redhat-1" removal)</li>
 *       <li><b>Custom Build Mode</b>: Validates SNAPSHOT ↔ version-number changes</li>
 *       <li>If only expected patterns found → EXPECTED_DIFFERENCES</li>
 *       <li>If unexpected changes found → DIFFERENT</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h3>Status Transitions</h3>
 * <table border="1">
 *   <caption>Status transitions based on diff analysis</caption>
 *   <tr><th>From Status</th><th>Condition</th><th>To Status</th></tr>
 *   <tr><td>DIFFERENT</td><td>Only CRLF/LF differences</td><td>DIFFERENT_LINE_BREAKS</td></tr>
 *   <tr><td>EXPECTED_DIFFERENCES</td><td>module.xml with unexpected changes (RPM mode)</td><td>DIFFERENT</td></tr>
 *   <tr><td>DIFFERENT</td><td>module.xml with only expected changes (custom build)</td><td>EXPECTED_DIFFERENCES</td></tr>
 *   <tr><td>Any processable</td><td>Error during diff generation</td><td>ERROR</td></tr>
 * </table>
 *
 * <h3>Configuration Impact</h3>
 * <ul>
 *   <li><code>rpmAware</code>: Enables module.xml version-stripping validation</li>
 *   <li><code>fromSources</code>: Enables custom build SNAPSHOT validation</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * <ul>
 *   <li>Must run AFTER MD5SumsPhase (needs DIFFERENT artifacts)</li>
 *   <li>Must run AFTER JarVersionComparePhase (processes VERSION artifacts)</li>
 * </ul>
 *
 * @see FileArtifact
 * @see Status
 * @see ProcessPhase
 * @see DiffMatchPatch
 */
public class TextFilesDiffsPhase extends ProcessPhase {

    private static final Logger LOGGER = Logger.getLogger(TextFilesDiffsPhase.class.getName());

    private final DiffMatchPatch patch;

    public static final String NEWLINE = "\\ NEWLINE \\";

    /**
     * Default constructor
     */
    public TextFilesDiffsPhase() {
        patch = new DiffMatchPatch();
    }

    /**
     * @see ProcessPhase#process()
     */
    @Override
    public void process() {
        for (Artifact artifact : results.getArtifacts()) {
            try {
                //compare against statuses assigned in previously run phases
                if (artifact instanceof FileArtifact
                        && EnumSet.<Status>of(
                        Status.EXPECTED_DIFFERENCES,
                        Status.DIFFERENT,
                        Status.PATCHED_WRONG,
                        Status.VERSION).contains(artifact.getStatus())
                        && Tools.isTextFile(artifact.getPathA()) && Tools.isTextFile(artifact.getPathB())) {
                    calculateDiff((FileArtifact) artifact);
                }
            } catch (IOException e) {
                context.handleError(
                        new ErrorEvent("Unable to detect whether the file is text or binary", artifact));
            }
        }
    }

    private void manageModuleXml(FileArtifact artifact, LinkedList<DiffMatchPatch.Diff> diffs) {
        if (artifact.getName().equals("module.xml") && artifact.getStatus() == Status.EXPECTED_DIFFERENCES
                && distDiffConfiguration.isRpmAware()) {
            // we have a pair of module.xml's which are different, but their modules are equal...
            // in RPM mode, this might be correct, because the modules' filenames are different (the versions are stripped)
            // TODO improve the detection logic for unexpected changes in module.xml
            // so far it only tries to detect version stripping using a regular expression
            LOGGER.debug("comparing '" + artifact.getRelativePath() + "'");

            for (DiffMatchPatch.Diff diff : diffs) {
                switch (diff.operation) {
                    case EQUAL:
                        continue;
                    case INSERT:
                        LOGGER.info("Artifact '" + artifact.getRelativePath() + "': Found unexpected addition in module.xml: '" + diff.text + "'");
                        artifact.setStatus(Status.DIFFERENT, this.getClass().getSimpleName(),
                            "Unexpected content addition in module.xml (RPM mode): '" + diff.text + "'");
                        break;
                    case DELETE:
                        // if the diffing text matches a 'sane' pattern of a version removal
                        // eg. something like 4.2.4.Final-redhat-1 then let's consider this OK.
                        if (diff.text.matches("-?[0-9.a-zA-Z-_]+[.-]redhat-[0-9]+-?")) {
                            LOGGER.trace("Version strip in module.xml: '" + diff.text + "'");
                        } else {
                            LOGGER.info("Artifact '" + artifact.getRelativePath() + "': Unexpected deletion in module.xml: '" + diff.text + "'");
                            artifact.setStatus(Status.DIFFERENT, this.getClass().getSimpleName(),
                                "Unexpected content deletion in module.xml (RPM mode): '" + diff.text + "'");
                        }
                        break;
                    default:
                        // This should not happen - never
                        throw new IllegalStateException("Invalid state of 'operation' for current diff: '" + diff.text + "'");
                }
            }
        }
    }

    private void manageModuleXmlCustomBuild(FileArtifact artifact, LinkedList<DiffMatchPatch.Diff> diffs) {
        // When comparing a custom build from sources against a productized build, versions change from -redhat-X to
        // -redhat-SNAPSHOT If a module.xml only contains such differences, it should be treated as
        // EXPECTED_DIFFERENCES.
        if (artifact.getName().equals("module.xml") && artifact.getStatus() == Status.DIFFERENT
                && distDiffConfiguration.isFromSources()) {
            int i = 0;
            boolean onlyExpected = true;
            while (i < diffs.size()) {
                final DiffMatchPatch.Diff thisDiff = diffs.get(i);
                if (i == diffs.size() - 1) {
                    // the last diff
                    if (thisDiff.operation != DiffMatchPatch.Operation.EQUAL) {
                        LOGGER.debug("Found an unexpected change in " + artifact.getRelativePath() + ": " + thisDiff
                                + " is the last diff item");
                        onlyExpected = false;
                    }
                    break;
                }
                final DiffMatchPatch.Diff nextDiff = diffs.get(i + 1);
                // if the current diff's text is "SNAPSHOT" and the next is a number,
                // or vice versa, it is an expected difference
                if (thisDiff.operation != DiffMatchPatch.Operation.EQUAL) {
                    if ((thisDiff.operation == DiffMatchPatch.Operation.INSERT
                            && nextDiff.operation == DiffMatchPatch.Operation.DELETE)
                            || (thisDiff.operation == DiffMatchPatch.Operation.DELETE
                            && nextDiff.operation == DiffMatchPatch.Operation.INSERT)) {
                        if ((thisDiff.text.matches("SNAPSHOT") && nextDiff.text.matches("[0-9]+"))
                                || (nextDiff.text.matches("SNAPSHOT") && thisDiff.text.matches("[0-9]+"))) {
                            // expected
                            LOGGER.trace(
                                    "Expected change in " + artifact.getRelativePath() + ": " + thisDiff + " <-> " + nextDiff);
                            i++; // skip the next diff item, because it was successfully paired with the current one
                        } else if ((thisDiff.text.matches("SNAPSHOT") && nextDiff.text.matches("redhat-[0-9]+"))
                                || (nextDiff.text.matches("SNAPSHOT") && thisDiff.text.matches("redhat-[0-9]+"))) {
                            // expected
                            LOGGER.trace(
                                    "Expected change in " + artifact.getRelativePath() + ": " + thisDiff + " <-> " + nextDiff);
                            i++; // skip the next diff item, because it was successfully paired with the current one
                        } else {
                            LOGGER.debug("Found an unexpected change in " + artifact.getRelativePath() + ": " + thisDiff
                                    + " <-> " + nextDiff);
                            onlyExpected = false;
                            break;
                        }
                    } else {
                        LOGGER.debug("Found an unexpected change in " + artifact.getRelativePath() + ": " + thisDiff + " <-> "
                                + nextDiff);
                        onlyExpected = false;
                        break;
                    }
                }
                i++;
            }
            if (onlyExpected) {
                LOGGER.debug("Artifact '" + artifact.getRelativePath() + "': module.xml contains only expected SNAPSHOT/version differences");
                artifact.setStatus(Status.EXPECTED_DIFFERENCES, this.getClass().getSimpleName(),
                    "module.xml contains only expected redhat-SNAPSHOT to redhat-N version changes");
            }
        }
    }

    /**
     * Calculate diff for text files
     *
     * @param artifact target artifacts
     */
    private void calculateDiff(FileArtifact artifact) {
        String fileA = artifact.getPathA();
        String fileB = artifact.getPathB();
        if (fileA != null && fileB != null) {
            try {
                String contentA = Tools.readFile(fileA);
                String contentB = Tools.readFile(fileB);
                LinkedList<DiffMatchPatch.Diff> diffs = patch.diff_main(contentA, contentB);
                patch.diff_cleanupSemantic(diffs);

                for (DiffMatchPatch.Diff diff : diffs) {
                    if (diff.text.contains("\n")) {
                        // Diff text contains a new-line - let's make it more visible.
                        if (DiffMatchPatch.Operation.DELETE.equals(diff.operation)) {
                            diff.text = diff.text.replace("\n", NEWLINE);
                        } else if (DiffMatchPatch.Operation.INSERT.equals(diff.operation)) {
                            diff.text = diff.text.replace("\n", "\n" + NEWLINE);
                        }
                    }
                }

                String diff_prettyHtml = patch.diff_prettyHtml(diffs);
                diff_prettyHtml = diff_prettyHtml.replaceAll("&para;", "");

                // detect line ending style
                String lineBreaksMessage = null;
                try {
                    LOGGER.debug("Detecting line breaks.");
                    LineBreakStyle lineBreaksA = Tools.detectLineBreakStyle(artifact.getPathA());
                    LineBreakStyle lineBreaksB = Tools.detectLineBreakStyle(artifact.getPathB());
                    LOGGER.debug("A: " + lineBreaksA + ", B: " + lineBreaksB);
                    if (lineBreaksA != lineBreaksB) {
                        LOGGER.debug("Different line breaks detected.");
                        lineBreaksMessage = "Artifact in A uses " + lineBreaksA + " style line endings while B uses "
                                + lineBreaksB;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    context.handleError(new ErrorEvent("Can't detect line endings, the cause: " + e.getMessage(), artifact));
                }

                if (diffs.size() > 1) {
                    if (lineBreaksMessage != null) {
                        artifact.setTextDiff(lineBreaksMessage + "<br/><br/>" + diff_prettyHtml);
                    } else {
                        artifact.setTextDiff(diff_prettyHtml);
                    }
                    if (isDiffOfFileWithOnlyDifferentLineBreaks(diffs)) {
                        LOGGER.info("Artifact '" + artifact.getRelativePath() + "': Only line break differences detected");
                        artifact.setStatus(Status.DIFFERENT_LINE_BREAKS, this.getClass().getSimpleName(),
                            "Files differ only in line break style (CRLF vs LF)");
                    }
                } else {
                    artifact.setTextDiff(lineBreaksMessage);
                }

                manageModuleXml(artifact, diffs);
                manageModuleXmlCustomBuild(artifact, diffs);
            } catch (Exception e) {
                LOGGER.error("Artifact '" + artifact.getRelativePath() + "': Error calculating diff - " + e.getMessage(), e);
                artifact.setStatus(Status.ERROR, this.getClass().getSimpleName(), "Error calculating diff: " + e.getMessage());
            }
        }
    }

    private boolean isDiffOfFileWithOnlyDifferentLineBreaks(LinkedList<DiffMatchPatch.Diff> diffs) {
        Set<DiffMatchPatch.Operation> distinctOperations = diffs.stream()
                .map(diff -> diff.operation)
                .collect(Collectors.toSet());
        Set<DiffMatchPatch.Operation> SAME_DELETE = new HashSet<>(Arrays.asList(DiffMatchPatch.Operation.EQUAL, DiffMatchPatch.Operation.DELETE));
        Set<DiffMatchPatch.Operation> SAME_INSERT = new HashSet<>(Arrays.asList(DiffMatchPatch.Operation.EQUAL, DiffMatchPatch.Operation.INSERT));
        if (distinctOperations.equals(SAME_DELETE) || distinctOperations.equals(SAME_INSERT)) {
            /* The diff will consist of "EQUAL" lines and "DELETE"/"INSERT" with platform differences. In this case the
               difference between CRLF and LF (\r\n, \n) is CR. So we're checking if all non-EQUAL diffs are just \r.
             */
            return diffs.stream().filter(diff ->
                            diff.operation.equals(DiffMatchPatch.Operation.INSERT) || diff.operation.equals(DiffMatchPatch.Operation.DELETE))
                    .allMatch(diff -> diff.text.equals("\r"));
        }
        return false;
    }

}
