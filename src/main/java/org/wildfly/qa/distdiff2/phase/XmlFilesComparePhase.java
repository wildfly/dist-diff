package org.wildfly.qa.distdiff2.phase;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.FileArtifact;
import org.wildfly.qa.distdiff2.results.Status;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

/**
 * XmlFilesComparePhase
 * <p>
 * Implementation of {@link ProcessPhase}, compares two XML files at DOM level.
 * White spaces between elements are ignored.
 * Includes items which are {@link FileArtifact} with {@link Status#DIFFERENT} status and mime type is
 * <code>application/xml</code>
 */
public class XmlFilesComparePhase extends ProcessPhase {

    private static final Logger LOGGER = Logger.getLogger(XmlFilesComparePhase.class.getName());

    /**
     * @see ProcessPhase#process()
     */
    @Override
    public void process() {
        if (results == null || results.getArtifacts() == null) {
            LOGGER.warn("Xml files compare phase was called with null or empty list!");
            return;
        }
        for (Artifact artifact : results.getArtifacts()) {
            if (artifact instanceof FileArtifact && Status.DIFFERENT.equals(artifact.getStatus())
                    && isXml((FileArtifact) artifact)) {
                processArtifact((FileArtifact) artifact);
            }
        }
    }

    /**
     * Processes given artifacts and set corresponding status
     *
     * @param artifact target artifact in distributions
     */
    private void processArtifact(FileArtifact artifact) {
        boolean result;
        try {
            result = isXmlSame(artifact, context.getConfiguration().isCompareXmlLeniently());
        } catch (Exception e) {
            LOGGER.warn("Cannot compare document " + artifact + ":" + e.getMessage(), e);
            result = false;
        }
        if (result) {
            artifact.setStatus(Status.SAME);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("XML File " + artifact + " is same at XML level");
            }
        }
    }

    /**
     * Checks whether given {@link FileArtifact} is XML file or not. There is necessary to check two different types of
     * mime texts as 'application/xml' has been used up to JDK-8. Although, since JDK-9, the 'text/xml' is used
     * instead.
     *
     * @param artifact artifact to be checked
     * @return true if given artifact is XML, false otherwise
     */
    private boolean isXml(FileArtifact artifact) {
        String mimeType = artifact.getMimeType();

        return "application/xml".equalsIgnoreCase(mimeType) || "text/xml".equalsIgnoreCase(mimeType);
    }

    /**
     * Compares two XML files at DOM level. There can be specified also whether the comparison should be lenient or not.
     *
     * @param file    target artifact for comparison
     * @param lenient whether to perform lenient comparison - files that have different order of some elements,
     *                although meaning and values are same, are considered to be identical (similar). If set to
     *                false, strict comparison is performed where also ordering of elements is taken into account
     *                during the comparison procedure.
     * @return <code>true</code> if files are same at DOM level, <code>false</code> otherwise
     * @throws IOException                  in case of any IO exception
     * @throws SAXException                 in case of any exception related to XML processing
     * @throws ParserConfigurationException in case of any exception related to parser configuration
     */
    private static boolean isXmlSame(FileArtifact file, boolean lenient) throws ParserConfigurationException,
            IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setCoalescing(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setIgnoringComments(true);
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document doc1 = db.parse(new File(file.getPathA()));
        doc1.normalizeDocument();

        Document doc2 = db.parse(new File(file.getPathB()));
        doc2.normalizeDocument();

        if (lenient) {
            // similar: the content of the nodes in the documents are the same, but minor differences exist e.g.
            // sequencing of sibling elements, values of namespace prefixes, use of implied attribute values
            Diff myDiffSimilar = DiffBuilder.compare(doc1).withTest(doc2)
                    .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
                    .checkForSimilar()
                    .build();
            return !myDiffSimilar.hasDifferences();
        } else {
            // identical: the content and sequence of the nodes in the documents are exactly the same
            Diff myDiffIdentical = DiffBuilder.compare(doc1).withTest(doc2)
                    .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
                    .checkForIdentical()
                    .build();
            return !myDiffIdentical.hasDifferences();
        }
    }
}
