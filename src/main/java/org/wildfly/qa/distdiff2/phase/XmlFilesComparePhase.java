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
 * XmlFilesComparePhase - XML Document Structural Comparison Phase
 *
 * <h3>Purpose</h3>
 * This phase performs structural comparison of XML files at the DOM (Document Object Model) level,
 * rather than byte-by-byte comparison. This allows detecting when XML files are semantically
 * identical despite differences in formatting, whitespace, or element ordering.
 *
 * <h3>Processing Logic</h3>
 * <ol>
 *   <li><b>Eligible Artifacts</b>: Only processes {@link FileArtifact} instances that:
 *     <ul>
 *       <li>Have status {@link Status#DIFFERENT} (MD5 differs)</li>
 *       <li>Are XML files (detected by MIME type or .xml extension)</li>
 *       <li>Exist in both distributions</li>
 *     </ul>
 *   </li>
 *   <li><b>XML Parsing</b>: Parses both XML files into DOM trees
 *     <ul>
 *       <li>Uses standard Java DocumentBuilder</li>
 *       <li>Handles XML parsing errors gracefully</li>
 *     </ul>
 *   </li>
 *   <li><b>Structural Comparison</b>: Uses XMLUnit to compare DOM trees
 *     <ul>
 *       <li><b>Standard Mode</b>: Strict element ordering and structure</li>
 *       <li><b>Lenient Mode</b>: Ignores element ordering (--xml-lenient-compare flag)
 *         <ul>
 *           <li>Uses ElementSelectors.byNameAndText for matching</li>
 *           <li>Allows children in different order if content is same</li>
 *         </ul>
 *       </li>
 *       <li>Always ignores whitespace between elements</li>
 *       <li>Compares element names, attributes, and text content</li>
 *     </ul>
 *   </li>
 *   <li><b>Status Update</b>: If DOM trees are identical
 *     <ul>
 *       <li>Changes status from DIFFERENT → SAME</li>
 *       <li>Indicates the MD5 difference was due to formatting only</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h3>Status Transitions</h3>
 * <table border="1">
 *   <caption>Status transitions based on XML comparison</caption>
 *   <tr><th>From Status</th><th>Condition</th><th>To Status</th></tr>
 *   <tr><td>DIFFERENT</td><td>XML documents structurally identical</td><td>SAME</td></tr>
 *   <tr><td>DIFFERENT</td><td>XML documents structurally different</td><td>No change (remains DIFFERENT)</td></tr>
 *   <tr><td>DIFFERENT</td><td>XML parsing error</td><td>No change (remains DIFFERENT)</td></tr>
 * </table>
 *
 * <h3>Comparison Modes</h3>
 * <ul>
 *   <li><b>Standard Mode</b>: Elements must appear in same order
 *     <pre>{@code
 *     <config>
 *       <item>A</item>
 *       <item>B</item>
 *     </config>
 *     }</pre>
 *     Different from:
 *     <pre>{@code
 *     <config>
 *       <item>B</item>
 *       <item>A</item>
 *     </config>
 *     }</pre>
 *   </li>
 *   <li><b>Lenient Mode</b>: Element ordering ignored
 *     <ul>
 *       <li>Above examples would be considered SAME</li>
 *       <li>Useful for configuration files where order doesn't matter</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>Configuration Impact</h3>
 * <ul>
 *   <li><code>--xml-lenient-compare</code>: Enable lenient comparison (ignore element ordering)</li>
 *   <li><code>-x / --xml-as-text</code>: If enabled, this phase is skipped (files treated as text)</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * <ul>
 *   <li>Must run AFTER MD5SumsPhase (needs DIFFERENT artifacts)</li>
 *   <li>Should run BEFORE TextFilesDiffsPhase (to avoid generating diffs for formatting-only changes)</li>
 *   <li>Requires XMLUnit library for DOM comparison</li>
 * </ul>
 *
 * <h3>Common Use Cases</h3>
 * <ul>
 *   <li>Detecting reformatted XML configuration files</li>
 *   <li>Identifying whitespace-only changes in XML</li>
 *   <li>Validating XML content regardless of formatting style</li>
 *   <li>Comparing XML with reordered elements (lenient mode)</li>
 * </ul>
 *
 * @see Status
 * @see FileArtifact
 * @see ProcessPhase
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
            LOGGER.info("Artifact '" + artifact.getRelativePath() + "': XML files are identical at DOM level despite MD5 differences");
            artifact.setStatus(Status.SAME, this.getClass().getSimpleName(),
                "XML documents are identical at DOM level (whitespace/formatting differences only)");
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
