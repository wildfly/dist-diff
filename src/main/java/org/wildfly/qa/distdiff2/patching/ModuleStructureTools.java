package org.wildfly.qa.distdiff2.patching;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;

/**
 * @author Jan Martiska
 */
public class ModuleStructureTools {

    private static final Logger LOGGER = Logger.getLogger(ModuleStructureTools.class);
    public static final String BASE_LAYER = "base";
    public static final String LAYERS_CONF = "layers.conf";

    /**
     * grabs the names of all layer-patches applied onto a specific layer
     *
     * @param layer                    the name of the layer, usually 'base'
     * @param distributionAbsolutePath the path to the distribution
     */
    public static List<String> getLayerPatchIDs(String layer, String distributionAbsolutePath) {
        File overlaysDir = new File(distributionAbsolutePath
                + File.separator + "modules" + File.separator + "system" + File.separator
                + "layers" + File.separator + layer + File.separator + PatchingMechanismAwarenessPhase.OVERLAYS);
        if (!overlaysDir.isDirectory()) {
            throw new IllegalArgumentException(overlaysDir + " is not a directory");
        }
        File[] patchesDirs = overlaysDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        List<String> names = new ArrayList<>();
        if (patchesDirs != null) {
            for (File file : patchesDirs) {
                names.add(file.getName());
            }
        }
        return names;
    }

    /**
     * Grabs the name of the currently active layer-patch applied onto a specific layer.
     * Returns null if there is no active overlay (eg. there is no .overlays file or it is empty).
     *
     * @param layer                    the name of the layer, usually 'base'
     * @param distributionAbsolutePath the path to the distribution
     */
    public static String getActiveLayerPatchID(final String layer, final String distributionAbsolutePath) {
        File overlaysDir = new File(distributionAbsolutePath
                + File.separator + "modules" + File.separator + "system" + File.separator
                + "layers" + File.separator + layer + File.separator + PatchingMechanismAwarenessPhase.OVERLAYS);

        if (!overlaysDir.isDirectory()) {
            LOGGER.warn("Can't determine name of the applied patch because '" + overlaysDir +
                    "' does not exist or is not a directory.");
            return null;
        }

        File dotOverlaysFile = new File(overlaysDir, PatchingMechanismAwarenessPhase.OVERLAYS);
        if (!dotOverlaysFile.exists() || !dotOverlaysFile.isFile()) {
            LOGGER.warn("Can't determine name of the applied patch because '" + dotOverlaysFile +
                    "' does not exist or is not a file.");
            return null;
        }

        try (Scanner scanner = new Scanner(dotOverlaysFile)) {
            scanner.useDelimiter("\\Z");
            if (scanner.hasNext()) {
                String activePatchNameString = scanner.next();
                if (activePatchNameString.length() == 0) {
                    LOGGER.warn("Can't determine name of the applied patch from '" + dotOverlaysFile + "' file.");
                    return null;
                }

                // Check that determined name of applied patch is also present as a directory.
                File[] patchesDirs = overlaysDir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isDirectory();
                    }
                });
                if (patchesDirs != null) {
                    for (File file : patchesDirs) {
                        if (file.getName().equals(activePatchNameString)) {
                            return activePatchNameString;
                        }
                    }
                }

                LOGGER.warn("The '" + dotOverlaysFile + "' file says that an overlay named '" + activePatchNameString +
                        "' is applied on top of layer '" + layer + "', but there is no such directory under '" +
                        overlaysDir.getAbsolutePath() + "'. As such, we could not determine applied patch which may " +
                        "cause problems with further distribution comparison!");
                return null;
            } else {
                LOGGER.warn("Can't determine name of the applied patch from '" + dotOverlaysFile + "' file.");
                return null;
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets list of layers in distribution. It tries to read file 'JBOSS_HOME/modules/layers.conf'. If this file is
     * not present, then only one {@link ModuleStructureTools#BASE_LAYER} is expected and returned. Order of the
     * layers is kept as defined in 'layers.conf' file so we respect priority. Result list of the layers expected is
     * verified by checking that relevant layer directories exist. Also, it is expected that these layers lists are
     * same both for A and B distributions.
     *
     * @param distroAAbsolutePath absolute path to the distribution A
     * @param distroBAbsolutePath absolute path to the distribution B
     * @return list of detected layers in order that respects layers priority (from most important to least)
     * @throws IllegalStateException if layers could not be successfully detected
     */
    public static List<String> getLayers(final String distroAAbsolutePath, final String distroBAbsolutePath)
            throws IllegalStateException {
        LinkedList<String> layers = new LinkedList<>();

        File modulesDir = new File(distroBAbsolutePath + File.separator + "modules");
        if (!modulesDir.isDirectory()) {
            String message = "Can't determine list of layers in distribution because '" + modulesDir +
                    "' does not exist or is not a directory.";
            LOGGER.error(message);
            throw new IllegalStateException(message);
        }

        File layersConfFile = new File(modulesDir, LAYERS_CONF);
        if (layersConfFile.exists() && layersConfFile.isFile()) {
            // This is probably release 7.3.1+. Get list of layers from this file
            LOGGER.info("'" + layersConfFile.getAbsolutePath()
                    + "' file has been detected, reading it's content now...");

            try (Scanner scanner = new Scanner(layersConfFile)) {
                scanner.useDelimiter("\\Z");
                if (!scanner.hasNext()) {
                    throw new IllegalStateException("Cannot read layers from the '" + layersConfFile + "' file.");
                }
                String layersList = scanner.next();

                // layers.conf file contains something like: layers=microprofile,base
                if (!layersList.isEmpty() && layersList.contains("=")) {
                    layersList = layersList.split("=")[1];

                    layers.addAll(Arrays.asList(layersList.split(",")));
                } else {
                    String message = "Can't determine layers list from the '" + layersConfFile + "' file.";
                    LOGGER.error(message);
                    throw new IllegalStateException(message);
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOGGER.info("'" + layersConfFile.getAbsolutePath()
                    + "' file has NOT been detected, using default layer only.");
        }

        if (!layers.contains(BASE_LAYER)) {
            // Older releases does not have 'layers.conf' file. We shall use just default layer instead.
            //     ... or...
            // Default layer does not have to be explicitly defined in the 'layers.conf' file. In such case, we
            // need to add this layer to the list at the end manually now.
            layers.add(BASE_LAYER);
        }

        // Now check that directories for detected layers exist in both distributions
        for (String layer : layers) {
            checkDistroForLayerDirectory(distroAAbsolutePath, layer);
            checkDistroForLayerDirectory(distroBAbsolutePath, layer);
        }

        return layers;
    }

    /**
     * Checks for relevant folder for layer in given distribution.
     *
     * @param distroAbsolutePath absolute path to checked distribution folder
     * @param layerName          name of the layer that shall be checked that its folder exists in distribution
     * @throws IllegalStateException if relevant layer does not exist in distribution
     */
    private static void checkDistroForLayerDirectory(final String distroAbsolutePath, final String layerName)
            throws IllegalStateException {
        final String variablePath = File.separator + "modules" + File.separator + "system" + File.separator + "layers"
                + File.separator + layerName;
        final File layerDir = new File(distroAbsolutePath + variablePath);

        if (!layerDir.isDirectory()) {
            // Detected layer does not exist as a directory in distribution
            String message = "Detected layer '" + layerName + "' does not exist as a directory in distribution '"
                    + distroAbsolutePath + "'. Cannot locate '" + layerDir.getAbsolutePath() + "'.";
            LOGGER.error(message);
            throw new IllegalStateException(message);
        }
    }
}
