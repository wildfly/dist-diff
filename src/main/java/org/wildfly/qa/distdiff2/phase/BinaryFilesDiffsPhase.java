package org.wildfly.qa.distdiff2.phase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.LinkedList;

import com.sksamuel.diffpatch.DiffMatchPatch;
import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.Platform;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.FileArtifact;
import org.wildfly.qa.distdiff2.errors.ErrorEvent;
import org.wildfly.qa.distdiff2.results.Status;
import org.wildfly.qa.distdiff2.tools.DistDiffsDiffMatchPatch;
import org.wildfly.qa.distdiff2.tools.Tools;

/**
 * BinaryFilesDiffsPhase - Binary Executable File Comparison Phase
 *
 * <h3>Purpose</h3>
 * This phase compares binary executable files (native libraries, executables) by decompiling
 * them with objdump and comparing the resulting assembly code. This provides insight into
 * machine-level differences in compiled code.
 *
 * <h3>Scope</h3>
 * <p><b>Binary executables this phase handles:</b>
 * <ul>
 *   <li>Executable files (ELF binaries on Linux)</li>
 *   <li>Shared libraries (.so files)</li>
 *   <li>Static libraries (.a files)</li>
 * </ul>
 *
 * <p><b>NOT handled by this phase:</b>
 * <ul>
 *   <li>Archive files (.zip, .tar, .gz, .jar)</li>
 *   <li>Image files (.png, .jpg, .bmp)</li>
 *   <li>Java bytecode (.class files - handled by JarDiffPhase)</li>
 * </ul>
 *
 * <h3>Processing Logic</h3>
 * <ol>
 *   <li><b>Platform Check</b>: Only runs on Linux (requires objdump utility)
 *     <ul>
 *       <li>Gracefully skips on non-Linux platforms</li>
 *     </ul>
 *   </li>
 *   <li><b>Eligible Artifacts</b>: Only processes {@link FileArtifact} instances that:
 *     <ul>
 *       <li>Are binary executables (MIME type: application/x-sharedlib or x-executable)</li>
 *       <li>Have status: EXPECTED_DIFFERENCES, DIFFERENT, PATCHED_WRONG, or VERSION</li>
 *       <li>Exist in both distributions</li>
 *     </ul>
 *   </li>
 *   <li><b>Decompilation</b>: Uses objdump to decompile binaries
 *     <ul>
 *       <li><b>Instruction Mode</b> (-C flag): Only compares instruction tables
 *         <ul>
 *           <li>Command: {@code objdump --disassemble}</li>
 *           <li>Faster, focuses on actual code differences</li>
 *         </ul>
 *       </li>
 *       <li><b>Full Mode</b> (default -c flag): Compares all binary content
 *         <ul>
 *           <li>Command: {@code objdump --all-headers --disassemble-all}</li>
 *           <li>More thorough, includes headers and data sections</li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </li>
 *   <li><b>Diff Comparison</b>: Compares decompiled outputs
 *     <ul>
 *       <li>Uses DiffMatchPatch for character-level comparison</li>
 *       <li>Generates HTML-formatted diff for report</li>
 *       <li>Truncates very large diffs (>200,000 chars) to keep report size manageable</li>
 *     </ul>
 *   </li>
 *   <li><b>Status Update</b>:
 *     <ul>
 *       <li>If decompiled outputs are identical → SAME</li>
 *       <li>If decompiled outputs differ → DIFFERENT</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h3>Status Transitions</h3>
 * <table border="1">
 *   <caption>Status transitions based on binary comparison</caption>
 *   <tr><th>From Status</th><th>Condition</th><th>To Status</th></tr>
 *   <tr><td>DIFFERENT</td><td>Decompiled binaries identical</td><td>SAME</td></tr>
 *   <tr><td>DIFFERENT</td><td>Decompiled binaries differ</td><td>No change (remains DIFFERENT)</td></tr>
 *   <tr><td>VERSION</td><td>Decompiled binaries identical</td><td>SAME</td></tr>
 *   <tr><td>VERSION</td><td>Decompiled binaries differ</td><td>No change (remains VERSION)</td></tr>
 *   <tr><td>Any</td><td>objdump execution fails</td><td>ERROR</td></tr>
 * </table>
 *
 * <h3>Comparison Modes</h3>
 * <ul>
 *   <li><b>Instruction Mode</b> (-C / --binary-comparison-instruction):
 *     <ul>
 *       <li>Compares only instruction sections</li>
 *       <li>Faster execution</li>
 *       <li>Best for detecting actual code changes</li>
 *       <li>May miss header or metadata changes</li>
 *     </ul>
 *   </li>
 *   <li><b>Full Mode</b> (-c / --binary-comparison):
 *     <ul>
 *       <li>Compares entire binary content</li>
 *       <li>Slower execution</li>
 *       <li>Detects all changes including headers, symbols, debug info</li>
 *       <li>More comprehensive but may report non-functional differences</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>Configuration Impact</h3>
 * <ul>
 *   <li><code>-c / --binary-comparison</code>: Enable full binary comparison mode</li>
 *   <li><code>-C / --binary-comparison-instruction</code>: Enable instruction-only comparison mode</li>
 *   <li>At least one flag must be set for this phase to run</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * <ul>
 *   <li><b>Platform</b>: Linux only (requires objdump utility)</li>
 *   <li><b>External Tool</b>: objdump must be installed (usually from binutils package)</li>
 *   <li>Must run AFTER MD5SumsPhase (needs DIFFERENT artifacts)</li>
 *   <li>Should run AFTER TextFilesDiffsPhase</li>
 * </ul>
 *
 * <h3>Performance Considerations</h3>
 * <ul>
 *   <li>Decompilation can be slow for large binaries</li>
 *   <li>Diff generation can be memory-intensive</li>
 *   <li>Large diffs (>200KB) are truncated to prevent report bloat</li>
 * </ul>
 *
 * <h3>Common Use Cases</h3>
 * <ul>
 *   <li>Verifying compiled native libraries are identical</li>
 *   <li>Detecting compiler flag changes</li>
 *   <li>Identifying rebuild artifacts (same source, different build)</li>
 *   <li>Debugging native code regressions</li>
 * </ul>
 *
 * @see Status
 * @see FileArtifact
 * @see ProcessPhase
 */
