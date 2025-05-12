package org.wildfly.qa.distdiff2.patching;

import static org.wildfly.qa.distdiff2.patching.PatchingMechanismAwarenessPhase.FILE_SEPARATOR_QUOTED;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.log4j.Logger;

/**
 * Used as a tool for working with files with respect to patching.
 * It is mainly used to map files' paths - convert from original (unpatched) path to overlay path and vice versa.
 *
 * @author Jan Martiska
 */
public final class PatchingAwareFileArtifact {

    private static final Logger LOGGER = Logger.getLogger(PatchingAwareFileArtifact.class);

    private String distributionAAbsolute;
    private String distributionBAbsolute;

    private String overlayFileRelative;
    private String originalFileRelative;
    private String moduleRootOverlaidRelative;

    private PatchingAwareFileArtifact() {
    }

    public static PatchingAwareFileArtifact fromOverlayFoundInB(String distributionA, String distributionB,
                                                                String relativePathInB) {
        PatchingAwareFileArtifact file = new PatchingAwareFileArtifact();
        file.distributionAAbsolute = distributionA;
        file.distributionBAbsolute = distributionB;
        file.overlayFileRelative = relativePathInB;
        file.moduleRootOverlaidRelative = getPathToModuleRoot(distributionB, relativePathInB);
        file.originalFileRelative = mapOverlayToOriginalPath(relativePathInB);
        return file;
    }

    public static PatchingAwareFileArtifact fromRemovedFileInB(String distributionA,
                                                               String distributionB,
                                                               String relativePathInA,
                                                               String layerPatchID) {
        PatchingAwareFileArtifact file = new PatchingAwareFileArtifact();
        file.distributionAAbsolute = distributionA;
        file.distributionBAbsolute = distributionB;
        file.overlayFileRelative = mapOriginalToOverlayPath(relativePathInA, layerPatchID);
        file.originalFileRelative = relativePathInA;
        String moduleRoot = getPathToModuleRoot(distributionA, relativePathInA);
        if (moduleRoot != null) {
            file.moduleRootOverlaidRelative = mapOriginalToOverlayPath(moduleRoot, layerPatchID);
        }
        return file;
    }

    public static PatchingAwareFileArtifact fromSameFileInAAndB(String distributionA,
                                                                String distributionB,
                                                                String relativePath,
                                                                String layerPatchID) {
        PatchingAwareFileArtifact file = new PatchingAwareFileArtifact();
        file.distributionAAbsolute = distributionA;
        file.distributionBAbsolute = distributionB;
        file.overlayFileRelative = mapOriginalToOverlayPath(relativePath, layerPatchID);
        String moduleRoot = getPathToModuleRoot(distributionA, relativePath);
        if (moduleRoot != null) {
            file.moduleRootOverlaidRelative = mapOriginalToOverlayPath(moduleRoot, layerPatchID);
        }
        return file;
    }

    /**
     * relative path to the overlay version of a file, eg.
     * 'modules/system/layers/base/.overlays/my-patch/org/jboss/ejb3/main/some-jar.jar'
     */
    public String getRelativePathToOverlay() {
        return overlayFileRelative;
    }

    /**
     * relative path to the original version of a file, eg.
     * 'modules/system/layers/base/org/jboss/ejb3/main/some-jar.jar'
     */
    public String getRelativePathToOriginal() {
        return mapOverlayToOriginalPath(overlayFileRelative);
    }


    /**
     * relative path to the overlaid version of module root, eg.
     * 'modules/system/layers/base/.overlays/my-patch/org/jboss/ejb3/main'
     * null if the file does not belong to a module
     */
    public String getModuleRootOverlaidRelative() {
        return moduleRootOverlaidRelative;
    }

    /**
     * absolute path to the original version of module root, eg.
     * '$SERVER_HOME/modules/system/layers/base/org/jboss/ejb3/main'
     * null if the file does not belong to a module
     */
    public String getModuleRootOriginalRelative() {
        if (getModuleRootOverlaidRelative() != null) {
            LOGGER.trace("Overlaid path: " + getModuleRootOverlaidRelative());
            LOGGER.trace("Mapped back to original path: " + mapOverlayToOriginalPath(getModuleRootOverlaidRelative()));
            return mapOverlayToOriginalPath(getModuleRootOverlaidRelative());
        } else {
            return null;
        }
    }

