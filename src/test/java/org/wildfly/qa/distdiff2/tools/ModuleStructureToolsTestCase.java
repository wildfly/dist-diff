package org.wildfly.qa.distdiff2.tools;

import static org.wildfly.qa.distdiff2.patching.ModuleStructureTools.BASE_LAYER;
import static org.wildfly.qa.distdiff2.patching.ModuleStructureTools.LAYERS_CONF;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

import org.wildfly.qa.distdiff2.patching.ModuleStructureTools;
import org.wildfly.qa.distdiff2.patching.PatchingMechanismAwarenessPhase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link ModuleStructureTools} class.
 *
 * @author Jan Stourac
 */
public class ModuleStructureToolsTestCase {

    private static Path rootA;
    private static Path rootB;
    private static final String EXTRA_LAYER = "extraLayer";
    private final String ACTIVE_PATCH_OVERLAY_ID = "active-patch-overlay-id";

    @After
    public void clean() throws IOException {
        if (Files.exists(rootA)) {
            Files.walk(rootA).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        if (Files.exists(rootB)) {
            Files.walk(rootB).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    /**
     * .overlays dir does not exist
     *
     * @throws IOException in case of failure in preparation of tmp files/dirs necessary for testing
     */
    @Test
    public void getActiveLayerPatchIDOverlaysDirDoesNotExist() throws IOException {
        rootA = Files.createTempDirectory("rootA");
        Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system", "layers",
                BASE_LAYER));

        Assert.assertNull(ModuleStructureTools.getActiveLayerPatchID(BASE_LAYER, rootA.toAbsolutePath().toString()));
    }

    /**
     * .overlays file does not exist
     *
     * @throws IOException in case of failure in preparation of tmp files/dirs necessary for testing
     */
    @Test
    public void getActiveLayerPatchIDOverlaysFileDoesNotExist() throws IOException {
        rootA = Files.createTempDirectory("rootA");
        Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system", "layers",
                BASE_LAYER, PatchingMechanismAwarenessPhase.OVERLAYS));

