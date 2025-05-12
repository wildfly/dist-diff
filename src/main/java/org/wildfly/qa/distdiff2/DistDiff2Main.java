package org.wildfly.qa.distdiff2;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.configuration.DistDiffConfiguration;
import org.wildfly.qa.distdiff2.configuration.DistDiff2Context;
import org.wildfly.qa.distdiff2.execution.DistDiff2Execution;
import org.wildfly.qa.distdiff2.tools.DistDiff2Version;
import org.kohsuke.args4j.CmdLineParser;

/**
 * Main
 * <p>
 * Main class for Dist-diff2 project.
 */
public class DistDiff2Main {

    private static final Logger LOGGER = Logger.getLogger(DistDiff2Main.class.getName());

    /**
     * Main method
     */
    public static void main(String[] args) {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context context;
        try {
            context = builder.programArgumentsConfiguration(args).detectServerDistributionAndRegisterPhases().build();
        } catch (InvalidDistDiffConfigurationException e) {
            System.err.println(e.getMessage());
            printUsage(System.err);
            System.exit(1);
            return;
        }
        DistDiff2Execution execution = new DistDiff2Execution(context);
        execution.execute();

        boolean exitWithError = false;

        if (context.getResults().getErrorMessages().size() > 0) {
            LOGGER.info("There have been found some error messages in results!");
            exitWithError = true;
        }

        if (context.getResults().isThereAnyErrorInResults()) {
            LOGGER.info("Found at least one artifact in state " +
                    "ERROR/PATCHED_WRONG/ADDED/REMOVED/DIFFERENT/NOT_PATCHED!");
            exitWithError = true;
        }

        if (exitWithError) {
            LOGGER.info("Returning 2 as exit code...");
            System.exit(2);
        }
    }

    public static void printUsage(OutputStream target) {
        if (target == null) {
            target = System.err;
        }
        try {
            target.write("DistDiff2 -- comparison tool for Java based applications\n\n".getBytes());

            CmdLineParser parser = new CmdLineParser(new DistDiffConfiguration());
            parser.printUsage(target);

            String versionInfo = "\nDistDiff2 version: " + DistDiff2Version.VERSION + "\n";
            target.write(versionInfo.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
