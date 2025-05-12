package org.wildfly.qa.distdiff2.sorting;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.phase.ProcessPhase;

/**
 * Sorts artifacts. There are 3 levels of sorting.
 * - status priority (more severe artifacts go first)
 * - level (in how many subdirectories the artifact is hidden)
 * - alphabetically
 *
 * @author Jan Martiska
 */
public class SortingPhase extends ProcessPhase {

    private static final Logger LOGGER = Logger.getLogger(SortingPhase.class.getName());

    @Override
    public void process() {
        if (results == null || results.getArtifacts() == null) {
            LOGGER.warn("Xml files compare phase was called with null or empty list!");
            return;
        }
        List<Artifact> artifacts = results.getArtifacts();
        long time = System.currentTimeMillis();
        Collections.sort(artifacts, new DefaultArtifactSortingComparator());
        long now = System.currentTimeMillis();
        LOGGER.info("Sorting " + artifacts.size() + " elements took " + (now - time) + " milliseconds");
    }

    static class DefaultArtifactSortingComparator implements Comparator<Artifact> {

        @Override
        public int compare(Artifact o1, Artifact o2) {
            int level1 = o2.getStatus().getPriority() - o1.getStatus().getPriority();
            if (level1 != 0) {
                return level1;
            } else {
                int result = Integer.compare(o1.getLevel(), o2.getLevel());
                if (result == 0) {
                    result = o1.getRelativePath().compareTo(o2.getRelativePath());
                }
                return result;
            }
        }
    }

}
