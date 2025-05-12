package org.wildfly.qa.distdiff2.rpm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.FileArtifact;
import org.wildfly.qa.distdiff2.phase.ProcessPhase;
import org.wildfly.qa.distdiff2.results.Status;

/**
 * @author Jan Martiska
 */
public class RPMDetailsPhase extends ProcessPhase {

    private static final Logger LOGGER = Logger.getLogger(RPMDetailsPhase.class.getName());

    @Override
    public void process() {
        if (results == null || results.getArtifacts() == null) {
            LOGGER.warn("TextFilesDiffsPhase sum phase was called with null or empty list!");
            return;
        }

        List<FileArtifact> toBeProcessed = new ArrayList<>();
        for (Artifact artifact : results.getArtifacts()) {
            if (artifact instanceof FileArtifact && artifact.getStatus() != Status.REMOVED) {
                toBeProcessed.add((FileArtifact) artifact);
            }
        }
        int size = toBeProcessed.size();
        LOGGER.info("Getting RPM info for " + size + " files, this might take a while...");
        int step = 20;
        int done = 0;
        for (FileArtifact artifact : toBeProcessed) {
            RPMDetails details = getDetails(artifact);
            if (details.getPackageName() != null || details.getPackageShortName() != null) {
                artifact.setRpmDetails(details);
            }

            if (EnumSet.of(Status.DIFFERENT, Status.ADDED, Status.ERROR, Status.REMOVED, Status.VERSION)
                    .contains(artifact.getStatus())) {
                if ((artifact.getRpmDetails()) != null && (artifact.getRpmDetails().getPackageShortName()
                        != null)) {
                    artifact.getRpmDetails().setAvailablePackageVersions(RPMDetailsObtainingTools
                            .getAllAvailablePackageVersionsCallingYumSeparately(
                                    artifact.getRpmDetails().getPackageShortName()));
                }
            }

            done++;
            if (done % step == 0) {
                LOGGER.info("Done " + done + "/" + size);
            }
        }

    }

    private RPMDetails getDetails(FileArtifact artifact) {
        String canonicalPath;
        RPMDetails details = new RPMDetails();
        try {
            canonicalPath = new File(artifact.getPathB()).getCanonicalPath();
        } catch (IOException e) {
            LOGGER.warn("Cannot get RPM details of " + artifact, e);
            return details;
        }

        String fullPackageName = RPMDetailsObtainingTools.getFullPackageName(canonicalPath);
        details.setPackageName(fullPackageName);
        String shortPackageName = RPMDetailsObtainingTools.getShortPackageName(canonicalPath);
        details.setPackageShortName(shortPackageName);
        LOGGER.info("RPM details of " + canonicalPath + ": shortPackageName=" + shortPackageName
                + ", longPackageName=" + fullPackageName);
        return details;
    }

}