        Assert.assertNull(ModuleStructureTools.getActiveLayerPatchID(BASE_LAYER, rootA.toAbsolutePath().toString()));
    }

    /**
     * .overlays file is empty
     *
     * @throws IOException in case of failure in preparation of tmp files/dirs necessary for testing
     */
    @Test
    public void getActiveLayerPatchIDOverlaysFileIsEmpty() throws IOException {
        rootA = Files.createTempDirectory("rootA");
        Path overlaysDir = Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system",
                "layers", BASE_LAYER, PatchingMechanismAwarenessPhase.OVERLAYS));
        Files.createFile(Paths.get(overlaysDir.toAbsolutePath().toString(), PatchingMechanismAwarenessPhase.OVERLAYS));

        Assert.assertNull(ModuleStructureTools.getActiveLayerPatchID(BASE_LAYER, rootA.toAbsolutePath().toString()));
    }

    /**
     * determined overlay name is NOT present as a directory
     *
     * @throws IOException in case of failure in preparation of tmp files/dirs necessary for testing
     */
    @Test
    public void getActiveLayerPatchIDOverlaysMissingDetectedDirectory() throws IOException {
        rootA = Files.createTempDirectory("rootA");
        Path overlaysDir = Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system",
                "layers", BASE_LAYER, PatchingMechanismAwarenessPhase.OVERLAYS));

        Path overlaysFile = Files.createFile(Paths.get(overlaysDir.toAbsolutePath().toString(),
                PatchingMechanismAwarenessPhase.OVERLAYS));
        Files.write(overlaysFile, (ACTIVE_PATCH_OVERLAY_ID).getBytes());

        Assert.assertNull(ModuleStructureTools.getActiveLayerPatchID(BASE_LAYER, rootA.toAbsolutePath().toString()));
    }

    /**
     * determined overlay name is correctly present as a directory
     *
     * @throws IOException in case of failure in preparation of tmp files/dirs necessary for testing
     */
    @Test
    public void getActiveLayerPatchIDOverlaysSuccess() throws IOException {
        rootA = Files.createTempDirectory("rootA");
        Path overlaysDir = Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system",
                "layers", BASE_LAYER, PatchingMechanismAwarenessPhase.OVERLAYS));
        Files.createDirectories(Paths.get(overlaysDir.toAbsolutePath().toString(), ACTIVE_PATCH_OVERLAY_ID));

        Path overlaysFile = Files.createFile(Paths.get(overlaysDir.toAbsolutePath().toString(),
                PatchingMechanismAwarenessPhase.OVERLAYS));
        Files.write(overlaysFile, (ACTIVE_PATCH_OVERLAY_ID).getBytes());

        Assert.assertEquals(ACTIVE_PATCH_OVERLAY_ID, ModuleStructureTools.getActiveLayerPatchID(BASE_LAYER,
                rootA.toAbsolutePath().toString()));
    }

    /**
     * correct overlay is chosen in case that more overlay directories are present
     *
     * @throws IOException in case of failure in preparation of tmp files/dirs necessary for testing
     */
    @Test
    public void getActiveLayerPatchIDOverlaysCorrectlyDetectedDirectory() throws IOException {
        rootA = Files.createTempDirectory("rootA");
        Path overlaysDir = Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system",
                "layers", BASE_LAYER, PatchingMechanismAwarenessPhase.OVERLAYS));
        Files.createDirectories(Paths.get(overlaysDir.toAbsolutePath().toString(), ACTIVE_PATCH_OVERLAY_ID));
        Files.createDirectories(Paths.get(overlaysDir.toAbsolutePath().toString(), "dummy" + ACTIVE_PATCH_OVERLAY_ID));
        Files.createDirectories(Paths.get(overlaysDir.toAbsolutePath().toString(),
                "dummy" + ACTIVE_PATCH_OVERLAY_ID + "waka-waka"));

        Path overlaysFile = Files.createFile(Paths.get(overlaysDir.toAbsolutePath().toString(),
                PatchingMechanismAwarenessPhase.OVERLAYS));
        Files.write(overlaysFile, (ACTIVE_PATCH_OVERLAY_ID).getBytes());

        Assert.assertEquals(ACTIVE_PATCH_OVERLAY_ID, ModuleStructureTools.getActiveLayerPatchID(BASE_LAYER,
                rootA.toAbsolutePath().toString()));
    }

    /**
     * layers.conf not present (layer directories also good)
     *
     * @throws Exception in case of failure in preparation of tmp files/dirs necessary for testing
     */
    @Test
    public void getLayersTestNoLayersConfLayersGood() throws Exception {
        rootA = Files.createTempDirectory("rootA");
        Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system", "layers",
                BASE_LAYER));
        rootB = Files.createTempDirectory("rootB");
        Files.createDirectories(Paths.get(rootB.toAbsolutePath().toString(), "modules", "system", "layers",
                BASE_LAYER));

        try {
            List<String> list = ModuleStructureTools.getLayers(rootA.toAbsolutePath().toString(),
                    rootB.toAbsolutePath().toString());
            Assert.assertEquals(1, list.size());
            Assert.assertEquals(BASE_LAYER, list.get(0));
        } catch (Exception e) {
            Assert.fail("No exception was not expected! Test failed! " + e.getMessage());
        }
    }

    /**
     * layers.conf not present (layer directory missing)
     *
     * @throws Exception in case of failure in preparation of tmp files/dirs necessary for testing
     */
    @Test
    public void getLayersTestNoLayersConfLayersBad() throws Exception {
        rootA = Files.createTempDirectory("rootA");
        Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system", "layers",
                BASE_LAYER));
        rootB = Files.createTempDirectory("rootB");
        Files.createDirectories(Paths.get(rootB.toAbsolutePath().toString(), "modules", "system", "layers"));

        Assert.assertThrows(IllegalStateException.class,
                () -> ModuleStructureTools.getLayers(rootA.toAbsolutePath().toString(),
                        rootB.toAbsolutePath().toString()));
    }

    /**
     * layers.conf present, only base layer (layer directories good)
     *
     * @throws Exception in case of failure in preparation of tmp files/dirs necessary for testing
     */
    @Test
    public void getLayersTestLayersConfLayersGoodOnlyBaseLayer() throws Exception {
        rootA = Files.createTempDirectory("rootA");
        Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system", "layers",
                BASE_LAYER));
        rootB = Files.createTempDirectory("rootB");
        Files.createDirectories(Paths.get(rootB.toAbsolutePath().toString(), "modules", "system", "layers",
                BASE_LAYER));
        Path layerConf = Files.createFile(Paths.get(rootB.toAbsolutePath().toString(), "modules", LAYERS_CONF));
        Files.write(layerConf, ("layers=" + BASE_LAYER).getBytes());

        try {
            List<String> list = ModuleStructureTools.getLayers(rootA.toAbsolutePath().toString(),
                    rootB.toAbsolutePath().toString());
            Assert.assertEquals(1, list.size());
            Assert.assertEquals(BASE_LAYER, list.get(0));
        } catch (Exception e) {
            Assert.fail("No exception was not expected! Test failed! " + e.getMessage());
        }
    }

    /**
     * layers.conf present but empty, only base layer (layer directories good)
     *
     * @throws Exception in case of failure in preparation of tmp files/dirs necessary for testing
     */
    @Test
    public void getLayersTestLayersConfEmpty() throws Exception {
        rootA = Files.createTempDirectory("rootA");
        Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system", "layers",
                BASE_LAYER));
        rootB = Files.createTempDirectory("rootB");
        Files.createDirectories(Paths.get(rootB.toAbsolutePath().toString(), "modules", "system", "layers",
                BASE_LAYER));
        Files.createFile(Paths.get(rootB.toAbsolutePath().toString(), "modules", LAYERS_CONF));

        Assert.assertThrows(IllegalStateException.class,
                () -> ModuleStructureTools.getLayers(rootA.toAbsolutePath().toString(),
                        rootB.toAbsolutePath().toString()));
    }

    /**
     * layers.conf present, extra and base layers defined in file (layer directories good)
     *
     * @throws Exception in case of failure in preparation of tmp files/dirs necessary for testing
     */
    @Test
    public void getLayersTestLayersConfLayersGoodExtraAndBaseLayer() throws Exception {
        rootA = Files.createTempDirectory("rootA");
        Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system", "layers",
                BASE_LAYER));
        Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system", "layers",
                EXTRA_LAYER));
        rootB = Files.createTempDirectory("rootB");
        Files.createDirectories(Paths.get(rootB.toAbsolutePath().toString(), "modules", "system", "layers",
                BASE_LAYER));
        Files.createDirectories(Paths.get(rootB.toAbsolutePath().toString(), "modules", "system", "layers",
                EXTRA_LAYER));
        Path layerConf = Files.createFile(Paths.get(rootB.toAbsolutePath().toString(), "modules", LAYERS_CONF));
        Files.write(layerConf, ("layers=" + EXTRA_LAYER + "," + BASE_LAYER).getBytes());

        try {
            List<String> list = ModuleStructureTools.getLayers(rootA.toAbsolutePath().toString(),
                    rootB.toAbsolutePath().toString());
            Assert.assertEquals(2, list.size());
            Assert.assertEquals(EXTRA_LAYER, list.get(0));
            Assert.assertEquals(BASE_LAYER, list.get(1));
        } catch (Exception e) {
            Assert.fail("No exception was not expected! Test failed! " + e.getMessage());
        }
    }

    /**
     * layers.conf present, only extra layer in file defined (layer directories good - both base and extra)
     *
     * @throws Exception in case of failure in preparation of tmp files/dirs necessary for testing
     */
    @Test
    public void getLayersTestLayersConfLayersGoodExtraAndBaseLayers() throws Exception {
        rootA = Files.createTempDirectory("rootA");
        Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system", "layers",
                BASE_LAYER));
        Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system", "layers",
                EXTRA_LAYER));
        rootB = Files.createTempDirectory("rootB");
        Files.createDirectories(Paths.get(rootB.toAbsolutePath().toString(), "modules", "system", "layers",
                BASE_LAYER));
        Files.createDirectories(Paths.get(rootB.toAbsolutePath().toString(), "modules", "system", "layers",
                EXTRA_LAYER));
        Path layerConf = Files.createFile(Paths.get(rootB.toAbsolutePath().toString(), "modules", LAYERS_CONF));
        Files.write(layerConf, ("layers=" + EXTRA_LAYER).getBytes());

        try {
            List<String> list = ModuleStructureTools.getLayers(rootA.toAbsolutePath().toString(),
                    rootB.toAbsolutePath().toString());
            Assert.assertEquals(2, list.size());
            Assert.assertEquals(EXTRA_LAYER, list.get(0));
            Assert.assertEquals(BASE_LAYER, list.get(1));
        } catch (Exception e) {
            Assert.fail("No exception was not expected! Test failed! " + e.getMessage());
        }
    }

    /**
     * layers.conf present, only extra layer in file defined (only extra layer directories present - missing base)
     *
     * @throws Exception in case of failure in preparation of tmp files/dirs necessary for testing
     */
    @Test
    public void getLayersTestLayersConfLayersMissingBaseLayer() throws Exception {
        rootA = Files.createTempDirectory("rootA");
        Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system", "layers",
                EXTRA_LAYER));
        rootB = Files.createTempDirectory("rootB");
        Files.createDirectories(Paths.get(rootB.toAbsolutePath().toString(), "modules", "system", "layers",
                EXTRA_LAYER));
        Path layerConf = Files.createFile(Paths.get(rootB.toAbsolutePath().toString(), "modules", LAYERS_CONF));
        Files.write(layerConf, ("layers=" + EXTRA_LAYER).getBytes());

        Assert.assertThrows(IllegalStateException.class,
                () -> ModuleStructureTools.getLayers(rootA.toAbsolutePath().toString(),
                        rootB.toAbsolutePath().toString()));
    }

    /**
     * layers.conf present, only non-sense layer defined in file (base layer directories okay)
     *
     * @throws Exception in case of failure in preparation of tmp files/dirs necessary for testing
     */
    @Test
    public void getLayersTestLayersConfNonSenseLayer() throws Exception {
        rootA = Files.createTempDirectory("rootA");
        Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system", "layers",
                BASE_LAYER));
        rootB = Files.createTempDirectory("rootB");
        Files.createDirectories(Paths.get(rootB.toAbsolutePath().toString(), "modules", "system", "layers",
                BASE_LAYER));
        Path layerConf = Files.createFile(Paths.get(rootB.toAbsolutePath().toString(), "modules", LAYERS_CONF));
        Files.write(layerConf, ("layers=non-sense").getBytes());

        Assert.assertThrows(IllegalStateException.class,
                () -> ModuleStructureTools.getLayers(rootA.toAbsolutePath().toString(),
                        rootB.toAbsolutePath().toString()));
    }

    /**
     * layers.conf present, base layout only, missing directory
     *
     * @throws Exception in case of failure in preparation of tmp files/dirs necessary for testing
     */
    @Test
    public void getLayersTestLayersConfMissingBaseLayerDir() throws Exception {
        rootA = Files.createTempDirectory("rootA");
        Files.createDirectories(Paths.get(rootA.toAbsolutePath().toString(), "modules", "system", "layers"));
        rootB = Files.createTempDirectory("rootB");
        Files.createDirectories(Paths.get(rootB.toAbsolutePath().toString(), "modules", "system", "layers",
                BASE_LAYER));
        Path layerConf = Files.createFile(Paths.get(rootB.toAbsolutePath().toString(), "modules", LAYERS_CONF));
        Files.write(layerConf, ("layers=" + BASE_LAYER).getBytes());

        Assert.assertThrows(IllegalStateException.class,
                () -> ModuleStructureTools.getLayers(rootA.toAbsolutePath().toString(),
                        rootB.toAbsolutePath().toString()));
    }
}
