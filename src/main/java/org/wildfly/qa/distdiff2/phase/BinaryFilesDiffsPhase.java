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
 * This class implements phase for comparing binary executable files in case they differ somehow. By binary
 * executable we mean only executable files and static and dynamic libraries. We don't mean archives or compressed
 * files (.zip, .gzip, .tar, .jar,...), pictures (.png, .bmp,...). What we want is to compare compiled binary files
 * to see their differences in machine source code.
 * <p>
 * Implementation of {@link ProcessPhase}, calculates differences between binary files.
 * It includes artifacts according following rules:
 * <ul>
 * <li>items which are {@link FileArtifact}</li>
 * <li>{@link Status#EXPECTED_DIFFERENCES} status</li>
 * <li>{@link Status#DIFFERENT} status</li>
 * <li>{@link Status#PATCHED_WRONG} status</li>
 * <li>{@link Status#VERSION} status</li>
 * <li>its mime-type is 'application/x-sharedlib'</li>
 * </ul>
 * To be able to compare two binary files, we have to decompile them first. For this we call native command 'objdump'.
 * This way we get binary representation in readable form - symbol tables and instructions of the binary. These
 * results are then compared against each other.
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
                    artifact.setStatus(Status.DIFFERENT);
                } else {
                    artifact.setTextDiff(null);
                    artifact.setStatus(Status.SAME);
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                e.printStackTrace();
                artifact.setStatus(Status.ERROR);
            }
        }
    }

}
