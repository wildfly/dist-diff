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
 * MDSumsPhase
 * <p>
 * Implementation of {@link ProcessPhase}, calculates MD5 for files and compare calculated MD5.
 * Includes items which are {@link FileArtifact} and has {@link Status#SAME} status
 * This phase assigns status {@link Status#DIFFERENT} when found which is used in later phases.
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
     * Calculates MD5 hashes for provided artifact and sets status
     *
     * @param artifact target artifacts
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
                        if (EnumSet.<Status>of(Status.PATCHED, Status.PATCHED_UNNECESSARILY)
                                .contains(artifact.getStatus())) {
                            artifact.setStatus(Status.PATCHED_WRONG);
                        } else {
                            // if the artifact is a module.xml and the respective modules are hash-equal, that means not patched,
                            // set the module.xml's status to VERSION
                            if (artifact.getName().endsWith("module.xml")) {
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
                                    if (distDiffConfiguration.isRpmAware()) {
                                        // in RPM mode, this might be actually correct -> check this in TextFilesDiffsPhase.
                                        artifact.setStatus(Status.EXPECTED_DIFFERENCES);
                                    } else {
                                        artifact.setStatus(Status.DIFFERENT);
                                    }
                                }
                            } else {
                                artifact.setStatus(Status.DIFFERENT);
                            }
                        }
                        artifact.setMd5sumA(md5sumA);
                        artifact.setMd5sumB(md5sumB);
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                    artifact.setStatus(Status.ERROR);
                }
            } else {
                artifact.setStatus(Status.ERROR);
            }
        }
    }

}
