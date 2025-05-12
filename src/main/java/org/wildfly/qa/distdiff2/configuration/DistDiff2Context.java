package org.wildfly.qa.distdiff2.configuration;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.InvalidDistDiffConfigurationException;
import org.wildfly.qa.distdiff2.errors.ErrorEvent;
import org.wildfly.qa.distdiff2.phase.ProcessPhase;
import org.wildfly.qa.distdiff2.serverdistributions.ServerDistribution;
import org.wildfly.qa.distdiff2.serverdistributions.ServerDistributionDetector;
import org.wildfly.qa.distdiff2.results.Results;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ParserProperties;
import org.kohsuke.args4j.spi.Messages;

/**
 * Context of the dist-diff built from the configuration
 * @author Jan Martiska
 */
public final class DistDiff2Context {

    private final DistDiffConfiguration distDiffConfiguration;
    private final Results results;
    private final List<Class<? extends ProcessPhase>> phases;
    private final ServerDistribution serverDistribution;
    private static final Logger LOGGER = Logger.getLogger(DistDiff2Context.class.getName());

    private DistDiff2Context(DistDiffConfiguration distDiffConfiguration, List<Class<? extends ProcessPhase>> phases,
                             Results results, ServerDistribution serverDistribution) {
        this.distDiffConfiguration = distDiffConfiguration;
        this.phases = phases;
        this.results = results;
        this.serverDistribution = serverDistribution;
        // TODO it is stupid to create results in the builder, but the builder
        // needs it because only it has access to the CMD arguments
        // maybe move cmd arguments from results to configuration...
    }

    public DistDiffConfiguration getConfiguration() {
        return distDiffConfiguration;
    }

    public List<Class<? extends ProcessPhase>> getPhases() {
        return phases;
    }

    public Results getResults() {
        return results;
    }

    public ServerDistribution getServerDistribution() {
        return serverDistribution;
    }

    public void handleError(ErrorEvent event) {
        String artifactName = null;
        if (event.getArtifact() != null) {
            artifactName = event.getArtifact().getName();
        }
        if (artifactName != null) {
            LOGGER.warn("Error occured! Artifact=" + artifactName + ", message=" + event.getMessage());
        } else {
            LOGGER.warn("Error occured! Message=" + event.getMessage());
        }
        results.addErrorMessage(event.getMessage());
    }

    public static class Builder {

        private final DistDiffConfiguration distDiffConfiguration;
        private final List<Class<? extends ProcessPhase>> phases = new ArrayList<>();
        private static final Logger LOGGER = Logger.getLogger(Builder.class.getName());
        private final Results results = new Results();
        private ServerDistribution serverDistribution;

        public Builder() {
            this.distDiffConfiguration = new DistDiffConfiguration();
        }

        public Builder programArgumentsConfiguration(String[] args) {
            CmdLineParser parser = new CmdLineParser(this.distDiffConfiguration,
                    ParserProperties.defaults().withUsageWidth(120));
            results.setCommandLineArguments(Arrays.toString(args));
            results.setSystemHostnameArchitecture(obtainSystemHostnameAndArch());
            try {
                parser.parseArgument(args);
                if (this.distDiffConfiguration.getFolderA() != null && !this.distDiffConfiguration.getFolderA()
                        .isDirectory()) {
                    throw new CmdLineException(parser,
                            Messages.ILLEGAL_PATH, "Folder with A distribution must point to the directory");
                }
                if (this.distDiffConfiguration.getFolderB() != null && !this.distDiffConfiguration.getFolderB()
                        .isDirectory()) {
                    throw new CmdLineException(parser,
                            Messages.ILLEGAL_PATH, "Folder with B distribution must point to the directory");
                }
            } catch (CmdLineException e) {
                throw new InvalidDistDiffConfigurationException(e);
            }
            return this;
        }

