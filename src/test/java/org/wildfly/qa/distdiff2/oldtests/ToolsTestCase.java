package org.wildfly.qa.distdiff2.oldtests;

import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.wildfly.qa.distdiff2.Platform;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.FileArtifact;
import org.wildfly.qa.distdiff2.artifacts.FolderArtifact;
import org.wildfly.qa.distdiff2.artifacts.JarArtifact;
import org.wildfly.qa.distdiff2.results.Results;
import org.wildfly.qa.distdiff2.results.Status;
import org.wildfly.qa.distdiff2.tools.Tools;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link Tools}
 */
public class ToolsTestCase {

    @Test
    public void testGenerateXml() throws Exception {
        List<Artifact> artifacts = new LinkedList<>();
        artifacts.add(new FolderArtifact("folder1", "/folder1"));
        artifacts.add(new FolderArtifact("folder2", "/folder2"));
        artifacts.add(new FileArtifact("file1", "/folder2/file1", 1024));

        Results results = new Results();
        results.setFolderA("folderA-test");
        results.setFolderB("folderB-test");
        results.setArtifacts(artifacts);

        StringWriter stringWriter = new StringWriter();
        Tools.generateXml(results, stringWriter);

        String xml = stringWriter.toString();
        assertNotNull(xml);
        assertTrue(xml.contains("name=\"folder1\""));
        assertTrue(xml.contains("name=\"folder2\""));
        assertTrue(xml.contains("name=\"file1\""));
    }

    @Test
    public void testXslt() throws Exception {
        List<Artifact> artifacts = new LinkedList<>();
        artifacts.add(new FolderArtifact("folder1", "/folder1"));
        artifacts.add(new FolderArtifact("folder2", "/folder2"));
        artifacts.add(new FileArtifact("file1", "/folder2/file1", 1024));

        Results results = new Results();
        results.setFolderA("folderA-test");
        results.setFolderB("folderB-test");
        results.setArtifacts(artifacts);
        StringWriter stringWriter = new StringWriter();
        Tools.generateXml(results, stringWriter);

        String xml = stringWriter.toString();

        StringWriter output = new StringWriter();
        StreamSource source = new StreamSource(new StringReader(xml));
        StreamSource xslTemplate = new StreamSource(this.getClass().getResourceAsStream("/xml-to-html.xsl"));
        Tools.xslt(source, xslTemplate, new StreamResult(output));

        String content = output.toString();
        assertNotNull(content);
        assertTrue(content.contains("/folder2/file1"));
    }

    @Test
    public void testCalculateMD5() throws Exception {
        File tmpFile = File.createTempFile("test", null);
        FileWriter fw = new FileWriter(tmpFile);
        fw.write("Test");
        fw.close();
        String md5 = Tools.calculateMD5(tmpFile.getAbsolutePath());
        assertNotNull(md5);
        assertEquals("cbc6611f554bd0809a388dc95a615b", md5);
    }

