package org.wildfly.qa.distdiff2.phase;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.ItemWithChild;
import org.wildfly.qa.distdiff2.results.Status;

/**
 * RemoveSameItemsPhase
 * <p>
 * Implementation of {@link ProcessPhase}, removes same items from the results
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
