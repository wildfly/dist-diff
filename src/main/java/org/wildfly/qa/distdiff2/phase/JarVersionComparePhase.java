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
 * JarVersionComparePhase
 * <p>
 * Implementation of {@link ProcessPhase}, reads and compares version of jar file (version from file name)
 * Includes items which are {@link JarArtifact} and has
 * {@link Status#ADDED} or {@link Status#REMOVED} status
 * only.
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
     * Tries to find artifact with same name but different version or build. Basically iterates over artifacts that are
     * marked as {@link Status#REMOVED} and then searches for matching artifacts that are marked as
     * {@link Status#ADDED}.
     *
     * @param artifact                  target artifacts
     * @param artifacts                 List of all artifacts
     * @param filenameMappingExceptions List of artifacts that have manually set mapping to other artifact
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
                        artifact.setPathB(a.getPathB());
                        artifact.setBuildInformationB(a.getBuildInformationB());
                        if (distDiffConfiguration.isRpmAware()) {
                            String filenameRPM = new File(artifact.getPathB()).getName();
                            String expectedRPMName;
                            try {
                                expectedRPMName = translateToRPMName(artifact,
                                        filenameMappingExceptions);
                            } catch (NoImplementationVersionPresentException e) {
                                context.handleError(new ErrorEvent("Jar " + artifact.getPathB()
                                        + " doesn't contain an Implementation-Version manifest attribute, dist-diff2 is unable "
                                        + "to automatically find the mapping from zip-filename to rpm-filename. Consider specifying the mapping manually using zip-to-rpm-filename-mapping.properties.",
                                        artifact));
                                artifact.setStatus(ERROR);
                                all.remove(a);
                                break;
                            } catch (WrongImplementationVersionPresentException ex) {
                                context.handleError(new ErrorEvent("Jar " + artifact.getPathB()
                                        + " contains a probably invalid Implementation-Version manifest attribute (the value is "
                                        + ex.getVersion() + "), dist-diff2 is unable "
                                        + "to automatically find the mapping from zip-filename to rpm-filename. Consider specifying the mapping manually using zip-to-rpm-filename-mapping.properties.",
                                        artifact));
                                artifact.setStatus(ERROR);
                                all.remove(a);
                                break;
                            }
                            LOGGER.debug("Translated ZIP-name to expected RPM-name: " + new File(
                                    artifact.getPathA()).getName() + " --> " + expectedRPMName);
                            if (filenameRPM.equals(expectedRPMName)) {
                                try {
                                    String md5sumA = Tools.calculateMD5(artifact.getPathA());
                                    String md5sumB = Tools.calculateMD5(artifact.getPathB());
                                    if (md5sumA.equals(md5sumB)) {
                                        artifact.setStatus(Status.SAME);
                                    } else {
                                        artifact.setStatus(Status.DIFFERENT);
                                    }
                                    all.remove(a);
                                    break;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    artifact.setStatus(Status.ERROR);
                                }
                            } else {
                                artifact.setStatus((isSameVersion(inf, inf2)) ? Status.BUILD
                                        : Status.VERSION);
                                all.remove(a);
                                break;
                            }
                        } else {
                            artifact.setStatus(
                                    (isSameVersion(inf, inf2)) ? Status.BUILD : Status.VERSION);
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
                            LOGGER.info(
                                    "Matched translated ZIP-name with expected RPM-name: " + new File(
                                            artifact.getPathA()).getName() + " --> "
                                            + expectedRPMName);
                            try {
                                String md5sumA = Tools.calculateMD5(artifact.getPathA());
                                String md5sumB = Tools.calculateMD5(artifact.getPathB());
                                if (md5sumA.equals(md5sumB)) {
                                    artifact.setStatus(Status.SAME);
                                } else {
                                    artifact.setStatus(Status.DIFFERENT);
                                }
                                all.remove(a);
                                break;
                            } catch (Exception e) {
                                e.printStackTrace();
                                artifact.setStatus(ERROR);
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
