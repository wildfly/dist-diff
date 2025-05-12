package org.wildfly.qa.distdiff2.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.sun.xml.bind.marshaller.CharacterEscapeHandler;
import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.FolderArtifact;
import org.wildfly.qa.distdiff2.artifacts.JarArtifact;
import org.wildfly.qa.distdiff2.patching.PatchingMechanismAwarenessPhase;
import org.wildfly.qa.distdiff2.results.Results;
import org.wildfly.qa.distdiff2.results.Status;

/**
 * Tools
 * <p>
 * Shared tools across dist-diff2
 */
public final class Tools {

    // Internal logger
    private static final Logger LOGGER = Logger.getLogger(Tools.class);

    /**
     * Generates XML file from the given object
     *
     * @param results instance of results
     * @param writer  target writer for generated XMLs
     * @throws JAXBException if something goes wrong
     */
    public static void generateXml(Results results, Writer writer) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Results.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "utf-8");
        marshaller.setProperty(CharacterEscapeHandler.class.getName(), new CharacterEscapeHandler() {
            public void escape(char[] cbuf, int offset, int length, boolean flag,
                               Writer writer) throws IOException {

                // Characters that are too low in the ASCII table (usually special/command characters) should be avoided
                // in XML output, except for #x9 (TAB) | #xA (LF) | #xD (CR), see https://www.w3.org/TR/xml/#charsets
                // for more info. We're gonna replace them by simple space.
                for (int i = 0; i < cbuf.length; i++) {
                    if (cbuf[i] < 32 && cbuf[i] != 9 && cbuf[i] != 10 && cbuf[i] != 13) {
                        cbuf[i] = 32;
                    }
                }

                writer.write(cbuf, offset, length);
            }
        });
        marshaller.marshal(results, writer);
    }

    /**
     * XSL transformation
     *
     * @param xmlSource source XML
     * @param template  XSLT template
     * @param result    target stream for output
     * @throws Exception if something foes wrong
     */
    public static void xslt(StreamSource xmlSource, StreamSource template, StreamResult result) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(template);
        transformer.transform(xmlSource, result);
    }

    /**
     * Reads all files in folder and creates representation
     *
     * @param sourceFolder  source folder with distribution
     * @param distributionA is distribution A?
     * @return List of created representations
     */
    public static List<Artifact> readAllItemsFromFolder(File sourceFolder, boolean distributionA) {
        return readAllItemsFromFolder(sourceFolder, sourceFolder, 0, distributionA);
    }

    /**
     * Internal recursive method for {@link Tools#readAllItemsFromFolder(java.io.File, boolean)}
     *
     * @param folder     actual folder
     * @param distFolder distribution folder
     * @param level      current level
     * @param distA      is distribution A?
     * @return List of created representations
     */
    private static List<Artifact> readAllItemsFromFolder(File folder, File distFolder, int level, boolean distA) {
        List<Artifact> files = new LinkedList<>();
        if (folder.isDirectory()) {
            File[] listOfFiles = folder.listFiles();
            if (listOfFiles != null) {
                for (File fileItem : listOfFiles) {
                    Artifact artifact = ArtifactBuilder.create(fileItem, distFolder, level, distA);
                    if (artifact != null) {
                        files.add(artifact);
                        if (artifact instanceof FolderArtifact) {
                            files.addAll(readAllItemsFromFolder(fileItem, distFolder, level + 1, distA));
                        }
                    }
                }
            }
        }
        return files;
    }

    /**
     * Calculates MD5 sum for file
     *
     * @param fileName target file
     * @return MD5 sum for file
     * @throws Exception if something goes wrong
     */
    public static String calculateMD5(String fileName) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        StringBuilder hexString = new StringBuilder();
        try (FileInputStream in = new FileInputStream(fileName)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }
            byte[] digest = md.digest();

            if (digest != null) {
                for (byte aDigest : digest) {
                    hexString.append(Integer.toHexString(0xFF & aDigest));
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        }
        return hexString.toString();
    }

    /**
     * Walks through a list and detects whether artifacts are present or missing in a second list
     *
     * @param source source list of artifacts
     * @param target target list of artifacts
     * @param status status of artifacts ({@link Status#ADDED} or
     * {@link Status#REMOVED})
     * @return list of missing or added artifacts
     */
    public static List<Artifact> walkThroughList(List<Artifact> source, List<Artifact> target, Status status) {
        List<Artifact> results = new LinkedList<>();
        if (source != null) {
            for (Artifact artifact : source) {
                if (!target.contains(artifact)) {
                    artifact.setStatus(status);
                    results.add(artifact);
                }
            }
            if (results.size() > 0) {
                source.removeAll(results);
            }
        }
        return results;
    }

    /**
     * Reads text file and returns it as String without losing EOLs.
     *
     * @param fileName target file name
     * @return String representation of a file content
     * @throws IOException is something goes wrong
     */
    public static String readFile(String fileName) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(fileName));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    /**
     * Returns extension of a file
     *
     * @param filename filename
     * @return calculated extension
     */
    public static String getExtension(final String filename) {
        if (filename == null) return null;
        final String afterLastSlash = filename.substring(filename.lastIndexOf('/') + 1);
        final String afterLastBackslash = afterLastSlash.substring(afterLastSlash.lastIndexOf('\\') + 1);
        final int dotIndex = afterLastBackslash.lastIndexOf('.');
        return (dotIndex == -1) ? "" : afterLastBackslash.substring(dotIndex + 1);
    }

    /**
     * Reads Manifest information from the given jar file
     *
     * @param fileName source jar file
     * @return instance of class {@link Manifest}
     */
    public static Manifest readManifestFromJar(final String fileName) {
        Manifest manifest = null;
        JarInputStream jarStream = null;
        try {
            jarStream = new JarInputStream(new FileInputStream(fileName));
            manifest = jarStream.getManifest();
            if (manifest == null) {
                // damn JDK can't find the MANIFEST.MF if it's not the first entry in META-INF directory...
                final URLClassLoader loader = new URLClassLoader(new URL[]{new URL("file:" + fileName)}, null);
                final InputStream resourceAsStream = loader.getResourceAsStream("META-INF/MANIFEST.MF");
                if (resourceAsStream != null) {
                    manifest = new Manifest(resourceAsStream);
                } else {
                    LOGGER.warn("Unable to read MANIFEST.MF from " + fileName);
                }
                loader.close();
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            if (jarStream != null) {
                try {
                    jarStream.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
        return manifest;
    }

    /**
     * Reads more detailed information about jar files and sets data into jar artifact
     *
     * @param jarArtifact instance of jar artifact
     */
    public static void parseJarVersionInformation(JarArtifact jarArtifact) {
        if (jarArtifact != null) {
            jarArtifact.setBuildInformationA(Tools.getBuildInformation(jarArtifact.getPathA()));
            if (jarArtifact.getBuildInformationA() != null) {
                jarArtifact.getBuildInformationA().setFullName(jarArtifact.getName());
            }
            jarArtifact.setBuildInformationB(Tools.getBuildInformation(jarArtifact.getPathB()));
            if (jarArtifact.getBuildInformationB() != null) {
                jarArtifact.getBuildInformationB().setFullName(jarArtifact.getName());
            }
        }
    }

    /**
     * Removes extension from filename
     *
     * @param filePath filename
     * @return filename without extension
     */
    public static String removeExtension(String filePath) {
        String filename = null;
        if (filePath != null && filePath.trim().length() > 0) {
            filename = new File(filePath).getName();
            // Remove the extension.
            int extensionIndex = filename.lastIndexOf(".");
            if (extensionIndex != -1) {
                filename = filename.substring(0, extensionIndex).trim();
            }
            filename = (filename.length() > 0) ? filename : null;
        }
        return filename;
    }

    /**
     * Parses detailed information about version from file name
     * <p>
     * Looks for schema xx-yy-Major-Minor-Micro(.|-)(Final|SP)-*-number
     *
     * @param path full or relative path of jar
     * @return version information
     */
    public static JarArtifact.BuildInformation getBuildInformation(String path) {
        final String FILE_NAME_ELEMENTS_SEPARATOR = "-";
        JarArtifact.BuildInformation buildInformation = null;
        if (path != null && path.trim().length() > 0) {
            String name = Tools.removeExtension(path);
            if (name != null) {
                int indexOf = name.lastIndexOf(FILE_NAME_ELEMENTS_SEPARATOR);
                if (indexOf > 0) {
                    String[] segments = name.split(FILE_NAME_ELEMENTS_SEPARATOR);
                    buildInformation = new JarArtifact.BuildInformation();
                    // Let's get name of artifact
                    StringBuilder sb = new StringBuilder();
                    int actualSegment = 0;
                    boolean stillName = true;
                    while (actualSegment < segments.length && stillName) {
                        if (sb.length() > 0) {
                            sb.append(FILE_NAME_ELEMENTS_SEPARATOR);
                        }
                        sb.append(segments[actualSegment]);
                        if (actualSegment < segments.length - 1) {
                            String startNextSegment = segments[actualSegment + 1];
                            if (startNextSegment != null && startNextSegment.length() > 0) {
                                startNextSegment = startNextSegment.substring(0, 1);
                                stillName = startNextSegment.matches("\\D");
                            }
                        }
                        actualSegment++;
                    }
                    buildInformation.setName(sb.toString());
                    // Let's parse version information
                    StringBuilder sbBuild = new StringBuilder();
                    if (actualSegment < segments.length) {
                        String versionStr = segments[actualSegment];
                        String restBuildInfo = parseVersionInformation(versionStr, buildInformation);
                        if (restBuildInfo != null) {
                            sbBuild.append(restBuildInfo);
                        }
                        actualSegment++;
                    }
                    // Rest should be build information
                    while (actualSegment < segments.length) {
                        if (sbBuild.length() > 0) {
                            sbBuild.append(FILE_NAME_ELEMENTS_SEPARATOR);
                        }
                        sbBuild.append(segments[actualSegment++]);
                    }
                    buildInformation.setBuild(sbBuild.toString());
                } else if (!name.equals(File.separator)) {
                    buildInformation = new JarArtifact.BuildInformation(name);
                }
            }
        }
        return buildInformation;
    }

    /**
     * Parses version information from string and set data into instance of build information
     *
     * @param version     Version information in <code>String</code>
     * @param information instance of build information
     * @return rest of version information -&gt; should be build information
     */
    public static String parseVersionInformation(String version, JarArtifact.BuildInformation information) {
        final String VERSION_SEPARATOR = "\\.";
        StringBuilder sb = new StringBuilder();
        if (version != null) {
            String[] segments = version.split(VERSION_SEPARATOR);
            if (segments.length == 0) {
                information.setMajorVersion(version);
            }
            if (segments.length > 0) {
                information.setMajorVersion(segments[0]);
            }
            if (segments.length > 1) {
                information.setMinorVersion(segments[1]);
            }
            if (segments.length > 2) {
                information.setMicroVersion(segments[2]);
            }
            if (segments.length > 3) {
                information.setSuffix(segments[3]);
            }
            if (segments.length > 4) {
                for (int index = 4; index < segments.length; index++) {
                    if (sb.length() > 0) {
                        sb.append(VERSION_SEPARATOR);
                    }
                    sb.append(segments[index]);
                }
            }
        }
        return (sb.length() > 0) ? sb.toString() : null;
    }

    public static Integer calculateHashOfDirectory(File dir) throws Exception {
        if (dir == null) {
            throw new IllegalArgumentException("Cannot calculate hash of null");
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(dir.getAbsolutePath() + " is not a directory");
        }
        Integer result = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    int hash = calculateHashOfDirectory(file);
                    LOGGER.trace("Appending directory " + file.getAbsolutePath() + ", hash=" + hash);
                    result = result + hash;
                } else {
                    int hash = calculateMD5(file.getAbsolutePath()).hashCode();
                    LOGGER.trace("Appending file " + file.getAbsolutePath() + ", hash=" + hash);
                    result = result + hash;
                }
            }
        }
        return result;
    }

    public static File findJarInDirectory(String namePrefix, File directory) {
        LOGGER.debug("Searching for jar with prefix " + namePrefix + " in " + directory);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().startsWith(namePrefix) && f.getName().endsWith(".jar")) {
                    LOGGER.debug("Found: " + f.getAbsolutePath());
                    return f;
                }
            }
        }
        return null;
    }

    public static Long byteArrayToInteger(byte[] array) {
        long result = 0;
        for (int i = array.length - 1; i > -1; i--) {
            result = result * 256 + array[i];
        }
        return result;
    }

    public static String getStackTrace(Exception e) {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter(sWriter);
        e.printStackTrace(pWriter);
        return sWriter.toString();
    }

    /**
     * Stolen from http://stackoverflow.com/questions/620993/determining-binary-text-file-type-in-java
     */
    public static boolean isTextFile(String filePath) throws IOException {
        File f = new File(filePath);
        if (!f.exists() || !f.isFile()) {
            return false;
        }

        byte[] data;
        try (FileInputStream in = new FileInputStream(f)) {
            int size = in.available();
            if (size > 1000) {
                size = 1000;
            }
            data = new byte[size];
            in.read(data);
        }

        String s = new String(data, StandardCharsets.UTF_8);
        String s2 = s.replaceAll(
                "[a-zA-Z0-9ßöäü\\.\\*!\"§\\$\\%&/()=\\?@~'#:,;\\" +
                        "+><\\|\\[\\]\\{\\}\\^°²³\\\\ \\n\\r\\t_\\-`´âêîô" +
                        "ÂÊÔÎáéíóàèìòÁÉÍÓÀÈÌÒ©‰¢£¥€±¿»«¼½¾™ª]", ""
        );
        // will delete all text signs

        double d = (double) (s.length() - s2.length()) / (double) (s.length());
        // percentage of text signs in the text
        return d > 0.95;
    }

    /**
     * Simply converts all available data in given {@link InputStream} into the string. It uses given charset for
     * decoding.
     *
     * @param inputStream input stream which data will be converted to string
     * @param charset     target encoding
     * @return string from data in input stream
     * @throws IOException in case of IO error
     */
    public static String inputStreamToString(InputStream inputStream, Charset charset) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            byteStream.write(buffer, 0, length);
        }
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }
        String result = byteStream.toString(charset.displayName());
        byteStream.close();

        return result;
    }

    public static LineBreakStyle detectLineBreakStyle(String filePath) throws IOException {
        if (!isTextFile(filePath)) {
            throw new IllegalArgumentException(filePath + " is a binary file, I can't detect line breaks");
        }
        File f = new File(filePath);
        if (!f.exists()) {
            throw new FileNotFoundException(filePath);
        }
        FileInputStream in = new FileInputStream(f);
        int size = in.available();
        if (size > 1000) {
            size = 1000;
        }
        byte[] data = new byte[size];
        in.read(data);
        in.close();
        String s = new String(data, StandardCharsets.UTF_8);
        if (s.contains("\r\n")) {
            return LineBreakStyle.WINDOWS;
        } else {
            return LineBreakStyle.UNIX;
        }
    }


    /**
     * @return major version of currently used JDK based on 'java.version' value.
     */
    public static int getMajorJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        String[] parsedJavaVersion = javaVersion.split("\\.");
        String result;

        if (parsedJavaVersion.length == 0) {
            // Seems like JDK10+ as no dot is present in the 'java.version'
            result = javaVersion;
        } else {
            // Use first part of the version by default
            result = parsedJavaVersion[0];

            if ("1".equals(result)) {
                // Okay, seems, like we have 1.x.y -> let's use second part then
                result = parsedJavaVersion[1];
            }
        }

        // In case we run with JDK early release, remove '-ea' suffix.
        if (result.contains("-ea")) {
            result = result.replace("-ea", "");
        }

        return Integer.parseInt(result);
    }

    /**
     * Converts relative path of 'module.xml' artifact into the module name.<br>
     * Example:<br>
     * <p>
     * input: modules/system/layers/base/.overlays/layer-base-patch-ER1-to-ER2/org/jboss/as/connector/main/module
     * .xml<br>
     * output: org.jboss.as.connector.main
     *
     * @param path   relative path to the 'module.xml' artifact in the distribution
     * @param layers list of layers that are present in the distribution
     * @return module name in the form of package name
     */
    public static String convertRelativePathToModuleName(String path, List<String> layers) {
        String[] elements = path.split("/");
        List<String> list = new LinkedList<>(Arrays.asList(elements));

        // Trim current list so we get span right after the 'layers' directory just before the 'module.xml' file.
        if (!list.isEmpty() && list.contains("layers") && list.indexOf("layers") + 2 < list.size()
                && list.indexOf("module.xml") == list.size() - 1) {
            list = list.subList(list.indexOf("layers") + 1, list.size() - 1);
        } else {
            return null;
        }

        // Check that layer is present now.
        if (layers.contains(list.get(0))) {
            list = list.subList(1, list.size());
        } else {
            return null;
        }

        // In case of '.overlays', we need to get rid of it and it's subfolder.
        if (PatchingMechanismAwarenessPhase.OVERLAYS.equals(list.get(0))) {
            if (list.size() > 2) {
                list = list.subList(2, list.size());
            } else {
                return null;
            }
        }

        // Now we are clean for conversion.
        return String.join(".", list);
    }
}