        private String obtainSystemHostnameAndArch() {
            String hostname = "";
            String architecture = "";

            try {
                hostname = InetAddress.getLocalHost().getHostName();
                architecture = System.getProperty("os.arch");
            } catch (UnknownHostException | SecurityException | NullPointerException | IllegalArgumentException e) {
            } finally {
                if (hostname.isEmpty()) {
                    hostname = "unknown";
                }

                if (architecture.isEmpty()) {
                    architecture = "unknown";
                }
            }

            return hostname + " / " + architecture;
        }

        public Builder pathA(String path) {
            distDiffConfiguration.setFolderA(new File(path));
            return this;
        }

        public Builder pathB(String path) {
            distDiffConfiguration.setFolderB(new File(path));
            return this;
        }

        public Builder decompile(boolean value) {
            distDiffConfiguration.setDecompile(value);
            return this;
        }

        public Builder decompileAll(boolean value) {
            distDiffConfiguration.setDecompileAll(value);
            return this;
        }

        public Builder addedFilesFile(String path) {
            distDiffConfiguration.setAddedFilesFile(path);
            return this;
        }

        public Builder removedFilesFile(String path) {
            distDiffConfiguration.setRemovedFilesFile(path);
            return this;
        }

        public Builder modifiedFilesFile(String path) {
            distDiffConfiguration.setModifiedFilesFile(path);
            return this;
        }

        public Builder isFromSources(boolean value) {
            distDiffConfiguration.setFromSources(value);
            return this;
        }

        public Builder rpmAware(boolean value) {
            distDiffConfiguration.setRpmAware(value);
            return this;
        }

        public Builder permissionDiff(boolean value) {
            distDiffConfiguration.setPermissionsDiff(value);
            return this;
        }

        public Builder fullBinaryComparison(boolean value) {
            distDiffConfiguration.setFullBinaryComparison(value);
            return this;
        }

        public Builder instructionBinaryComparison(boolean value) {
            distDiffConfiguration.setInstructionBinaryComparison(value);
            return this;
        }

        public Builder xmlCompareLenient(boolean value) {
            distDiffConfiguration.setCompareXmlLeniently(value);
            return this;
        }

        public Builder processPhase(Class<? extends ProcessPhase> clazz) {
            LOGGER.info("Registering Phase - " + clazz);
            phases.add(clazz);
            return this;
        }

        public Builder processPhases(Class<? extends ProcessPhase>... classes) {
            LOGGER.info("Registering Phases - " + Arrays.toString(classes));
            phases.addAll(Arrays.asList(classes));
            return this;
        }

        public Builder detectServerDistributionAndRegisterPhases() {
            final ServerDistribution serverDistribution = new ServerDistributionDetector().detect(distDiffConfiguration);
            final List<Class<? extends ProcessPhase>> phases = serverDistribution.getPhases(distDiffConfiguration);
            this.phases.addAll(phases);
            this.serverDistribution = serverDistribution;
            return this;
        }

        public DistDiffConfiguration getConfiguration() {
            return distDiffConfiguration;
        }

        /**
         * Use precise matching when comparing artifacts from report with files in exclusion list. With this option
         * enabled, files won't be treated as included in exclusion list if any of their parent directory is already
         * included in file exclusion list. In other words you need to specify each file explicitly to be excluded
         * from the comparison despite it's parent directory.
         * @return instance of this builder.
         */
        public Builder preciseExclusionMatching() {
            distDiffConfiguration.setPreciseExclusionMatching(true);
            return this;
        }

        public DistDiff2Context build() throws InvalidDistDiffConfigurationException {
            if (serverDistribution == null) {
                serverDistribution = new ServerDistributionDetector().detect(distDiffConfiguration);
            }
            validate();
            return new DistDiff2Context(this.distDiffConfiguration, this.phases, this.results, this.serverDistribution);
        }

        /**
         * Validates whether this builder instance has a valid configuration
         * and therefore can be used to build a DistDiff2Context.
         * <p>
         * This method either does nothing, or throws an IllegalStateException with
         * a message describing the problem.
         */
        public Builder validate() throws InvalidDistDiffConfigurationException {
            if (phases == null || phases.isEmpty()) {
                throw new InvalidDistDiffConfigurationException("No phases were configured");
            }
            return this;
        }

    }
}
