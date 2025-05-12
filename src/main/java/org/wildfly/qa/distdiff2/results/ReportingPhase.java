package org.wildfly.qa.distdiff2.results;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.List;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.configuration.DistDiffConfiguration;
import org.wildfly.qa.distdiff2.phase.ProcessPhase;
import org.wildfly.qa.distdiff2.tools.Tools;

/**
 * @author Jan Martiska
 */
public class ReportingPhase extends ProcessPhase {

    private static final Logger LOGGER = Logger.getLogger(ReportingPhase.class.getName());

    @Override
    public void process() {
        DistDiffConfiguration distDiffConfiguration = context.getConfiguration();
        Results results = context.getResults();


        final String XML_FILE = distDiffConfiguration.getOutput() + File.separator + "dist-diff2-output.xml";
        final String TXT_FILE = distDiffConfiguration.getOutput() + File.separator + "dist-diff2-output.txt";
        final String HTML_FILE = distDiffConfiguration.getOutput() + File.separator + "dist-diff2-output.html";

        if (results == null) {
            return;
        }

        FileOutputStream xmlFileOutStream = null;
        FileOutputStream txtFileOutStream = null;
        try {
            if (!distDiffConfiguration.getOutput().exists()) {
                boolean success = distDiffConfiguration.getOutput().mkdirs();
                if (!success) {
                    LOGGER.error(String.format("Cannot create target directory! '%s'",
                            distDiffConfiguration.getOutput()));
                }
            }

            // Put number overview of basic artifact states.
            setArtifactsNumbers(results);

            // Generate XML report
            xmlFileOutStream = new FileOutputStream(XML_FILE);
            OutputStreamWriter xmlFileOutStreamWriter = new OutputStreamWriter(xmlFileOutStream, "utf-8");
            Tools.generateXml(results, xmlFileOutStreamWriter);
            xmlFileOutStreamWriter.close();
            xmlFileOutStream.close();
            xmlFileOutStream = null;

            // Generate HTML report
            StreamSource xmlFile = new StreamSource(new File(XML_FILE));
            StreamSource xslTemplate;
            if (distDiffConfiguration.isPatchingMechanismAware()) {
                xslTemplate = new StreamSource(
                        ReportingPhase.class.getResourceAsStream("/xml-to-html-patching.xsl"));
            } else {
                xslTemplate = new StreamSource(ReportingPhase.class.getResourceAsStream("/xml-to-html.xsl"));
            }
            StreamResult htmlFileWriter = new StreamResult(new File(HTML_FILE));
            Tools.xslt(xmlFile, xslTemplate, htmlFileWriter);

            // Generate TXT report
            xslTemplate = new StreamSource(ReportingPhase.class.getResourceAsStream("/xml-to-txt.xsl"));
            StreamResult txtOutput = new StreamResult(new StringWriter());
            Tools.xslt(xmlFile, xslTemplate, txtOutput);

            // Big report is not expected ...
            String txtContent = txtOutput.getWriter().toString();
            System.out.println(txtContent);
            txtFileOutStream = new FileOutputStream(TXT_FILE);
            OutputStreamWriter txtFileOutStreamWriter = new OutputStreamWriter(txtFileOutStream, "utf-8");
            txtFileOutStreamWriter.write(txtContent);
            txtFileOutStreamWriter.close();
            txtFileOutStream.close();
            txtFileOutStream = null;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            if (xmlFileOutStream != null) {
                try {
                    xmlFileOutStream.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
            if (txtFileOutStream != null) {
                try {
                    txtFileOutStream.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }

    }

    private void setArtifactsNumbers(Results results) {
        List<Artifact> artifacts = results.getArtifacts();

        int added = 0;
        int removed = 0;
        int different = 0;
        int same = 0;
        int others = 0;

        for (Artifact artifact : artifacts) {
            switch (artifact.getStatus()) {
                case ADDED:
                    added++;
                    break;
                case REMOVED:
                    removed++;
                    break;
                case DIFFERENT:
                    different++;
                    break;
                case SAME:
                    same++;
                    break;
                default:
                    others++;
            }
        }

        results.setArtifactsNumbers(added, removed, different, same, others, artifacts.size());
    }
}
