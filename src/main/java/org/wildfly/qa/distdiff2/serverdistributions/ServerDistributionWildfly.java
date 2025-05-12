package org.wildfly.qa.distdiff2.serverdistributions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.configuration.DistDiffConfiguration;
import org.wildfly.qa.distdiff2.excludelist.ExclusionPhase;
import org.wildfly.qa.distdiff2.jardiff.JarDiffPhase;
import org.wildfly.qa.distdiff2.patching.PatchingMechanismAwarenessPhase;
import org.wildfly.qa.distdiff2.phase.BinaryFilesDiffsPhase;
import org.wildfly.qa.distdiff2.phase.JarVersionComparePhase;
import org.wildfly.qa.distdiff2.phase.MD5SumsPhase;
import org.wildfly.qa.distdiff2.phase.ModuleChangesSummaryPhase;
import org.wildfly.qa.distdiff2.phase.PermissionDiffPhase;
import org.wildfly.qa.distdiff2.phase.ProcessPhase;
import org.wildfly.qa.distdiff2.phase.ShowOnlyChangedItemsPhase;
import org.wildfly.qa.distdiff2.phase.TextFilesDiffsPhase;
import org.wildfly.qa.distdiff2.phase.XmlFilesComparePhase;
import org.wildfly.qa.distdiff2.results.ReportingPhase;
import org.wildfly.qa.distdiff2.rpm.RPMDetailsPhase;
import org.wildfly.qa.distdiff2.sorting.SortingPhase;

/**
 * Class describing specific entity - WildFly
 * @author Jan Martiska
 */
public class ServerDistributionWildfly implements ServerDistribution {

    private static final Logger LOGGER = Logger.getLogger(ServerDistributionWildfly.class);

    /**
     * Compose list of phases for {@link ServerDistributionWildfly}.
     * @param distDiffConfiguration Current configuration of dist-diff
     * @return list of phases which are required to execute
     */
    @Override
    public List<Class<? extends ProcessPhase>> getPhases(DistDiffConfiguration distDiffConfiguration) {
        final List<Class<? extends ProcessPhase>> phases = new ArrayList<>();
        if (distDiffConfiguration.isPatchingMechanismAware()) {
            phases.add(PatchingMechanismAwarenessPhase.class);
        }
        phases.add(MD5SumsPhase.class);
        if (distDiffConfiguration.isParseXmlAsTextDisabled()) {
            phases.add(XmlFilesComparePhase.class);
        }
        phases.addAll(Arrays.asList(
                TextFilesDiffsPhase.class,
                JarVersionComparePhase.class,
                JarDiffPhase.class,
                ModuleChangesSummaryPhase.class));
        if (distDiffConfiguration.isBinaryComparisonEnabled()) {
            phases.add(BinaryFilesDiffsPhase.class);
        }
        if (distDiffConfiguration.isPermissionsDiff()) {
            phases.add(PermissionDiffPhase.class);
        }
        if (distDiffConfiguration.isIgnoreSameItems()) {
            phases.add(ShowOnlyChangedItemsPhase.class);
        }
        phases.add(ExclusionPhase.class);
        if (distDiffConfiguration.isRpmAware()) {
            phases.add(RPMDetailsPhase.class);
        }
        phases.add(SortingPhase.class);
        phases.add(ReportingPhase.class);
        return phases;
    }

    /**
     * Detect whether the dist-diff is being run with server distribution represented by current class
     * @param distDiffConfiguration Current configuration of dist-diff
     * @return true if dist-diff is run over {@link ServerDistributionWildfly}, false otherwise.
     */
    @Override
    public boolean detect(DistDiffConfiguration distDiffConfiguration) {
        File jbossModulesJarA = new File(distDiffConfiguration.getFolderA(), "jboss-modules.jar");
        File jbossModulesJarB = new File(distDiffConfiguration.getFolderB(), "jboss-modules.jar");
        if (jbossModulesJarA.exists() && !jbossModulesJarB.exists()) {
            LOGGER.warn("Only entity in A (" + distDiffConfiguration.getFolderA() + ") was detected as " + getName() + "!");
        } else if (!jbossModulesJarA.exists() && jbossModulesJarB.exists()) {
            LOGGER.warn("Only entity in B (" + distDiffConfiguration.getFolderB() + ") was detected as " + getName() + "!");
        }
        return jbossModulesJarA.exists() && jbossModulesJarB.exists();
    }

    @Override
    public String getName() {
        return "WildFly";
    }

}
