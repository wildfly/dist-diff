package org.wildfly.qa.distdiff2.phase;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.ItemWithChild;
import org.wildfly.qa.distdiff2.results.Status;

/**
 * ShowOnlyChangedItemsPhase - Filter Phase to Remove Unchanged Items
 *
 * <h3>Purpose</h3>
 * This phase filters the results to show only changed items, removing all artifacts
 * with {@link Status#SAME} status. This reduces report size and focuses attention on
 * actual differences between distributions.
 *
 * <h3>Processing Logic</h3>
 * <ol>
 *   <li><b>Artifact Filtering</b>: Removes SAME artifacts from results
 *     <ul>
 *       <li>Iterates through all artifacts in results</li>
 *       <li>Identifies artifacts with Status.SAME</li>
 *       <li>Removes these artifacts from the results list</li>
 *     </ul>
 *   </li>
 *   <li><b>Hierarchical Filtering</b>: Handles nested artifacts
 *     <ul>
 *       <li>For artifacts with children (e.g., JARs containing classes)</li>
 *       <li>Recursively removes SAME child artifacts</li>
 *       <li>Preserves hierarchy for changed items</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h3>Removed Statuses</h3>
 * <p>Only removes artifacts with:
 * <ul>
 *   <li>{@link Status#SAME} - Files that are identical in both distributions</li>
 * </ul>
 *
 * <h3>Preserved Statuses</h3>
 * <p>Keeps all artifacts with these statuses:
 * <ul>
 *   <li>{@link Status#ADDED} - New files</li>
 *   <li>{@link Status#REMOVED} - Deleted files</li>
 *   <li>{@link Status#DIFFERENT} - Changed files</li>
 *   <li>{@link Status#DIFFERENT_LINE_BREAKS} - Line ending differences</li>
 *   <li>{@link Status#VERSION} - Version changes</li>
 *   <li>{@link Status#BUILD} - Build changes</li>
 *   <li>{@link Status#EXPECTED_DIFFERENCES} - Expected changes</li>
 *   <li>{@link Status#PATCHED} - Patched files</li>
 *   <li>{@link Status#PATCHED_WRONG} - Incorrectly patched files</li>
 *   <li>{@link Status#PATCHED_UNNECESSARILY} - Unnecessarily patched files</li>
 *   <li>{@link Status#NOT_PATCHED} - Files that should be patched but aren't</li>
 *   <li>{@link Status#ERROR} - Files with processing errors</li>
 * </ul>
 *
 * <h3>Configuration Impact</h3>
 * <ul>
 *   <li><code>-i / --ignore-same-items</code>: Must be enabled for this phase to run</li>
 *   <li>When enabled, significantly reduces report size</li>
 *   <li>Makes reports more focused on actionable differences</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * <ul>
 *   <li>Must run LATE in the phase pipeline (after all status determinations)</li>
 *   <li>Should run BEFORE SortingPhase and ReportingPhase</li>
 *   <li>Requires all comparison phases to have completed first</li>
 * </ul>
 *
 * <h3>Use Cases</h3>
 * <ul>
 *   <li>Focus on changes only when comparing large distributions</li>
 *   <li>Reduce report size for faster analysis</li>
 *   <li>Highlight problematic differences without noise from unchanged files</li>
 *   <li>Generate concise summaries for stakeholders</li>
 * </ul>
 *
 * <h3>Important Notes</h3>
 * <ul>
 *   <li>This phase permanently removes artifacts from results</li>
 *   <li>Cannot be undone - ensure all other phases have completed</li>
 *   <li>Status history for removed artifacts is lost</li>
 *   <li>Use with caution when complete file inventory is needed</li>
 * </ul>
 *
 * @see ProcessPhase
 * @see Status
 */
public class ShowOnlyChangedItemsPhase extends ProcessPhase {

    private static final Logger LOGGER = Logger.getLogger(ShowOnlyChangedItemsPhase.class.getName());


    /**
     * @see ProcessPhase#process()
     */
    @Override
    public void process() {

        List<Artifact> removed = new LinkedList<>();
        for (Artifact artifact : results.getArtifacts()) {
            if (artifact instanceof ItemWithChild) {
                removeSubArtifacts((ItemWithChild) artifact);
            }
            if (Status.SAME.equals(artifact.getStatus())) {
                removed.add(artifact);
            }
        }
        results.getArtifacts().removeAll(removed);
    }

    /**
     * Removes all same child
     *
     * @param artifact target artifact
     */
    private void removeSubArtifacts(ItemWithChild artifact) {
        List<Artifact> removed = new LinkedList<>();
        if (artifact.getItems() != null) {
            for (Artifact a : artifact.getItems()) {
                if (a instanceof ItemWithChild) {
                    removeSubArtifacts((ItemWithChild) a);
                }
                if (Status.SAME.equals(a.getStatus())) {
                    removed.add(a);
                }
            }
            artifact.getItems().removeAll(removed);
        }
    }
}
