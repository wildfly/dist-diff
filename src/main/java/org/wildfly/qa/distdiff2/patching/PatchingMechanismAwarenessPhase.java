package org.wildfly.qa.distdiff2.patching;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.FolderArtifact;
import org.wildfly.qa.distdiff2.artifacts.JarArtifact;
import org.wildfly.qa.distdiff2.patching.hashing.ImprovedHashingUtils;
import org.wildfly.qa.distdiff2.phase.ProcessPhase;
import org.wildfly.qa.distdiff2.results.Status;
import org.wildfly.qa.distdiff2.tools.Tools;

/**
 * This phase reorganizes artifacts with respect to the directory structure changes introduced by the legacy patching mechanism.
 * It must be run FIRST, so other phases will have the artifacts correctly linked together.
 * <p>
 * It is REQUIRED that distribution A is the clean unzip of the patched version and B is the version with a patch
 * applied.
 * <p>
 * algorithm:
 * for each layer:
 * for every artifact:
 * if artifact not part of this layer, continue (will be handled in its own layer)
 * <p>
 * if state==ADDED
 * -- if (the relative path contains .overlays) -&gt; is an overlay file
 * ---- if (module supposed to be patched)
 * ------ set appropriate paths
 * ------ status = SAME
 * ---- else
 * ------ set appropriate paths
 * ------ status = PATCHED_UNNECESSARILY
 * -- else (is an older version which became overlaid, this is handled through the 'REMOVED' version of the same file)
 * ---- ignore, remove (will not be handled in any later phase either)
 * <p>
 * elsif state==REMOVED
 * -- is in NEW version and not in OLD version --&gt; the module is supposed to be patched, so there must be an overlay for this file
 * -- if (there is an overlay for this file)
 * ---- ignore, remove (it is handled through the 'ADDED' version of the same file)
 * -- else
 * ---- status = NOT_PATCHED
 * <p>
 * elsif state==SAME
 * -- OLD and NEW version contain the same file
 * -- if (module supposed to be patched)
 * ---- if (there is an overlay for this file)
 * ------ ignore, remove (it is handled through the 'ADDED' version of the file)
 * ---- else
 * ------ status = NOT_PATCHED
 * -- else
 * ---- ignore, remove
 *
 * @author Jan Martiska
 */
public class PatchingMechanismAwarenessPhase extends ProcessPhase {


    private static final Logger LOGGER = Logger.getLogger(PatchingMechanismAwarenessPhase.class.getName());
    public static final String OVERLAYS = ".overlays";
    public static final String OVERLAYS_REGEXP = "\\.overlays";
    private final DirectoryHashesCache cache = new DirectoryHashesCache();

    public static final String FILE_SEPARATOR_QUOTED = Pattern.quote(File.separator);

    @Override
    public void process() {
        final String distributionA = distDiffConfiguration.getFolderA().getAbsolutePath();
        final String distributionB = distDiffConfiguration.getFolderB().getAbsolutePath();

        // Get list of layers in the distribution and perform artifact unification per each layer.
        List<String> layers = ModuleStructureTools.getLayers(distributionA, distributionB);
        for (String layer : layers) {
            updateArtifacts(distributionA, distributionB, layer);
        }
    }

