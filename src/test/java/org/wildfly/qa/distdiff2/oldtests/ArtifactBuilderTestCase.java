package org.wildfly.qa.distdiff2.oldtests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;

import org.wildfly.qa.distdiff2.Platform;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.FileArtifact;
import org.wildfly.qa.distdiff2.artifacts.FolderArtifact;
import org.wildfly.qa.distdiff2.tools.ArtifactBuilder;
import org.wildfly.qa.distdiff2.tools.Tools;
import org.junit.Test;

/**
 * Test for {@link ArtifactBuilder}
 */
public class ArtifactBuilderTestCase {

    @Test
    public void testCreateBasicScenario() throws Exception {
        File sourceFile = File.createTempFile("test", ".xml");
        FileWriter fileWriter = new FileWriter(sourceFile, true);
        fileWriter.append("<xml>blabla</xml>").flush();
        fileWriter.close();
        assertNotNull(sourceFile);
        assertNotNull(sourceFile.getParent());

        Artifact artifact = ArtifactBuilder.create(sourceFile, sourceFile.getParentFile(), 0, true);
        assertNotNull(artifact);
        assertTrue(artifact instanceof FileArtifact);
        assertEquals(sourceFile.getName(), artifact.getName());
        assertEquals(sourceFile.getAbsolutePath(), artifact.getPathA());
        if (Platform.isHP()) {
            // well, this is weird...
            assertEquals("text/plain", ((FileArtifact) artifact).getMimeType());
        } else if (Platform.isWindows() || (Tools.getMajorJavaVersion() >= 9 && !Platform.isMac())) {
            assertEquals("text/xml", ((FileArtifact) artifact).getMimeType());
        } else {
            assertEquals("application/xml", ((FileArtifact) artifact).getMimeType());
        }

        Artifact folder = ArtifactBuilder.create(sourceFile.getParentFile(), sourceFile.getParentFile(), 0, true);
        assertNotNull(folder);
        assertTrue(folder instanceof FolderArtifact);
        assertEquals(sourceFile.getParentFile().getName(), folder.getName());
        assertEquals(sourceFile.getParentFile().getAbsolutePath(), folder.getPathA());
    }

}
