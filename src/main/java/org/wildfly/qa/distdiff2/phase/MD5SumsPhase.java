package org.wildfly.qa.distdiff2.phase;

import java.io.File;
import java.util.EnumSet;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.FileArtifact;
import org.wildfly.qa.distdiff2.patching.hashing.ImprovedHashingUtils;
import org.wildfly.qa.distdiff2.results.Status;
import org.wildfly.qa.distdiff2.tools.Tools;

/**
 * MD5SumsPhase - MD5 Checksum Comparison Phase
 *
 * <h3>Purpose</h3>
 * This phase calculates and compares MD5 checksums for files in both distributions
 * to detect byte-level differences. It processes artifacts that were initially marked
 * as {@link Status#SAME} (present in both distributions) to determine if they are
 * truly identical or have changed.
 *
 * <h3>Processing Logic</h3>
 * <ol>
 *   <li><b>Eligible Artifacts</b>: Only processes {@link FileArtifact} instances with status
 *       {@link Status#SAME}, {@link Status#PATCHED}, or {@link Status#PATCHED_UNNECESSARILY}</li>
 *   <li><b>MD5 Calculation</b>: Computes MD5 checksums for files in both distributions</li>
 *   <li><b>Status Transitions</b>:
 *     <ul>
 *       <li>SAME + MD5_MATCH → SAME (no change)</li>
 *       <li>SAME + MD5_DIFFER → DIFFERENT (content changed)</li>
 *       <li>PATCHED + MD5_DIFFER → PATCHED_WRONG (unexpected change in patched file)</li>
 *     </ul>
 *   </li>
 *   <li><b>Special Cases</b>:
 *     <ul>
 *       <li><b>module.xml files</b>: If MD5 differs but parent module directory hash is identical,
 *           may indicate version string changes only (expected in RPM distributions)</li>
 *       <li><b>RPM-aware mode</b>: Uses enhanced logic for module.xml validation</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h3>Dependencies</h3>
 * <ul>
 *   <li>Must run BEFORE TextFilesDiffsPhase (which needs DIFFERENT artifacts)</li>
 *   <li>Should run AFTER initial comparison (needs SAME artifacts to process)</li>
 * </ul>
 *
 * <h3>Configuration Impact</h3>
 * <ul>
 *   <li><code>rpmAware</code>: Enables special handling for module.xml files</li>
 *   <li><code>improvedHashing</code>: Uses ImprovedHashingUtils for module directory comparison</li>
 * </ul>
 *
 * @see Status
 * @see FileArtifact
 * @see ProcessPhase
 */
public class MD5SumsPhase extends ProcessPhase {

    private static final Logger LOGGER = Logger.getLogger(MD5SumsPhase.class.getName());

    /**
     * @see ProcessPhase#process()
     */
    @Override
    public void process() {
        for (Artifact artifact : results.getArtifacts()) {
            if (artifact instanceof FileArtifact) {
                calculateMD5((FileArtifact) artifact);
            }
        }
    }

