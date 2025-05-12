package org.wildfly.qa.distdiff2.phase;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.FileArtifact;
import org.wildfly.qa.distdiff2.patching.ModuleStructureTools;
import org.wildfly.qa.distdiff2.results.Results;
import org.wildfly.qa.distdiff2.results.Status;
import org.wildfly.qa.distdiff2.tools.Tools;

/**
 * Generates a short report on added/removed modules between two distributions
 *
 * @author Jan Martiska
 */
public class ModuleChangesSummaryPhase extends ProcessPhase {

    @Override
    public void process() {
        Set<String> modulesInA = new HashSet<>();
        Set<String> modulesInB = new HashSet<>();
        for (Artifact artifact : results.getArtifacts()) {
            if (artifact instanceof FileArtifact && artifact.getRelativePath().endsWith("module.xml")) {
                if (EnumSet
                        .of(Status.ADDED, Status.PATCHED, Status.PATCHED_UNNECESSARILY, Status.PATCHED_WRONG,
                                Status.DIFFERENT, Status.REMOVED)
                        .contains(artifact.getStatus())) {

                    final String distributionA = distDiffConfiguration.getFolderA().getAbsolutePath();
                    final String distributionB = distDiffConfiguration.getFolderB().getAbsolutePath();
                    List<String> layers = ModuleStructureTools.getLayers(distributionA, distributionB);

                    if (artifact.getPathA() != null) { // exists in A
                        String relativeA = artifact.getPathA()
                                .replace(distDiffConfiguration.getFolderA().getAbsolutePath(), "").substring(1);
                        modulesInA.add(Tools.convertRelativePathToModuleName(relativeA, layers));
                    }

                    if (artifact.getPathB() != null) { // exists in B
                        String relativeB = artifact.getPathB()
                                .replace(distDiffConfiguration.getFolderB().getAbsolutePath(), "").substring(1);
                        modulesInB.add(Tools.convertRelativePathToModuleName(relativeB, layers));
                    }
                }
            }
        }
        output(modulesInA, modulesInB, results);
    }

    private void output(Set<String> modulesInA, Set<String> modulesInB, Results results) {
        List<String> removedModules = new ArrayList<>();
        List<String> addedModules = new ArrayList<>();
        Iterator<String> iterator = modulesInA.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            if (!modulesInB.contains(next)) {
                removedModules.add(next);
            }
        }
        iterator = modulesInB.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            if (!modulesInA.contains(next)) {
                addedModules.add(next);
            }
        }
        results.setAddedModules(addedModules);
        results.setRemovedModules(removedModules);
    }
}
