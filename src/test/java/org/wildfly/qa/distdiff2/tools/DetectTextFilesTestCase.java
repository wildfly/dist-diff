package org.wildfly.qa.distdiff2.tools;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Ability to detect whether a file is a text file or a binary file.
 * @author Jan Martiska
 */
public class DetectTextFilesTestCase {

    @Test
    public void doTextFile() throws IOException {
        final String path = ClassLoader.getSystemResource("fileanalysis/textorbinary/text-file").getPath();
        Assert.assertTrue(Tools.isTextFile(path));
    }

    @Test
    public void doBinaryFile() throws IOException {
        final String path = ClassLoader.getSystemResource("fileanalysis/textorbinary/not-a-text-file.png").getPath();
        Assert.assertFalse(Tools.isTextFile(path));
    }

}
