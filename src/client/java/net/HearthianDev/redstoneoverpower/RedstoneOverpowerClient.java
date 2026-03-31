package net.HearthianDev.redstoneoverpower;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.HearthianDev.redstoneoverpower.client.gui.screen.ingame.DuctScreen;

import static net.HearthianDev.redstoneoverpower.utils.Initialiser.*;

public class RedstoneOverpowerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		MenuScreens.register(DUCT_SCREEN_HANDLER, DuctScreen::new);
	}
}