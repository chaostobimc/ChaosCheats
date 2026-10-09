package de.chaos.fakeclient;

import com.mojang.blaze3d.platform.InputConstants;
import de.chaos.fakeclient.gui.ActiveModulesHud;
import de.chaos.fakeclient.gui.ChaosScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Reine Oberflaeche: oeffnet ein Fake-Client-Menue per Taste.
 * Es werden keine Spielinhalte, Pakete oder Eingaben veraendert.
 */
public class ChaosFakeClient implements ClientModInitializer {
	private static KeyMapping openKey;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath("chaosfakeclient", "main"));
		openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.chaosfakeclient.open",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new ChaosScreen());
				}
			}
		});

		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath("chaosfakeclient", "active_modules"),
				(context, tickCounter) -> ActiveModulesHud.render(context));
	}
}