    /**
     * Calculates MD5 checksums for both copies of an artifact and updates its status.
     *
     * <p>This method implements the core logic of the MD5SumsPhase. It processes artifacts
     * that are expected to be identical (SAME) or properly patched, and validates this
     * assumption by comparing MD5 checksums.
     *
     * <h4>Algorithm</h4>
     * <ol>
     *   <li>Check if artifact is eligible (status: SAME, PATCHED, or PATCHED_UNNECESSARILY)</li>
     *   <li>Calculate MD5 for file in distribution A</li>
     *   <li>Calculate MD5 for file in distribution B</li>
     *   <li>Compare checksums:
     *     <ul>
     *       <li>If equal: Store single MD5, keep current status</li>
     *       <li>If different: Handle based on current status (see below)</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * <h4>Status Update Logic (MD5 Differs)</h4>
     * <table border="1">
     *   <caption>Status transitions when MD5 checksums differ</caption>
     *   <tr><th>Current Status</th><th>New Status</th><th>Reason</th></tr>
     *   <tr><td>PATCHED or PATCHED_UNNECESSARILY</td><td>PATCHED_WRONG</td>
     *       <td>File was expected to be correctly patched but differs</td></tr>
     *   <tr><td>SAME + module.xml + module hash match</td><td>EXPECTED_DIFFERENCES (RPM) or DIFFERENT</td>
     *       <td>module.xml differs but module content is identical (likely version string change)</td></tr>
     *   <tr><td>SAME + other files</td><td>DIFFERENT</td>
     *       <td>File content has changed</td></tr>
     * </table>
     *
     * <h4>Special Case: module.xml Files</h4>
     * <p>When a module.xml file's MD5 differs, this method computes a hash of the entire
     * parent module directory (excluding module.xml itself). If the module hashes are
     * identical, it suggests that only the module.xml metadata changed, which may be
     * expected behavior in RPM distributions where version strings are stripped.
     *
     * @param artifact The file artifact to process
     * @throws Exception If MD5 calculation fails, sets artifact status to ERROR
     */
    private void calculateMD5(FileArtifact artifact) {
        if (EnumSet.<Status>of(Status.SAME, Status.PATCHED, Status.PATCHED_UNNECESSARILY)
                .contains(artifact.getStatus())) {
            String fileA = artifact.getPathA();
            String fileB = artifact.getPathB();
            if (fileA != null && fileB != null) {
                try {
                    String md5sumA = Tools.calculateMD5(fileA);
                    String md5sumB = Tools.calculateMD5(fileB);
                    if (md5sumA.equals(md5sumB)) {
                        artifact.setMd5sum(md5sumA);
                    } else {
                        LOGGER.info("Artifact '" + artifact.getRelativePath() + "': MD5 sums differ (A=" + md5sumA + ", B=" + md5sumB + ")");

                        if (EnumSet.<Status>of(Status.PATCHED, Status.PATCHED_UNNECESSARILY)
                                .contains(artifact.getStatus())) {
                            LOGGER.warn("Artifact '" + artifact.getRelativePath() + "': Expected to be patched but MD5 differs - marking as PATCHED_WRONG");
                            artifact.setStatus(Status.PATCHED_WRONG, this.getClass().getSimpleName(),
                                "MD5 checksums differ for supposedly patched file (A=" + md5sumA + ", B=" + md5sumB + ")");
                        } else {
                            // if the artifact is a module.xml and the respective modules are hash-equal, that means not patched,
                            // set the module.xml's status to VERSION or EXPECTED_DIFFERENCES
                            if (artifact.getName().endsWith("module.xml")) {
                                LOGGER.debug("Artifact '" + artifact.getRelativePath() + "': Special module.xml handling");
                                Long hashA;
                                Long hashB;
                                File moduleDirA = new File(artifact.getPathA()).getParentFile();
                                File moduleDirB = new File(artifact.getPathB()).getParentFile();
                                if (distDiffConfiguration.isImprovedHashing()) {
                                    hashA = Tools.byteArrayToInteger(
                                            ImprovedHashingUtils.calculateHash(moduleDirA));
                                    hashB = Tools.byteArrayToInteger(
                                            ImprovedHashingUtils.calculateHash(moduleDirB));
                                } else {
                                    hashA = Long.valueOf(Tools.calculateHashOfDirectory(moduleDirA));
                                    hashB = Long.valueOf(Tools.calculateHashOfDirectory(moduleDirA));
                                }
                                LOGGER.trace("hash of " + moduleDirA.getAbsolutePath() + ": " + hashA);
                                LOGGER.trace("hash of " + moduleDirB.getAbsolutePath() + ": " + hashB);
                                if (hashA.equals(hashB)) {
                                    // the module.xml's are different but the rests of their modules are equal..
                                    LOGGER.info("Artifact '" + artifact.getRelativePath() + "': module.xml differs but module hashes match - may be expected");
                                    String reason = "module.xml file differs (MD5: A=" + md5sumA + ", B=" + md5sumB + ") but module directory hashes are equal (" + hashA + ")";
                                    if (distDiffConfiguration.isRpmAware()) {
                                        // in RPM mode, this might be actually correct -> check this in TextFilesDiffsPhase.
                                        artifact.setStatus(Status.EXPECTED_DIFFERENCES, this.getClass().getSimpleName(), reason + " - RPM mode");
                                    } else {
                                        artifact.setStatus(Status.DIFFERENT, this.getClass().getSimpleName(), reason);
                                    }
                                } else {
                                    // Module hashes differ, so this is a real difference
                                    LOGGER.info("Artifact '" + artifact.getRelativePath() + "': module.xml and module hashes both differ - marking as DIFFERENT");
                                    artifact.setStatus(Status.DIFFERENT, this.getClass().getSimpleName(),
                                        "module.xml differs and module directory hashes also differ (MD5: A=" + md5sumA + ", B=" + md5sumB + ")");
                                }
                            } else {
                                LOGGER.info("Artifact '" + artifact.getRelativePath() + "': MD5 differs, marking as DIFFERENT");
                                artifact.setStatus(Status.DIFFERENT, this.getClass().getSimpleName(),
                                    "MD5 checksums differ (A=" + md5sumA + ", B=" + md5sumB + ")");
                            }
                        }
                        artifact.setMd5sumA(md5sumA);
                        artifact.setMd5sumB(md5sumB);
                    }
                } catch (Exception e) {
                    LOGGER.error("Artifact '" + artifact.getRelativePath() + "': Error calculating MD5 - " + e.getMessage(), e);
                    artifact.setStatus(Status.ERROR, this.getClass().getSimpleName(), "Error calculating MD5: " + e.getMessage());
                }
            } else {
                LOGGER.error("Artifact '" + artifact.getRelativePath() + "': Path A or Path B is null");
                artifact.setStatus(Status.ERROR, this.getClass().getSimpleName(), "Path A or Path B is null");
            }
        }
    }

}
