package org.wildfly.qa.distdiff2.phase;

import static org.wildfly.qa.distdiff2.results.Status.ERROR;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.jar.Manifest;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.JarArtifact;
import org.wildfly.qa.distdiff2.errors.ErrorEvent;
import org.wildfly.qa.distdiff2.results.Status;
import org.wildfly.qa.distdiff2.rpm.NoImplementationVersionPresentException;
import org.wildfly.qa.distdiff2.rpm.WrongImplementationVersionPresentException;
import org.wildfly.qa.distdiff2.tools.Tools;

/**
 * JarVersionComparePhase - JAR Version Matching and Comparison Phase
 *
 * <h3>Purpose</h3>
 * This phase attempts to match JAR files that appear as ADDED in distribution B
 * with REMOVED JAR files from distribution A, identifying version upgrades, build
 * changes, or renamings. This prevents reporting version upgrades as separate
 * additions and removals.
 *
 * <h3>Processing Logic</h3>
 * <ol>
 *   <li><b>Candidate Selection</b>: For each REMOVED JAR artifact:
 *     <ol>
 *       <li>Parse build information (name, version, build) from filename</li>
 *       <li>Find ADDED artifacts in the same directory with matching characteristics</li>
 *       <li>Filter candidates by: directory match → MD5 match → name match</li>
 *     </ol>
 *   </li>
 *   <li><b>Version Comparison</b>: Compare major.minor.micro versions:
 *     <ul>
 *       <li>Same version → {@link Status#BUILD} (build number changed)</li>
 *       <li>Different version → {@link Status#VERSION} (version upgraded/downgraded)</li>
 *     </ul>
 *   </li>
 *   <li><b>RPM-Aware Mode</b>: In RPM distributions, JAR filenames may differ from ZIP:
 *     <ul>
 *       <li>Translate ZIP filename to expected RPM filename using Implementation-Version</li>
 *       <li>Check if RPM filename matches expectation</li>
 *       <li>If match + same MD5 → SAME, if match + diff MD5 → DIFFERENT</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h3>Status Transitions</h3>
 * <table border="1">
 *   <caption>Status transitions based on match characteristics</caption>
 *   <tr><th>From Status</th><th>Condition</th><th>To Status</th></tr>
 *   <tr><td>REMOVED + ADDED</td><td>Same name, same version, same build, RPM name matches, MD5 match</td><td>SAME</td></tr>
 *   <tr><td>REMOVED + ADDED</td><td>Same name, same version, same build, RPM name matches, MD5 diff</td><td>DIFFERENT</td></tr>
 *   <tr><td>REMOVED + ADDED</td><td>Same name, same version, different build</td><td>BUILD</td></tr>
 *   <tr><td>REMOVED + ADDED</td><td>Same name, different version</td><td>VERSION</td></tr>
 *   <tr><td>Any</td><td>Cannot parse Implementation-Version</td><td>ERROR</td></tr>
 * </table>
 *
 * <h3>Configuration Impact</h3>
 * <ul>
 *   <li><code>rpmAware</code>: Enables RPM filename translation logic</li>
 *   <li><code>zip-to-rpm-filename-mapping.properties</code>: Manual overrides for non-standard mappings</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * <ul>
 *   <li>Requires initial comparison (needs ADDED/REMOVED artifacts)</li>
 *   <li>Should run BEFORE TextFilesDiffsPhase</li>
 * </ul>
 *
 * @see JarArtifact
 * @see Status
 * @see ProcessPhase
 */
public class JarVersionComparePhase extends ProcessPhase {

    public static final String ZIP_TO_RPM_FILENAME_MAPPING_PROPERTIES
            = "zip-to-rpm-filename-mapping.properties";

    private static final Logger LOGGER = Logger.getLogger(JarVersionComparePhase.class.getName());


    /**
     * Default constructor
     */
    public JarVersionComparePhase() {
    }

