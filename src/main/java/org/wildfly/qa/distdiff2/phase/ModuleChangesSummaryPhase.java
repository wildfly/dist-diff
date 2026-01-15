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
 * ModuleChangesSummaryPhase - Module-Level Change Summary Phase
 *
 * <h3>Purpose</h3>
 * This phase generates a high-level summary of module changes between distributions
 * by analyzing module.xml files. It provides a quick overview of which modules were
 * added, removed, or changed without requiring detailed file-by-file analysis.
 *
 * <h3>Processing Logic</h3>
 * <ol>
 *   <li><b>Module Detection</b>: Identifies modules by finding module.xml files
 *     <ul>
 *       <li>Scans all artifacts with relative paths ending in "module.xml"</li>
 *       <li>Only processes modules with relevant status changes</li>
 *       <li>Converts file paths to module names (e.g., "org.jboss.logging")</li>
 *     </ul>
 *   </li>
 *   <li><b>Module Classification</b>: Categorizes modules into sets
 *     <ul>
 *       <li><b>Modules in A</b>: All modules present in distribution A</li>
 *       <li><b>Modules in B</b>: All modules present in distribution B</li>
 *     </ul>
 *   </li>
 *   <li><b>Change Detection</b>: Computes set differences
 *     <ul>
 *       <li><b>Added Modules</b>: Present in B but not in A</li>
 *       <li><b>Removed Modules</b>: Present in A but not in B</li>
 *       <li><b>Changed Modules</b>: Present in both but with modifications</li>
 *     </ul>
 *   </li>
 *   <li><b>Summary Generation</b>: Creates summary strings for report
 *     <ul>
 *       <li>Lists all added modules</li>
 *       <li>Lists all removed modules</li>
 *       <li>Lists all changed modules</li>
 *       <li>Attached to Results object for inclusion in final report</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h3>Eligible Artifacts</h3>
 * <p>Processes module.xml files with these statuses:
 * <ul>
 *   <li>{@link Status#ADDED} - New module in distribution B</li>
 *   <li>{@link Status#REMOVED} - Module removed from distribution A</li>
 *   <li>{@link Status#DIFFERENT} - Module content changed</li>
 *   <li>{@link Status#PATCHED} - Module was patched</li>
 *   <li>{@link Status#PATCHED_UNNECESSARILY} - Module patched but unnecessary</li>
 *   <li>{@link Status#PATCHED_WRONG} - Module patching failed</li>
 * </ul>
 *
 * <h3>Module Name Resolution</h3>
 * <p>Converts file paths to module names:
 * <pre>
 * Path:   modules/system/layers/base/org/jboss/logging/main/module.xml
 * Module: org.jboss.logging
 * </pre>
 * <ul>
 *   <li>Handles layered module structures (base, additional layers)</li>
 *   <li>Strips version directories (e.g., "main", "slot-name")</li>
 *   <li>Converts path separators to module name separators (. notation)</li>
 * </ul>
 *
 * <h3>Output Format</h3>
 * <p>The summary includes three sections:
 * <ul>
 *   <li><b>Added Modules</b>: List of modules new in distribution B</li>
 *   <li><b>Removed Modules</b>: List of modules no longer in distribution B</li>
 *   <li><b>Changed Modules</b>: List of modules with modifications</li>
 * </ul>
 *
 * <h3>Status Transitions</h3>
 * <p>This phase does NOT modify artifact statuses. It only reads status information
 * to generate a summary report.
 *
 * <h3>Dependencies</h3>
 * <ul>
 *   <li>Should run LATE in the phase pipeline (after status determination)</li>
 *   <li>Requires MD5SumsPhase, JarVersionComparePhase to have run first</li>
 *   <li>Must run BEFORE ReportingPhase to include summary in output</li>
 * </ul>
 *
 * <h3>Use Cases</h3>
 * <ul>
 *   <li>Quick overview of module-level changes between releases</li>
 *   <li>Identifying which subsystems were added/removed</li>
 *   <li>Validating expected module changes in release notes</li>
 *   <li>Generating change summaries for stakeholders</li>
 * </ul>
 *
 * @see ProcessPhase
 * @see ModuleStructureTools
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