    private void updateArtifacts(final String distributionA, final String distributionB, final String layer) {
        final String patchID = ModuleStructureTools.getActiveLayerPatchID(layer, distributionB);
        if (patchID != null) {
            LOGGER.info("Found " + layer + " layer patch with ID=" + patchID);
        } else {
            LOGGER.info("There is no applied overlay on the + " + layer + " layer.");
        }

        // thou shalt not use a for-each loop, for the algorithm removes elements from the collection and
        // whosoever shall use a for-each loop, shall face punishment from a diabolical ConcurrentModificationException
        for (Iterator<Artifact> artifacts = results.getArtifacts().iterator(); artifacts.hasNext();) {
            Artifact artifact = artifacts.next();
            String directory = artifact.getRelativePath().split(FILE_SEPARATOR_QUOTED)[0];

            // remove and ignore metadata files in .installation directory.. ignored in dist-diff2 completely
            if (directory.equals(".installation")) {
                artifacts.remove();
                continue;
            }
            // ignore osgi bundles completely
            if (directory.equals("bundles")) {
                artifacts.remove();
                continue;
            }

            // Just to keep consistent with previous releases, remove following directories from comparison:
            // modules
            // modules/system
            // modules/system/layers
            // Relevant artifacts representing these folers shall be removed, although not their content inside them.
            if (artifact.getRelativePath().endsWith("modules/system/layers") ||
                    artifact.getRelativePath().endsWith("modules/system") ||
                    artifact.getRelativePath().endsWith("modules")) {
                artifacts.remove();
                continue;
            }

            // process only artifacts from layer of interest and ignore artifacts from different layers
            // (not removing from the total list)
            if (!artifact.getRelativePath().contains("/layers/" + layer)) {
                LOGGER.debug("ignoring file '" + artifact.getRelativePath()
                        + "' as this layer is not subject at the moment, currently processed layer is '" + layer + "'");
                continue;
            }

            // ignore files from overlay directories which are not active
            if (artifact.getRelativePath().contains(OVERLAYS) && (patchID == null || !artifact
                    .getRelativePath().contains(patchID))) {
                LOGGER.debug("ignoring overlay file '" + artifact.getRelativePath()
                        + "' as this overlay is not active");
                artifacts.remove();
                continue;
            }

            // From now on, here are only artifacts placed in 'modules' directories.
            if (artifact instanceof FolderArtifact) {
                artifacts.remove();
                continue;
            }

            // ignore and remove the .overlays file
            if (artifact.getName().equals(OVERLAYS)) {
                artifacts.remove();
                continue;
            }

            LOGGER.debug(
                    "***********************************************************************************************");
            switch (artifact.getStatus()) {
                case ADDED:
                    if (artifact.getPathB().contains(OVERLAYS)) {
                        LOGGER.debug("Found overlay file in B: " + artifact.getPathB());
                        PatchingAwareFileArtifact f = PatchingAwareFileArtifact
                                .fromOverlayFoundInB(distributionA, distributionB, artifact.getRelativePath());

                        LOGGER.debug("Patched module root in B = " + f.getOverlaidModuleRootAbsolute());
                        LOGGER.debug("Original module root in B = " + f.getOriginalModuleRootInBAbsolute());
                        LOGGER.debug("Assumed module root in A = " + f.getModuleRootInAAbsolute());
                        LOGGER.debug("Assumed original file in A = " + f.getOriginalFileInAAbsolute());

                        if (!new File(f.getOriginalFileInAAbsolute()).exists()) {
                            artifact.setStatus(Status.ADDED);
                            continue;
                        }

                        try {
                            // For some layers (e.g. microprofile), the original module in B distro may not be present...
                            if (f.getOverlaidModuleRootAbsolute() != null && new File(f.getOriginalModuleRootInBAbsolute()).isDirectory() &&
                                    compareDirectories(f.getModuleRootInAAbsolute(),
                                            f.getOriginalModuleRootInBAbsolute())) {
                                // the modules are equal in A and B, but the patch contains these modules.. This is wrong.
                                LOGGER.info(
                                        "Patched unnecessarily! directories " + f.getModuleRootInAAbsolute() + ""
                                                + " and " + f.getOriginalModuleRootInBAbsolute()
                                                + " are equivalent");
                                artifact.setPathA(f.getOriginalFileInAAbsolute());
                                // jboss-as-version module is an exception, it should be patched always even if hashing shows that it wasn't changed in any way
                                if (distDiffConfiguration.isImprovedHashing() && f.getOverlaidModuleRootAbsolute()
                                        .contains("jboss" + File.separator + "as" + File.separator + "version")) {
                                    artifact.setStatus(Status.PATCHED);
                                } else {
                                    artifact.setStatus(Status.PATCHED_UNNECESSARILY);
                                }
                            } else {
                                LOGGER.info("Patched - OK. Directories " + f.getModuleRootInAAbsolute() + ""
                                        + " and " + f.getOriginalModuleRootInBAbsolute() + " are NOT equivalent");
                                artifact.setStatus(Status.PATCHED);
                                artifact.setPathA(f.getOriginalFileInAAbsolute());
                            }
                        } catch (Exception e) {
                            e.printStackTrace(System.out);
                            artifact.setStatus(Status.ERROR);
                        }
                    } else {
                        artifacts.remove();
                        continue;
                    }
                    break;
                case REMOVED:
                    LOGGER.debug("Found file removed in B, its path in A is: " + artifact.getPathA());
                    PatchingAwareFileArtifact file = PatchingAwareFileArtifact
                            .fromRemovedFileInB(distributionA, distributionB, artifact.getRelativePath(),
                                    patchID);
                    // check if there is an overlay for this file
                    if (new File(distributionB + File.separator + file.getRelativePathToOverlay()).exists()) {
                        LOGGER.debug("This file is overlaid, skipping for now");
                        artifacts.remove();
                        continue;
                    } else {
                        // if the module is not overlaid, OK - find the right path in the patched distribution
                        try {
                            if (compareDirectories(file.getModuleRootInAAbsolute(),
                                    file.getOriginalModuleRootInBAbsolute())) {
                                LOGGER.info("Module is not supposed to be overlaid and is not. Just a rename.");
                                String namePrefix = ((JarArtifact) artifact).getBuildInformationA().getName();
                                File assumedCounterpartInB = Tools.findJarInDirectory(namePrefix,
                                        new File(file.getOriginalModuleRootInBAbsolute()));
                                if (assumedCounterpartInB != null) {
                                    LOGGER.info(
                                            "Found counterpart in B: " + assumedCounterpartInB.getAbsolutePath());
                                    artifact.setPathB(assumedCounterpartInB.getAbsolutePath());
                                    artifact.setStatus(Status.VERSION);
                                } else {
                                    LOGGER.info("Could not find a counterpart in the patched distribution ;(");
                                    artifact.setStatus(Status.NOT_PATCHED);
                                }
                            } else {
                                LOGGER.info("Module is supposed to be overlaid but is not. State=NOT_PATCHED");
                                artifact.setStatus(Status.NOT_PATCHED);
                            }
                        } catch (Exception e) {
                            LOGGER.error(e);
                            artifact.setStatus(Status.ERROR);
                        }

                    }
                    break;
                case SAME:
                    LOGGER.debug("Found file which exists in both distributions: " + artifact.getRelativePath());
                    PatchingAwareFileArtifact f = PatchingAwareFileArtifact
                            .fromSameFileInAAndB(distributionA, distributionB, artifact.getRelativePath(),
                                    patchID);
                    try {
                        if (f.getOverlaidModuleRootAbsolute() != null) {
                            LOGGER.debug("overlay file supposed to be: " + f.getRelativePathToOverlay());
                            if (compareDirectories(f.getModuleRootInAAbsolute(),
                                    f.getOriginalModuleRootInBAbsolute())) {
                                LOGGER.debug("module is NOT supposed to be overlaid");
                                if (new File(distributionB + File.separator + f.getRelativePathToOverlay())
                                        .exists()) {
                                    // not supposed, but was overlaid
                                    // this will be handled by the ADDED file
                                    artifacts.remove();
                                }
                                // was not overlaid - ok, ignore
                            } else {
                                LOGGER.debug("module IS supposed to be overlaid");
                                if (new File(distributionB + File.separator + f.getRelativePathToOverlay())
                                        .exists()) {
                                    artifacts.remove();
                                } else {
                                    LOGGER.error("File is supposed to have an overlay, but I couldn't find one");
                                    artifact.setStatus(Status.NOT_PATCHED);
                                }
                            }
                        } else {
                            LOGGER.debug(artifact.getRelativePath() + " is not a module file");
                            // is there an overlay for this file? if yes, remove this
                            if (new File(distributionB + File.separator + f.getRelativePathToOverlay())
                                    .exists()) {
                                artifacts.remove();
                            }
                        }
                        continue;
                    } catch (Exception e) {
                        LOGGER.error(e);
                        e.printStackTrace();
                        artifact.setStatus(Status.ERROR);
                    }
                    break;
                // Following cases are ignored.
                case BUILD:
                case DIFFERENT:
                case ERROR:
                case EXPECTED_DIFFERENCES:
                case NOT_PATCHED:
                case PATCHED:
                case PATCHED_UNNECESSARILY:
                case PATCHED_WRONG:
                case VERSION:
                default:
                    // Do nothing...
            }
        }
    }

    private boolean areDirectoriesEqual(String directoryA, String directoryB) throws Exception {
        int hashA = cache.getHash(new File(directoryA));
        int hashB = cache.getHash(new File(directoryB));
        LOGGER.debug("Are modules equal? A='" + directoryA + "', B='" + directoryB + "', result='" + (hashA == hashB) +
                "'.");
        return hashA == hashB;
    }

    private boolean areDirectoriesEqualImproved(String directoryA, String directoryB) throws Exception {
        byte[] a = ImprovedHashingUtils.calculateHash(new File(directoryA));
        byte[] b = ImprovedHashingUtils.calculateHash(new File(directoryB));
        return Arrays.equals(a, b);
    }

    private boolean compareDirectories(String directoryA, String directoryB) throws Exception {
        if (distDiffConfiguration.isImprovedHashing()) {
            return areDirectoriesEqualImproved(directoryA, directoryB);
        } else {
            return areDirectoriesEqual(directoryA, directoryB);
        }
    }
}
