package org.wildfly.qa.distdiff2.patching.hashing;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.tools.Tools;

/**
 * @author Jan Martiska
 */
public class ImprovedHashingUtils {

    private static final Logger LOGGER = Logger.getLogger(ImprovedHashingUtils.class.getName());

    // Ignored manifest attributes
    private static final Set<String> IGNORED_MANIFEST_ATTRIBUTES = new HashSet<>();

    static {
        IGNORED_MANIFEST_ATTRIBUTES.add("Archiver-Version");
        IGNORED_MANIFEST_ATTRIBUTES.add("Built-By");
        IGNORED_MANIFEST_ATTRIBUTES.add("Build-Jdk");
        IGNORED_MANIFEST_ATTRIBUTES.add("Build-Timestamp");
        IGNORED_MANIFEST_ATTRIBUTES.add("Created-By");
        IGNORED_MANIFEST_ATTRIBUTES.add("Implementation-Version");
        IGNORED_MANIFEST_ATTRIBUTES.add("Java-Vendor");
        IGNORED_MANIFEST_ATTRIBUTES.add("Java-Version");
        IGNORED_MANIFEST_ATTRIBUTES.add("JBossAS-Release-Version");
        IGNORED_MANIFEST_ATTRIBUTES.add("Os-Arch");
        IGNORED_MANIFEST_ATTRIBUTES.add("Os-Name");
        IGNORED_MANIFEST_ATTRIBUTES.add("Os-Version");
        IGNORED_MANIFEST_ATTRIBUTES.add("Scm-Revision");
        IGNORED_MANIFEST_ATTRIBUTES.add("Specification-Version");
        IGNORED_MANIFEST_ATTRIBUTES.add("Bnd-LastModified");
    }

    /**
     * Calculate a has for a file. This might use different ways to calculate the hash for modules, jars and other
     * files.
     *
     * @param root the file system path
     * @return the hash for the path
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static byte[] calculateHash(final File root) throws Exception {

        final File moduleXml = new File(root, "module.xml");
        if (moduleXml.exists()) {
            return ModuleDiffUtils.processModule(root);
        } else if (root.getName().equals(".jar")) {
            return internalJarComparison(root);
        } else {
            return Tools.calculateHashOfDirectory(root).toString().getBytes();
        }
    }

    public static byte[] internalJarComparison(final File file) throws NoSuchAlgorithmException, IOException {
        final MessageDigest jarDigest = MessageDigest.getInstance("SHA1");
        internalJarComparison(file, jarDigest, true);
        return jarDigest.digest();
    }

    /**
     * Internally compare a .jar file, trying to ignore things that usually change through a rebuild.
     *
     * @param file      the jar file
     * @param jarDigest the message jar message digest
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static void internalJarComparison(final File file, final MessageDigest jarDigest, boolean debug) throws NoSuchAlgorithmException, IOException {
        final TreeSet<Entry> entries = new TreeSet<>();
        final MessageDigest digest = MessageDigest.getInstance("SHA1");
        final JarInputStream in = new JarInputStream(new BufferedInputStream(new FileInputStream(file)));
        try {
            JarEntry entry;
            while ((entry = in.getNextJarEntry()) != null) {
                // do not hash directories
                if (entry.isDirectory()) {
                    continue;
                }
                final String name = entry.getName();
                // do not hash information added by jarsigner
                if (name.startsWith("META-INF/")) {
                    if (name.endsWith(".SF") || name.endsWith(".DSA")) {
                        continue;
                    }
                }
                if (name.equals("META-INF/INDEX.LIST")) {
                    continue;
                }
                // do not hash timestamped maven artifacts
                // TODO: make this optional, enabled by default
                if (name.startsWith("META-INF/maven/")) {
                    if (name.endsWith("/pom.properties")
                            || name.endsWith("/pom.xml")
                            || name.endsWith("/effective-pom.xml")) {
                        continue;
                    }
                }
                // Ignore generated logger and bundle, since the created classes are not consistent
                if (name.endsWith(".class")) {
                    if (name.endsWith("_$bundle.class") || name.endsWith("_$logger.class")
                            || name.contains("_$bundle_") || name.contains("_$logger_")) {
                        continue;
                    }
                }

                digest.reset();
                final byte[] buf = new byte[4096];
                int l;
                while ((l = in.read(buf)) > 0) {
                    digest.update(buf, 0, l);
                }
                final byte[] d = digest.digest();
                // Add to ordered set
                entries.add(new Entry(name, d));
            }
        } finally {
            in.close();
        }

        // Process the manifest if there is any
        final Manifest manifest = in.getManifest();
        if (manifest != null) {
            digest.reset();
            final Attributes attributes = manifest.getMainAttributes();
            for (final Map.Entry<Object, Object> entry : attributes.entrySet()) {
                final String name = entry.getKey().toString();
                // Ignore attributes that change with every rebuild
                if (!IGNORED_MANIFEST_ATTRIBUTES.contains(name)) {
                    final String value = (String) entry.getValue();
                    digest.update(name.getBytes());
                    digest.update(value.getBytes());
                }
            }
            // Add the manifest
            entries.add(new Entry(JarFile.MANIFEST_NAME, digest.digest()));
        }
        // Add the hashes to the jarDigest
        for (final Entry entry : entries) {
            final byte[] hash = entry.getHash();
            if (hash != null) {
                if (file.getAbsolutePath().contains("version")) {
                    LOGGER.trace("entry: " + entry.getName() + ", integer hash=" + Tools.byteArrayToInteger(hash));
                }
                jarDigest.update(hash);
            }
        }
    }

    static class Entry implements Comparable<Entry> {

        private final String name;
        private final byte[] hash;

        Entry(String name, byte[] hash) {
            this.name = name;
            this.hash = hash;
        }

        String getName() {
            return name;
        }

        byte[] getHash() {
            return hash;
        }

        @Override
        public int compareTo(Entry o) {
            return name.compareTo(o.name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final Entry entry = (Entry) o;
            if (!name.equals(entry.name)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

}
