package org.wildfly.qa.distdiff2.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.artifacts.ArchiveArtifact;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.ClassArtifact;
import org.wildfly.qa.distdiff2.artifacts.FileArtifact;
import org.wildfly.qa.distdiff2.artifacts.FolderArtifact;
import org.wildfly.qa.distdiff2.artifacts.JarArtifact;

/**
 * ArtifactBuilder
 * <p>
 * Builds representation of file in distribution, file is represented by corresponding {@link Artifact} object.
 */
public final class ArtifactBuilder {

    // Internal logger
    private static final Logger LOGGER = Logger.getLogger(ArtifactBuilder.class);

    private static final Set<String> ZIP_ARCHIVES = new HashSet<>(Arrays.asList("zip", "war", "ear", "sar"));

    /**
     * Creates artifacts from given file
     *
     * @param file               source {@link File}
     * @param distributionFolder representation of source distribution
     * @param level              current level of processed artifact (relative from root of distribution)
     * @param distributionA      is processed file part of the A distribution
     * @return corresponding representation of file
     */
    public static Artifact create(File file, File distributionFolder, int level, boolean distributionA) {
        Artifact artifact = null;
        if (file != null) {
            // Calculation of paths
            String absolutePath = file.getAbsolutePath();
            String parentPath = file.getParent();
            String distributionPath = (distributionFolder != null) ? distributionFolder.getAbsolutePath()
                    : "";
            parentPath = (parentPath.length() > distributionPath.length()) ?
                    parentPath.substring(distributionPath.length()) : "";
            String relativePath = (distributionFolder != null) ?
                    absolutePath
                            .substring(distributionFolder.getAbsolutePath().length())
                    : "";
            if (relativePath.startsWith(File.separator)) {
                relativePath = relativePath.substring(1);
            }
            if (parentPath.startsWith(File.separator)) {
                parentPath = parentPath.substring(1);
            }

            // Create artifact
            if (file.isDirectory()) {
                artifact = createFolder(file, relativePath, absolutePath, distributionA);
            } else if (file.isFile()) {
                artifact = createFile(file, relativePath, absolutePath, distributionA);
            }

            // Shared parameters for all types of artifacts
            if (artifact != null) {
                artifact.setLevel(level);
                artifact.setParentRelativePath(parentPath);
            }
        }

        return artifact;
    }

    /**
     * Creates folder artifact
     *
     * @param file          instance of file
     * @param relativePath  relative path in distribution
     * @param absolutePath  absolute path
     * @param distributionA is distribution A?
     * @return instance of artifact
     */
    private static Artifact createFolder(File file, String relativePath, String absolutePath,
                                         boolean distributionA) {
        Artifact artifact;
        artifact = new FolderArtifact(file.getName(), relativePath);
        if (distributionA) {
            artifact.setPathA(absolutePath);
        } else {
            artifact.setPathB(absolutePath);
        }
        return artifact;
    }

    /**
     * Creates file artifact
     *
     * @param file          instance of file
     * @param relativePath  relative path in distribution
     * @param absolutePath  absolute path
     * @param distributionA is distribution A?
     * @return instance of artifact
     */
    private static Artifact createFile(File file, String relativePath, String absolutePath,
                                       boolean distributionA) {
        FileArtifact artifact;
        String name = file.getName();
        String extension = Tools.getExtension(name);
        String mimeType = null;
        try {
            mimeType = Files.probeContentType(Paths.get(file.getCanonicalPath()));
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        if (extension != null && extension.equalsIgnoreCase("jar")) {
            mimeType = "application/x-java-archive";
        }

        if ("application/x-java-archive".equals(mimeType)) {
            JarArtifact jarArtifact = new JarArtifact(name, relativePath, file.length());
            if (distributionA) {
                jarArtifact.setPathA(absolutePath);
            } else {
                jarArtifact.setPathB(absolutePath);
            }
            Tools.parseJarVersionInformation(jarArtifact);
            artifact = jarArtifact;

        } else if ("class".equalsIgnoreCase(extension)) { // Class file
            artifact = new ClassArtifact(name, relativePath, file.length());

        } else if (extension != null && ZIP_ARCHIVES.contains(extension.toLowerCase())) { // Archive artifact
            artifact = new ArchiveArtifact(name, relativePath, file.length());

        } else { // Just file
            artifact = new FileArtifact(name, relativePath, file.length());
        }

        if (distributionA) {
            artifact.setPathA(absolutePath);
        } else {
            artifact.setPathB(absolutePath);
        }

        if (mimeType != null) {
            try {
                if (mimeType.equalsIgnoreCase("text/plain") && !Tools.isTextFile(file.getCanonicalPath())) {
                    artifact.setMimeType("unknown binary format");
                } else {
                    artifact.setMimeType(mimeType);
                }
            } catch (IOException e) {
                LOGGER.error(e);
            }
        } else {
            // if the JDK cannot determine the mime type, default to text/plain, if it is a text file
            try {
                if (Tools.isTextFile(file.getCanonicalPath())) {
                    artifact.setMimeType("text/plain");
                } else {
                    artifact.setMimeType("unknown binary format");
                }

            } catch (IOException e) {
                LOGGER.error("Cannot determine if file is a text file " + e);
            }
        }

        return artifact;
    }

}
