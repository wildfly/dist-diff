package org.wildfly.qa.distdiff2.patching.hashing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.tools.Tools;

class ModuleDiffUtils implements XMLStreamConstants {

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    private static final Logger LOGGER = Logger.getLogger(ModuleDiffUtils.class.getName());

    /**
     * Process a module.
     * <p>
     * This will go parse the module.xml and record all attributes and elements, except resources, which are processed
     * after separately to ignore different file system paths.
     *
     * @param root the module root
     * @return the comparison hash for the module
     * @throws IOException
     */
    public static byte[] processModule(final File root) throws Exception {


        final File moduleXml = new File(root, "module.xml");
        if (!moduleXml.isFile()) {
            throw new IOException("not a module" + root.getAbsolutePath());
        }

        final Set<String> resources = new LinkedHashSet<>();
        final MessageDigest moduleDigest = MessageDigest.getInstance("SHA1");

        // Process the module.xml
        try (InputStream stream = new FileInputStream(moduleXml)) {
            final XMLInputFactory inputFactory = INPUT_FACTORY;
            setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            final XMLStreamReader reader = inputFactory.createXMLStreamReader(stream);
            processRoot(reader, moduleDigest, resources);
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }

        // Process resource paths
        for (final String path : resources) {
            final File resource = new File(root, path);
            if (!resource.exists()) {
                LOGGER.warn("Resource " + resource.getAbsolutePath() + " referenced from " + moduleXml.getAbsolutePath() + " doesn't exist -> ignoring it for the computation of module's digest");
                continue;
            }
            if (path.endsWith(".jar")) {
                try {
                    LOGGER.trace("**** Appending jar file " + path);
                    ImprovedHashingUtils.internalJarComparison(resource, moduleDigest, false);
                } catch (Exception e) {
                    throw new IOException("failed to process " + resource.getAbsolutePath(), e);
                }
            } else {
                if (!path.equals(".")) {
                    LOGGER.trace("**** Appending resource file " + path);
                    moduleDigest.update(ImprovedHashingUtils.calculateHash(resource));
                }
            }
        }

        // Process native libs
        final File lib = new File(root, "lib");
        if (lib.exists()) {
            LOGGER.trace("**** Appending lib directory");
            moduleDigest.update(ImprovedHashingUtils.calculateHash(lib));
        }

        byte[] digest = moduleDigest.digest();
        LOGGER.debug("** Computed digest of module " + root.getAbsolutePath() + " as " + Tools.byteArrayToInteger(digest));
        return digest;
    }

    protected static void processRoot(final XMLStreamReader reader, final MessageDigest digest,
                                      final Set<String> resources) throws XMLStreamException {

        reader.require(START_DOCUMENT, null, null);
        reader.nextTag();
        reader.require(START_ELEMENT, null, null);

        final String namespace = reader.getNamespaceURI();
        digest.update(namespace.getBytes());
        processAttributes(reader, digest);
        processXml(reader, digest, resources);
        while (reader.next() != END_DOCUMENT) {
        }
    }

    protected static void processXml(final XMLStreamReader reader, final MessageDigest digest,
                                     final Set<String> resources) throws XMLStreamException {
        processAttributes(reader, digest);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String localName = reader.getLocalName();
            if (localName.equals("resources")) {
                processResources(reader, resources);
            } else {
                digest.update(localName.getBytes());
                processXml(reader, digest, resources);
            }
        }
    }

    protected static void processAttributes(final XMLStreamReader reader, final MessageDigest digest) {
        int attributes = reader.getAttributeCount();
        for (int i = 0; i < attributes; i++) {
            final String name = reader.getAttributeLocalName(i);
            final String value = reader.getAttributeValue(i);
            digest.update(name.getBytes());
            digest.update(value.getBytes());
        }
    }

    protected static void processResources(final XMLStreamReader reader, final Set<String> resources) throws
            XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String localName = reader.getLocalName();
            if ("resource-root".equals(localName)) {
                processResource(reader, resources);
            } else {
                throw new XMLStreamException("unrecognized element " + localName);
            }
        }
    }

    protected static void processResource(final XMLStreamReader reader, final Set<String> resources) throws XMLStreamException {
        final int attributeCount = reader.getAttributeCount();
        if (attributeCount != 1) {
            throw new XMLStreamException();
        }

        final String path = reader.getAttributeValue(0);
        resources.add(path.trim());

        if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            throw new XMLStreamException("unexpected element");
        }
    }

    private static void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

}
