package net.HearthianDev.redstoneoverpower;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.HearthianDev.redstoneoverpower.client.gui.screen.ingame.DuctScreen;

import static net.HearthianDev.redstoneoverpower.utils.Initialiser.*;

public class RedstoneOverpowerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		MenuScreens.register(DUCT_SCREEN_HANDLER, DuctScreen::new);

		BlockRenderLayerMap.putBlock(LOGICAL_COMPARATOR_BLOCK, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(SCULK_PULSER_BLOCK, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(ECHO_CLUSTER_BLOCK, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(LARGE_ECHO_BUD_BLOCK, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(MEDIUM_ECHO_BUD_BLOCK, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(SMALL_ECHO_BUD_BLOCK, ChunkSectionLayer.CUTOUT);
	}
}