public class BinaryFilesDiffsPhase extends ProcessPhase {

    private static final Logger LOGGER = Logger.getLogger(BinaryFilesDiffsPhase.class.getName());

    private final DistDiffsDiffMatchPatch patch;
    private final int MAX_LENGTH_OF_BINARY_DIFF = 200_000;

    /**
     * Default constructor
     */
    public BinaryFilesDiffsPhase() {
        patch = new DistDiffsDiffMatchPatch();
    }

    /**
     * @see ProcessPhase#process()
     */
    @Override
    public void process() {
        if (!checkCompatibility()) {
            return;
        }

        // Choose artifacts for binary comparison - only executable binaries, non-archives, non-pictures, etc.
        for (Artifact artifact : results.getArtifacts()) {
            if (artifactCompatibility(artifact)) {
                compareAndDiffFiles((FileArtifact) artifact);
            }
        }
    }

    /**
     * Performs compatibility check whether given artifact is suitable for binary comparison.
     *
     * @return true, when artifact is suitable for binary comparison, false otherwise
     */
    private boolean artifactCompatibility(Artifact artifact) {
        EnumSet<Status> suitableStatuses = EnumSet.<Status>of(Status.EXPECTED_DIFFERENCES, Status.DIFFERENT,
                Status.PATCHED_WRONG, Status.VERSION);

        if (!(artifact instanceof FileArtifact)) {
            // If this is not a file, there is no sense to check anything.
            return false;
        }

        String artifactMimeType = ((FileArtifact) artifact).getMimeType();

        return artifact instanceof FileArtifact && suitableStatuses.contains(artifact.getStatus()) &&
                (artifactMimeType.equals("application/x-sharedlib") ||
                        artifactMimeType.equals("application/octet-stream") ||
                        artifactMimeType.equals("application/x-executable"));
    }

