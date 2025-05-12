package org.wildfly.qa.distdiff2.serverdistributions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.wildfly.qa.distdiff2.configuration.DistDiffConfiguration;
import org.wildfly.qa.distdiff2.excludelist.ExclusionPhase;
import org.wildfly.qa.distdiff2.jardiff.JarDiffPhase;
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
 * Representation of non-specific server distribution. Will be matched against configuration as last.
 */
public class ServerDistributionGeneric implements ServerDistribution {

    @Override
    public List<Class<? extends ProcessPhase>> getPhases(DistDiffConfiguration distDiffConfiguration) {
        final List<Class<? extends ProcessPhase>> phases = new ArrayList<>();
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

    @Override
    public boolean detect(DistDiffConfiguration distDiffConfiguration) {
        return true;
    }

    @Override
    public String getName() {
        return "Unknown generic application - no specific distribution was matched";
    }

}