    @Test
    public void testWalkThrowList() {
        List<Artifact> listA = new LinkedList<>();
        listA.add(new FileArtifact("name1", "name1", 0));
        listA.add(new FileArtifact("name2", "name2", 0));
        listA.add(new FileArtifact("name3", "name3", 0));

        List<Artifact> listB = new LinkedList<>();
        listB.add(new FileArtifact("name2", "name2", 0));
        listB.add(new FileArtifact("name3", "name3", 0));
        listB.add(new FileArtifact("name4", "name4", 0));

        List<Artifact> result = Tools.walkThroughList(listA, listB, Status.REMOVED);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(new FileArtifact("name1", "name1", 0)));
    }

    @Test
    public void testReadFile() throws Exception {
        String[] testContent = new String[]{"Test", "another test\n",
                "one more test\nand here is another line\nnewline", "one line\nsecond line\n"};

        File tmpFile = File.createTempFile("test", null);
        tmpFile.deleteOnExit();
        for (String testString : testContent) {
            FileWriter fw = new FileWriter(tmpFile);
            fw.write(testString);
            fw.close();

            String content = Tools.readFile(tmpFile.getAbsolutePath());
            assertNotNull(content);
            assertEquals(testString, content);
        }
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("zip", Tools.getExtension("test.zip"));
        assertEquals("class", Tools.getExtension("test.class"));
        assertEquals("gz", Tools.getExtension("test.tar.gz"));
    }

    @Test
    public void testRemoveExtension() {
        assertNull(Tools.removeExtension(null));
        assertNull(Tools.removeExtension(""));
        assertNull(Tools.removeExtension(File.separator));
        assertNull(Tools.removeExtension(File.separator + File.separator));
        if (Platform.isWindows()) {
            assertEquals("a", Tools.removeExtension("C:\\a"));
            assertEquals("a", Tools.removeExtension("/a"));
        } else {
            assertEquals("a", Tools.removeExtension(File.separator + "a"));
        }
        assertEquals("c", Tools.removeExtension("a" + File.separator + "b" + File.separator + "c"));
        assertEquals("c", Tools.removeExtension("a" + File.separator + "b" + File.separator + "c.txt"));
        assertEquals("c.txt",
                Tools.removeExtension("a" + File.separator + "b" + File.separator + "c.txt.jpg"));
        assertEquals("c.txt", Tools.removeExtension("c.txt.jpg"));
    }

    @Test
    public void testGetBuildInformation() {
        assertNull(Tools.getBuildInformation(null));
        assertNull(Tools.getBuildInformation(""));
        assertNull(Tools.getBuildInformation(File.separator));

        assertEquals("test",
                Tools.getBuildInformation(File.separator + "xx" + File.separator + "test.jar").getName());
        assertEquals("test.jar", Tools.getBuildInformation("/test.jar.jar").getName());

        JarArtifact.BuildInformation information;
        information = Tools.getBuildInformation("/test-second.jar");
        assertEquals("test-second", information.getName());

        information = Tools.getBuildInformation("/test-1.jar");
        assertEquals("test", information.getName());
        assertEquals("1", information.getMajorVersion());

        information = Tools.getBuildInformation("/test-second-1.jar");
        assertEquals("test-second", information.getName());
        assertEquals("1", information.getMajorVersion());

        information = Tools.getBuildInformation("jgroups-3.2.10.Final-redhat-2.jar");
        assertEquals("jgroups", information.getName());
        assertEquals("3", information.getMajorVersion());
        assertEquals("2", information.getMinorVersion());
        assertEquals("10", information.getMicroVersion());
        assertEquals("Final", information.getSuffix());
        assertEquals("redhat-2", information.getBuild());

        information = Tools.getBuildInformation("opensaml-2.5.1-redhat-1.jar");
        assertEquals("opensaml", information.getName());
        assertEquals("2", information.getMajorVersion());
        assertEquals("5", information.getMinorVersion());
        assertEquals("1", information.getMicroVersion());
        assertEquals("redhat-1", information.getBuild());

        information = Tools.getBuildInformation("picketbox-infinispan-4.0.17.SP2-redhat-2.jar");
        assertEquals("picketbox-infinispan", information.getName());
        assertEquals("4", information.getMajorVersion());
        assertEquals("0", information.getMinorVersion());
        assertEquals("17", information.getMicroVersion());
        assertEquals("SP2", information.getSuffix());
        assertEquals("redhat-2", information.getBuild());
    }

    @Test
    public void testParseVersionInformation() {
        assertNull(Tools.parseVersionInformation(null, null));
        assertNull(Tools.parseVersionInformation("", new JarArtifact.BuildInformation("name")));

        JarArtifact.BuildInformation information = new JarArtifact.BuildInformation();

        String build = Tools.parseVersionInformation("3.2.10.Final.Build", information);
        assertEquals("3", information.getMajorVersion());
        assertEquals("2", information.getMinorVersion());
        assertEquals("10", information.getMicroVersion());
        assertEquals("Final", information.getSuffix());
        assertEquals("Build", build);
    }

    /**
     * Test for {@link Tools#convertRelativePathToModuleName(String, List)}.
     */
    @Test
    public void convertRelativePathToModuleName() {
        Map<String, String> testContent = new HashMap<>();
        testContent.put("modules/system/layers/base/org/jboss/as/connector/main/module.xml",
                "org.jboss.as.connector.main");
        testContent.put("modules/system/layers/base/.overlays/layer-base-patch-ER1-to-ER2/org/jboss/as/connector/main" +
                        "/module.xml",
                "org.jboss.as.connector.main");
        testContent.put("layers/base/.overlays/layer-base-patch-ER1-to-ER2/org/jboss/as/connector/main/module.xml",
                "org.jboss.as.connector.main");
        testContent.put("layers/base/org/jboss/as/connector/main/module.xml", "org.jboss.as.connector.main");
        testContent.put("modules/system/layers/someLayer/org/jboss/as/connector/main/module.xml",
                "org.jboss.as.connector.main");
        testContent.put("modules/system/layers/someLayer/.overlays/layer-base-patch-ER1-to-ER2/org/jboss/as/connector" +
                "/main/module.xml", "org.jboss.as.connector.main");
        testContent.put("modules/system/layers/anotherLayer/org/connector/main/module.xml", "org.connector.main");
        // negative scenarios
        testContent.put("modules/system/layers", null);
        testContent.put("modules/system/layers/anotherLayer", null);
        testContent.put("modules/system/layers/anotherLayer/.overlays", null);
        testContent.put("modules/system/layers/anotherLayer/.overlays/layer-base-patch", null);
        testContent.put("modules/system/layers/anotherLayer/.overlays/layer-base-patch/module.xml", null);
        testContent.put("modules/system/layers/anotherLayer/.overlays/layer-base-patch/org/module.xml", "org");

        LinkedList<String> layers = new LinkedList<>();
        layers.add("base");
        layers.add("someLayer");
        layers.add("anotherLayer");

        for (String testString : testContent.keySet()) {
            String result = Tools.convertRelativePathToModuleName(testString, layers);
            Assert.assertEquals("Converting '" + testString + "'", testContent.get(testString), result);
        }
    }
}