    /**
     * @see ProcessPhase#process()
     */
    @Override
    public void process() {
        if (results == null || results.getArtifacts() == null) {
            LOGGER.warn("Jar Version Compare phase was called with null or empty list!");
            return;
        }
        Properties filenameMappingExceptions = null;
        if (distDiffConfiguration.isRpmAware()) {
            filenameMappingExceptions = loadFilenameMappingExceptions();
        }
        List<JarArtifact> jarArtifacts = new LinkedList<>();
        for (Artifact artifact : results.getArtifacts()) {
            if (artifact instanceof JarArtifact && (Status.REMOVED.equals(artifact.getStatus())
                    || Status.ADDED.equals(artifact.getStatus()))) {
                jarArtifacts.add((JarArtifact) artifact);

                // Let's compute MD5 hashsums for artifacts as we may need them later during candidates search.
                try {
                    if (artifact.getPathA() != null && ((JarArtifact) artifact).getMd5sumA() == null) {
                        ((JarArtifact) artifact).setMd5sumA(Tools.calculateMD5(artifact.getPathA()));
                    }

                    if (artifact.getPathB() != null && ((JarArtifact) artifact).getMd5sumB() == null) {
                        ((JarArtifact) artifact).setMd5sumB(Tools.calculateMD5(artifact.getPathB()));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        for (JarArtifact jarArtifact : jarArtifacts) {
            findVersionMatch(jarArtifact, jarArtifacts, results.getArtifacts(), filenameMappingExceptions);
        }
    }

    private Properties loadFilenameMappingExceptions() {
        final Properties mappings = new Properties();

        try {
            mappings.load(new FileReader(ZIP_TO_RPM_FILENAME_MAPPING_PROPERTIES));
            LOGGER.info("Reading file " + ZIP_TO_RPM_FILENAME_MAPPING_PROPERTIES);
            if (mappings.isEmpty()) {
                LOGGER.warn("No mappings found in " + ZIP_TO_RPM_FILENAME_MAPPING_PROPERTIES + "!");
            } else {
                for (String s : mappings.stringPropertyNames()) {
                    LOGGER.info(
                            "Acknowledging ZIP->RPM filename mapping exception from " + s + " to " + mappings
                                    .getProperty(s));
                }
            }
        } catch (FileNotFoundException fnfe) {
            LOGGER.warn("Cannot find " + ZIP_TO_RPM_FILENAME_MAPPING_PROPERTIES
                    + ", no custom mappings will be applied.");
        } catch (IOException e) {
            LOGGER.error("Cannot read file " + ZIP_TO_RPM_FILENAME_MAPPING_PROPERTIES
                    + ", no custom mappings will be applied.", e);
        }
        return mappings;
    }

    /**
     * Attempts to match a REMOVED JAR artifact with a corresponding ADDED JAR artifact,
     * identifying it as a version change, build change, or renaming rather than separate
     * add/remove operations.
     *
     * <h4>Algorithm</h4>
     * <ol>
     *   <li>Skip if artifact is not REMOVED (only process JARs missing from dist B)</li>
     *   <li>Parse build information from artifact filename (name, version, build)</li>
     *   <li>Find candidate matches in ADDED artifacts using progressive filtering:
     *     <ol>
     *       <li>Same parent directory path</li>
     *       <li>If multiple: same MD5 sum (exact binary match)</li>
     *       <li>If still multiple: same base name</li>
     *     </ol>
     *   </li>
     *   <li>For each candidate, compare build information:
     *     <ul>
     *       <li>If base name matches: Standard version/build comparison</li>
     *       <li>If base name differs: Try RPM filename translation (RPM mode only)</li>
     *     </ul>
     *   </li>
     *   <li>Update artifact status based on match type (see class-level Javadoc)</li>
     *   <li>Remove matched ADDED artifact from list (prevent duplicate matching)</li>
     * </ol>
     *
     * <h4>RPM-Aware Mode Logic</h4>
     * <p>In RPM distributions, JAR filenames often have version strings stripped. This method:
     * <ul>
     *   <li>Reads Implementation-Version from JAR manifest in distribution B</li>
     *   <li>Strips that version from distribution A's filename to get expected RPM name</li>
     *   <li>Compares expected name with actual distribution B filename</li>
     *   <li>If match: Further compare MD5 to determine SAME vs DIFFERENT</li>
     *   <li>If no match: Fall back to version/build comparison</li>
     * </ul>
     *
     * @param artifact                  The REMOVED jar artifact to find a match for
     * @param artifacts                 List of all JAR artifacts (both ADDED and REMOVED)
     * @param all                       Complete list of all artifacts (for removal operations)
     * @param filenameMappingExceptions Manual overrides for non-standard filename mappings
     */
    private void findVersionMatch(JarArtifact artifact, List<JarArtifact> artifacts, List<Artifact> all,
                                  Properties filenameMappingExceptions) {
        if (!Status.REMOVED.equals(artifact.getStatus())) {
            // We process here only artifacts that are marked as REMOVED - that means are present in distribution A
            // but are missing in distribution B.
            return;
        }

        JarArtifact.BuildInformation inf = getBuildInformation(artifact);
        if (inf != null && inf.getName() != null) {
            // Let's eliminate as many artifacts as possible to have ideally only one candidate to match left.
            List<JarArtifact> candidates = getListOfCandidates(artifact, inf, artifacts);

            for (JarArtifact a : candidates) {
                LOGGER.trace("candidate: " + a);
                JarArtifact.BuildInformation inf2 = getBuildInformation(a);
                if (inf2 != null) {
                    if (inf.getName().equals(inf2.getName())) {
                        LOGGER.debug("Artifact '" + artifact.getRelativePath() + "': Found matching jar '" + a.getRelativePath() + "' with same base name '" + inf.getName() + "'");
                        artifact.setPathB(a.getPathB());
                        artifact.setBuildInformationB(a.getBuildInformationB());
                        if (distDiffConfiguration.isRpmAware()) {
                            String filenameRPM = new File(artifact.getPathB()).getName();
                            String expectedRPMName;
                            try {
                                expectedRPMName = translateToRPMName(artifact,
                                        filenameMappingExceptions);
                            } catch (NoImplementationVersionPresentException e) {
                                String errorMsg = "Jar " + artifact.getPathB()
                                        + " doesn't contain an Implementation-Version manifest attribute, dist-diff2 is unable "
                                        + "to automatically find the mapping from zip-filename to rpm-filename. Consider specifying the mapping manually using zip-to-rpm-filename-mapping.properties.";
                                LOGGER.error("Artifact '" + artifact.getRelativePath() + "': " + errorMsg);
                                context.handleError(new ErrorEvent(errorMsg, artifact));
                                artifact.setStatus(ERROR, this.getClass().getSimpleName(), "No Implementation-Version in manifest");
                                all.remove(a);
                                break;
                            } catch (WrongImplementationVersionPresentException ex) {
                                String errorMsg = "Jar " + artifact.getPathB()
                                        + " contains a probably invalid Implementation-Version manifest attribute (the value is "
                                        + ex.getVersion() + "), dist-diff2 is unable "
                                        + "to automatically find the mapping from zip-filename to rpm-filename. Consider specifying the mapping manually using zip-to-rpm-filename-mapping.properties.";
                                LOGGER.error("Artifact '" + artifact.getRelativePath() + "': " + errorMsg);
                                context.handleError(new ErrorEvent(errorMsg, artifact));
                                artifact.setStatus(ERROR, this.getClass().getSimpleName(), "Invalid Implementation-Version in manifest: " + ex.getVersion());
                                all.remove(a);
                                break;
                            }
                            LOGGER.debug("Translated ZIP-name to expected RPM-name: " + new File(
                                    artifact.getPathA()).getName() + " --> " + expectedRPMName);
                            if (filenameRPM.equals(expectedRPMName)) {
                                LOGGER.debug("Artifact '" + artifact.getRelativePath() + "': RPM filename matches expected (" + expectedRPMName + "), checking MD5");
                                try {
                                    String md5sumA = Tools.calculateMD5(artifact.getPathA());
                                    String md5sumB = Tools.calculateMD5(artifact.getPathB());
                                    if (md5sumA.equals(md5sumB)) {
                                        LOGGER.debug("Artifact '" + artifact.getRelativePath() + "': MD5 match - marking as SAME");
                                        artifact.setStatus(Status.SAME, this.getClass().getSimpleName(),
                                            "RPM filename matches and MD5 sums are identical");
                                    } else {
                                        LOGGER.info("Artifact '" + artifact.getRelativePath() + "': MD5 differs - marking as DIFFERENT");
                                        artifact.setStatus(Status.DIFFERENT, this.getClass().getSimpleName(),
                                            "RPM filename matches but MD5 sums differ (A=" + md5sumA + ", B=" + md5sumB + ")");
                                    }
                                    all.remove(a);
                                    break;
                                } catch (Exception e) {
                                    LOGGER.error("Artifact '" + artifact.getRelativePath() + "': Error calculating MD5 - " + e.getMessage(), e);
                                    e.printStackTrace();
                                    artifact.setStatus(Status.ERROR, this.getClass().getSimpleName(), "Error calculating MD5: " + e.getMessage());
                                }
                            } else {
                                boolean sameVersion = isSameVersion(inf, inf2);
                                Status newStatus = sameVersion ? Status.BUILD : Status.VERSION;
                                String reason = sameVersion ?
                                    "Same version but different build: " + inf.getBuild() + " → " + inf2.getBuild() :
                                    "Version changed from " + inf.getMajorVersion() + "." + inf.getMinorVersion() + "." + inf.getMicroVersion() +
                                    " to " + inf2.getMajorVersion() + "." + inf2.getMinorVersion() + "." + inf2.getMicroVersion();
                                LOGGER.info("Artifact '" + artifact.getRelativePath() + "': " + reason);
                                artifact.setStatus(newStatus, this.getClass().getSimpleName(), reason);
                                all.remove(a);
                                break;
                            }
                        } else {
                            // Non-RPM mode: simple version/build comparison
                            boolean sameVersion = isSameVersion(inf, inf2);
                            Status newStatus = sameVersion ? Status.BUILD : Status.VERSION;
                            String reason = sameVersion ?
                                "Same version but different build: " + inf.getBuild() + " → " + inf2.getBuild() :
                                "Version changed from " + inf.getMajorVersion() + "." + inf.getMinorVersion() + "." + inf.getMicroVersion() +
                                " to " + inf2.getMajorVersion() + "." + inf2.getMinorVersion() + "." + inf2.getMicroVersion();
                            LOGGER.info("Artifact '" + artifact.getRelativePath() + "': " + reason);
                            artifact.setStatus(newStatus, this.getClass().getSimpleName(), reason);
                            all.remove(a);
                            break;
                        }
                    } else if (distDiffConfiguration.isRpmAware()) {
                        // there is no artifact of the same base name in the other distribution
                        // perhaps we were unable to parse the name correctly because it doesn't follow standard rules
                        // => try translating to rpm-name anyway
                        artifact.setPathB(a.getPathB());
                        artifact.setBuildInformationB(a.getBuildInformationB());
                        String filenameRPM = new File(artifact.getPathB()).getName();
                        String expectedRPMName;
                        try {
                            expectedRPMName = translateToRPMName(artifact,
                                    filenameMappingExceptions);
                        } catch (Exception e) {
                            continue;
                        }
                        LOGGER.trace("Base names don't match (" + inf.getName()
                                + " / " + inf2.getName()
                                + ", trying to translate zip->rpm names anyway");
                        if (filenameRPM.equals(expectedRPMName)) {
                            LOGGER.debug("Artifact '" + artifact.getRelativePath() + "': Matched translated ZIP-name with expected RPM-name: " + new File(
                                            artifact.getPathA()).getName() + " --> " + expectedRPMName);
                            try {
                                String md5sumA = Tools.calculateMD5(artifact.getPathA());
                                String md5sumB = Tools.calculateMD5(artifact.getPathB());
                                if (md5sumA.equals(md5sumB)) {
                                    LOGGER.debug("Artifact '" + artifact.getRelativePath() + "': MD5 match after RPM translation - marking as SAME");
                                    artifact.setStatus(Status.SAME, this.getClass().getSimpleName(),
                                        "RPM filename translation matched and MD5 sums are identical");
                                } else {
                                    LOGGER.info("Artifact '" + artifact.getRelativePath() + "': MD5 differs after RPM translation - marking as DIFFERENT");
                                    artifact.setStatus(Status.DIFFERENT, this.getClass().getSimpleName(),
                                        "RPM filename translation matched but MD5 sums differ (A=" + md5sumA + ", B=" + md5sumB + ")");
                                }
                                all.remove(a);
                                break;
                            } catch (Exception e) {
                                LOGGER.error("Artifact '" + artifact.getRelativePath() + "': Error calculating MD5 after RPM translation - " + e.getMessage(), e);
                                e.printStackTrace();
                                artifact.setStatus(ERROR, this.getClass().getSimpleName(), "Error calculating MD5: " + e.getMessage());
                            }
                        }
                    }
                }
            }

        }
    }


    public String translateToRPMName(JarArtifact artifact, Properties filenameMappingExceptions)
            throws NoImplementationVersionPresentException, WrongImplementationVersionPresentException {
        String zipName = new File(artifact.getPathA()).getName();
        String mappedFromExceptionFile = null;
        if (filenameMappingExceptions != null) {
            mappedFromExceptionFile = filenameMappingExceptions.getProperty(zipName);
        }
        if (mappedFromExceptionFile != null) {
            return mappedFromExceptionFile;
        } else {
            Manifest manifest = Tools.readManifestFromJar(artifact.getPathB());
            if (manifest != null) {
                String implementationVersion = manifest.getMainAttributes()
                        .getValue("Implementation-Version");
                if (implementationVersion != null) {
                    // Awesome - some value found, now we accept only version until first whitespace ignoring rest of the string
                    implementationVersion = implementationVersion.trim();
                    if (implementationVersion.matches(".*\\s+.*")) {
                        implementationVersion = implementationVersion.replaceAll("\\s+.*", "");
                    }

                    if (zipName.contains(implementationVersion)) {
                        return stripSubstringFromString("-" + implementationVersion, zipName);
                    } else {
                        throw new WrongImplementationVersionPresentException(implementationVersion);
                    }
                } else {
                    LOGGER.warn("Didn't find Implementation-Version manifest attribute in " + artifact
                            .getPathB());
                    throw new NoImplementationVersionPresentException();
                }
            } else {
                return zipName;
            }
        }
    }

    private static String stripSubstringFromString(String substring, String string) {
        return string.replace(substring, "");
    }


    /**
     * Checks if artifacts are same version
     *
     * @param a artifact build info A
     * @param b artifact build info B
     * @return <code>true</code> only if a1 has same version as a2
     */
    private boolean isSameVersion(JarArtifact.BuildInformation a, JarArtifact.BuildInformation b) {
        boolean result;
        result = (a.getMajorVersion() == null && b.getMajorVersion() == null) || (a.getMajorVersion() != null
                && a.getMajorVersion().equals(b.getMajorVersion()));
        result = result && (a.getMinorVersion() == null && b.getMinorVersion() == null) || (
                a.getMinorVersion() != null && a.getMinorVersion().equals(b.getMinorVersion()));
        result = result && (a.getMicroVersion() == null && b.getMicroVersion() == null) || (
                a.getMicroVersion() != null && a.getMicroVersion().equals(b.getMicroVersion()));
        return result;
    }

    /**
     * Returns corresponding build information according artifact status
     *
     * @param artifact instance of artifact
     * @return build information A or B
     */
    private JarArtifact.BuildInformation getBuildInformation(JarArtifact artifact) {
        JarArtifact.BuildInformation information = null;
        if (Status.REMOVED.equals(artifact.getStatus())) {
            information = artifact.getBuildInformationA();
        } else if (Status.ADDED.equals(artifact.getStatus())) {
            information = artifact.getBuildInformationB();
        }
        return information;
    }

    /**
     * Returns list of artifacts of the {@link Status#ADDED} (missing in distribution A, present in distribution B) that
     * look like matching candidates for given processed artifact which is in {@link Status#REMOVED} (present in
     * distribution A, missing in distribution B).
     * <p>
     * Priority for matching:
     * <ol>
     *     <li>artifact status ADDED</li>
     *     <li>matching artifact directory</li>
     *     <li>matching artifact MD5 sum</li>
     *     <li>matching artifact</li>
     * </ol>
     *
     * @param processedArtifact    parent path
     * @param processedArtifactInf build information for processed artifact
     * @param artifacts            list of all artifacts
     */
    private List<JarArtifact> getListOfCandidates(JarArtifact processedArtifact,
            JarArtifact.BuildInformation processedArtifactInf, List<JarArtifact> artifacts) {
        List<JarArtifact> candidates =
                getListOfCandidatesByPathAndAdded(processedArtifact.getParentRelativePath(), artifacts);

        if (candidates.size() > 1) {
            candidates = getListOfCandidatesByMd5Sum(processedArtifact.getMd5sumA(), candidates);

            if (candidates.size() > 1) {
                candidates = getListOfCandidatesByName(processedArtifactInf.getName(), candidates);
            }
        }

        return candidates;
    }

    /**
     * Returns list of artifacts in given folder with {@link Status#ADDED}.
     *
     * @param path      parent path
     * @param artifacts List of all artifacts
     */
    private List<JarArtifact> getListOfCandidatesByPathAndAdded(final String path, List<JarArtifact> artifacts) {
        List<JarArtifact> items = new LinkedList<>();
        if (path != null) {
            for (Artifact a : artifacts) {
                if (Status.ADDED.equals(a.getStatus()) && path.equals(a.getParentRelativePath())) {
                    items.add((JarArtifact) a);
                }
            }
        }
        return items;
    }

    /**
     * Returns list of artifact candidates based on same MD5 hashsum.
     *
     * @param expectedMD5Hashsum MD5 hashsum of processed artifact
     * @param addedArtifacts    list of all artifact in {@link Status#ADDED}
     */
    private List<JarArtifact> getListOfCandidatesByMd5Sum(final String expectedMD5Hashsum,
            List<JarArtifact> addedArtifacts) {
        List<JarArtifact> items = new LinkedList<>();
        if (expectedMD5Hashsum != null && !expectedMD5Hashsum.isEmpty()) {
            for (JarArtifact artifact : addedArtifacts) {
                // Only check checksum of B part as this artifact is expected to be in status of ADDED.
                if (artifact.getMd5sumB() != null && expectedMD5Hashsum.equals(artifact.getMd5sumB())) {
                    items.add(artifact);
                }
            }
        }
        return items;
    }

    /**
     * Returns list of artifact candidates based on same name.
     *
     * @param name           expected artifact name
     * @param addedArtifacts list of all artifact in {@link Status#ADDED}
     */
    private List<JarArtifact> getListOfCandidatesByName(final String name, List<JarArtifact> addedArtifacts) {
        List<JarArtifact> items = new LinkedList<>();
        if (name != null) {
            for (JarArtifact a : addedArtifacts) {
                JarArtifact.BuildInformation inf = getBuildInformation(a);
                if (inf != null && name.equals(inf.getName())) {
                    items.add(a);
                }
            }
        }
        return items;
    }
}
