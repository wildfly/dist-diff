package org.wildfly.qa.distdiff2.execution;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.configuration.DistDiffConfiguration;
import org.wildfly.qa.distdiff2.configuration.DistDiff2Context;
import org.wildfly.qa.distdiff2.phase.ProcessPhase;
import org.wildfly.qa.distdiff2.results.Results;
import org.wildfly.qa.distdiff2.results.Status;
import org.wildfly.qa.distdiff2.tools.Tools;

/**
 * @author Jan Martiska
 */
public class DistDiff2Execution {

    private final DistDiff2Context context;

    private static final Logger LOGGER = Logger.getLogger(DistDiff2Execution.class.getName());

    public DistDiff2Execution(DistDiff2Context context) {
        this.context = context;
    }


    public void execute() {
        final DistDiffConfiguration distDiffConfiguration = context.getConfiguration();
        final Results results = context.getResults();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Starting the diff process..");
            LOGGER.info("Configuration - " + distDiffConfiguration);
        }

        // Read all files from distribution A and B
        List<Artifact> filesA = Tools.readAllItemsFromFolder(distDiffConfiguration.getFolderA(), true);
        List<Artifact> filesB = Tools.readAllItemsFromFolder(distDiffConfiguration.getFolderB(), false);

        // Basic comparison - added and removed items
        List<Artifact> artifactList = new LinkedList<>();
        artifactList.addAll(Tools.walkThroughList(filesA, filesB, Status.REMOVED));
        artifactList.addAll(Tools.walkThroughList(filesB, filesA, Status.ADDED));
        for (Artifact artifact : filesA) {
            artifact.setStatus(Status.SAME);
            artifact.setPathB(distDiffConfiguration.getFolderB().getAbsolutePath() + File.separator + artifact
                    .getRelativePath());
            artifactList.add(artifact);
        }

        results.setFolderA(distDiffConfiguration.getFolderA().getAbsolutePath());
        results.setFolderB(distDiffConfiguration.getFolderB().getAbsolutePath());
        results.setArtifacts(artifactList);
        results.setServerDistributionName(context.getServerDistribution().getName());

        // the actual execution
        for (Class<? extends ProcessPhase> phaseClass : context.getPhases()) {
            ProcessPhase processPhase;
            try {
                processPhase = phaseClass.newInstance();
            } catch (Exception e) {
                // problem with instantiation
                throw new IllegalStateException(e);
            }
            try {
                processPhase.setContext(context);
                LOGGER.info("*****************************************************");
                LOGGER.info("Starting phase: " + phaseClass.getName());
                LOGGER.info("*****************************************************");
                final long time_start = System.currentTimeMillis();
                processPhase.process();
                final long time = System.currentTimeMillis() - time_start;
                LOGGER.info("*****************************************************");
                LOGGER.info("Phase " + phaseClass.getName() + " finished successfully in " + time + " milliseconds");
                LOGGER.info("*****************************************************");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