    /**
     * relative path to the original version of the file, eg.
     * 'modules/system/layers/base/org/jboss/ejb3/main/some-jar.jar'
     */
    public String getOriginalFileRelative() {
        return originalFileRelative;
    }

    /**
     * absolute path to the original version of the file in distribution A (the 'new' version)
     * eg. '$FRESHLY_UNZIPPED_SERVER/modules/system/layers/base/org/jboss/ejb3/main/some-jar.jar'
     */
    public String getOriginalFileInAAbsolute() {
        return distributionAAbsolute + File.separator + originalFileRelative;
    }

    /**
     * absolute path to the overlaid version of module root in distribution B (patched)
     * eg. '$SERVER_WITH_APPLIED_PATCH/modules/system/layers/base/.overlays/my-patch/org/jboss/ejb3/main'
     * null if the file does not belong to a module
     */
    public String getOverlaidModuleRootAbsolute() {
        if (moduleRootOverlaidRelative != null) {
            return distributionBAbsolute + File.separator + moduleRootOverlaidRelative;
        } else {
            return null;
        }
    }

    /**
     * absolute path to the original version of module root in distribution B (patched)
     * eg. '$SERVER_WITH_APPLIED_PATCH/modules/system/layers/base/org/jboss/ejb3/main'
     * null if the file does not belong to a module
     */
    public String getOriginalModuleRootInBAbsolute() {
        if (getModuleRootOriginalRelative() != null) {
            return distributionBAbsolute + File.separator + getModuleRootOriginalRelative();
        } else {
            return null;
        }
    }

    /**
     * absolute path to the original version of module root in distribution A (freshly unzipped CP)
     * eg. '$FRESHLY_UNZIPPED_SERVER/modules/system/layers/base/org/jboss/ejb3/main'
     * null if the file does not belong to a module
     */
    public String getModuleRootInAAbsolute() {
        if (getModuleRootOriginalRelative() == null) {
            return null;
        } else {
            return distributionAAbsolute + File.separator + getModuleRootOriginalRelative();
        }
    }

    /**
     * Gets the relative path to the module root for a file which belongs under that module.
     */
    private static String getPathToModuleRoot(String distribution, String relativePath) {
        // This assumes that modules contain no module.xml other than the 'expected' module.xml in their root directory.
        // Not sure if this assumption might cause issues some time in the future.
        String path = relativePath;
        if (canBeModuleRoot(distribution + File.separator + path)) {
            return path;
        }
        while (path.length() > 0) {
            int endIndex = path.lastIndexOf(File.separator);
            if (endIndex > path.length() || endIndex == -1) {
                return null;
            }
            path = path.substring(0, endIndex);
            if (canBeModuleRoot(distribution + File.separator + path)) {
                return path;
            }
        }
        return null;
    }


    /**
     * An abstract pathname can be a module root if:
     * it is a directory && it contains a file named module.xml
     */
    private static boolean canBeModuleRoot(String absolutePath) {
        File dir = new File(absolutePath);
        if (!dir.isDirectory()) {
            return false;
        }
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.equals("module.xml");
            }
        });
        if (files != null) {
            return files.length == 1;
        }
        return false;
    }

    private static String mapOverlayToOriginalPath(String relativePathInB) {
        String[] parts = relativePathInB.split(FILE_SEPARATOR_QUOTED +
                PatchingMechanismAwarenessPhase.OVERLAYS_REGEXP + FILE_SEPARATOR_QUOTED +
                "[-._a-zA-Z0-9]*" + FILE_SEPARATOR_QUOTED);
        String result = parts[0];
        if (parts.length > 1) {
            result = result + File.separator + parts[1];
        }
        return result;
    }

    private static String mapOriginalToOverlayPath(String relativePathInA, String patchID) {
        String[] parts = relativePathInA.split(FILE_SEPARATOR_QUOTED);
        StringBuilder result = new StringBuilder();
        boolean layerNameFollowing = false;
        for (String part : parts) {
            result.append(part);
            result.append(File.separator);
            if (layerNameFollowing) {
                layerNameFollowing = false;
                result.append(PatchingMechanismAwarenessPhase.OVERLAYS);
                result.append(File.separator);
                result.append(patchID);
                result.append(File.separator);
            }
            if (part.equals("layers")) {
                layerNameFollowing = true;
            }
        }
        result.deleteCharAt(result.length() - 1); // delete trailing slash
        return result.toString();

    }


}
