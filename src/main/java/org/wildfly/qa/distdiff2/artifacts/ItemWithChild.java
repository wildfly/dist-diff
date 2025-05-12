package org.wildfly.qa.distdiff2.artifacts;

import org.wildfly.qa.distdiff2.results.Status;

import java.util.List;

/**
 * ItemWithChild
 * <p>
 * Interface for artifacts with child
 */
public interface ItemWithChild {

    List<Artifact> getItems();

    void setItems(List<Artifact> items);

    Status getStatus();
}