    /**
     * Performs compatibility check whether machine can perform binary comparison.
     *
     * @return true if system is compatible to perform binary comparison phase, false otherwise.
     */
    private boolean checkCompatibility() {
        // For proper function we need to ensure that the 'objdump' tool is installed on the system. We check this
        // via 'shell' command. Thus this is expected to be executed on Unix-like machine. Thus other OSs are skipped.
        // Effectively this means that systems without '/bin/sh' or 'objdump' programs will return here with no binary
        // comparison execution at all.
        if (!Platform.isLinux()) {
            context.handleError(new ErrorEvent(
                    "Binary comparison feature is available on the Unix-like machines with 'objdump' tool installed."
                            + " This system is not Linux thus skipping this phase completely."));
            return false;
        }
        try {
            if (Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "type objdump"}).waitFor() != 0) {
                context.handleError(new ErrorEvent("There is not available 'objdump' tool on this machine. Cannot " +
                        "perform binary comparison for binary artifacts."));
                return false;
            }
        } catch (IOException | InterruptedException e) {
            context.handleError(new ErrorEvent("Could not determine whether 'objdump' tool is available on this " +
                    "machine. Cannot perform binary comparison for binary artifacts. Message: " + e.getMessage()));
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private String decompileFile(String filePath) throws FileNotFoundException {
        // Perform basic check...
        File f = new File(filePath);
        if (!f.exists() || !f.isFile()) {
            throw new FileNotFoundException("Object on '" + filePath + "' path is not file or does not exist at all!");
        }

        // Decompile file and retrieve the command output
        final Runtime rt = Runtime.getRuntime();
        final String[] cmd;
        if (distDiffConfiguration.isFullBinaryComparisonEnabled()) {
            cmd = new String[]{"objdump", "--all-headers", "--disassemble-all", filePath};
        } else {
            cmd = new String[]{"objdump", "--disassemble", filePath};
        }
        try {
            Process cmdRun = rt.exec(cmd);

            StringBuilder cmdStdOutSb = new StringBuilder();
            StringBuilder cmdErrOutSb = new StringBuilder();
            // While process is running, we have to read its STDOUT and STDERR periodically so it can continue its
            // execution...
            boolean isRunning = true;
            int exitValue = -1;
            while (isRunning) {
                try {
                    exitValue = cmdRun.exitValue();
                    isRunning = false;
                } catch (IllegalThreadStateException e) {
                    // Command is still running -> just read STDOUT and STDERR and perform another cycle spin
                    cmdStdOutSb.append(Tools.inputStreamToString(cmdRun.getInputStream(), StandardCharsets.UTF_8));
                    cmdErrOutSb.append(Tools.inputStreamToString(cmdRun.getErrorStream(), StandardCharsets.UTF_8));
                }
            }
            // Now command should be finished, perform final STDOUT and STDERR read
            cmdStdOutSb.append(Tools.inputStreamToString(cmdRun.getInputStream(), StandardCharsets.UTF_8));
            cmdErrOutSb.append(Tools.inputStreamToString(cmdRun.getErrorStream(), StandardCharsets.UTF_8));

            //remove few lines from objdump output due to containing path to the dumped file
            // 'objdump --disassemble' needs only 2 lines
            // 'objdump --all-headers --disassemble-all' needs 3 lines
            String objdumpOutputSubstring = cmdStdOutSb.toString();
            objdumpOutputSubstring = objdumpOutputSubstring.substring(objdumpOutputSubstring
                    .indexOf(System.getProperty("line.separator")) + 1);
            objdumpOutputSubstring = objdumpOutputSubstring.substring(objdumpOutputSubstring
                    .indexOf(System.getProperty("line.separator")) + 1);
            if (distDiffConfiguration.isFullBinaryComparisonEnabled()) {
                objdumpOutputSubstring = objdumpOutputSubstring.substring(objdumpOutputSubstring
                        .indexOf(System.getProperty("line.separator")) + 1);
            }

            if (exitValue != 0) {
                context.handleError(new ErrorEvent("Command '" + cmd + "' execution failed: " + cmdErrOutSb.toString()));
            } else {
                return objdumpOutputSubstring;
            }
        } catch (IOException e) {
            context.handleError(new ErrorEvent("Native command '" + cmd + "' execution failed: " + e.getMessage()));
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Calculate diff for text files
     *
     * @param artifact target artifacts
     */
    private void compareAndDiffFiles(FileArtifact artifact) {
        String fileA = artifact.getPathA();
        String fileB = artifact.getPathB();

        if (fileA != null && fileB != null) {
            try {
                String decompiledA = decompileFile(fileA);
                String decompiledB = decompileFile(fileB);

                LinkedList<DiffMatchPatch.Diff> diffs = patch.diff_main(decompiledA, decompiledB);

                if (diffs.size() > 1) {
                    patch.diff_cleanupSemantic(diffs);
                    String diff_prettyHtml = patch.diff_prettyHtmlOnlyChanged(diffs);
                    diff_prettyHtml = diff_prettyHtml.replaceAll("&para;", "");

                    if (diff_prettyHtml.length() > MAX_LENGTH_OF_BINARY_DIFF) {
                        diff_prettyHtml = "Generated diff of these files was bigger than " + MAX_LENGTH_OF_BINARY_DIFF
                                + " characters and thus was not included in this report to reduce its size. Such big "
                                + "change is suspicious and might mean that those files differ significantly."
                                + "<br>Please perform manual comparison if necessary. Decompiling can be performed "
                                + "e.g. via 'objdump ";
                        diff_prettyHtml += (distDiffConfiguration.isInstructionBinaryComparisonEnabled()) ? "--disassemble" :
                                "--all-headers --disassemble-all";
                        diff_prettyHtml += " <file>'.";
                    }

                    artifact.setTextDiff(diff_prettyHtml);
                    LOGGER.info("Artifact '" + artifact.getRelativePath() + "': Binary decompilation shows differences");
                    artifact.setStatus(Status.DIFFERENT, this.getClass().getSimpleName(),
                        "Binary files differ after objdump decompilation");
                } else {
                    LOGGER.debug("Artifact '" + artifact.getRelativePath() + "': Binary decompilation shows no differences");
                    artifact.setTextDiff(null);
                    artifact.setStatus(Status.SAME, this.getClass().getSimpleName(),
                        "Binary files are identical after objdump decompilation");
                }
            } catch (IOException e) {
                LOGGER.error("Artifact '" + artifact.getRelativePath() + "': Error during binary comparison - " + e.getMessage(), e);
                e.printStackTrace();
                artifact.setStatus(Status.ERROR, this.getClass().getSimpleName(), "Error during binary comparison: " + e.getMessage());
            }
        }
    }

}
