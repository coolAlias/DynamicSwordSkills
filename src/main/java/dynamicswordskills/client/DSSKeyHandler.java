/**
    Copyright (C) <2017> <coolAlias>

    This file is part of coolAlias' Dynamic Sword Skills Minecraft Mod; as such,
    you can redistribute it and/or modify it under the terms of the GNU
    General Public License as published by the Free Software Foundation,
    either version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package dynamicswordskills.client;

import org.lwjgl.input.Keyboard;

import dynamicswordskills.CommonProxy;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.bidirectional.ActivateSkillPacket;
import dynamicswordskills.network.server.OpenGuiPacket;
import dynamicswordskills.ref.Config;
import dynamicswordskills.skills.ILockOnTarget;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class DSSKeyHandler
{
	private final Minecraft mc;

	/** Key index for easy handling and retrieval of keys and key descriptions */
	public static final byte 
	KEY_SKILL_ACTIVATE = 0,
	KEY_NEXT_TARGET = 1,
	KEY_SKILLS_GUI = 2,
	KEY_ATTACK = 3,
	KEY_LEFT = 4,
	KEY_RIGHT = 5,
	KEY_DOWN = 6,
	KEY_BLOCK = 7;

	/** Key descriptions - this is what the player sees when changing key bindings in-game */
	private static final String[] desc = {
			"activate",
			"next",
			"skills_gui",
			"attack",
			"left",
			"right",
			"down",
			"block"
	};

	/** Default key values */
	private static final int[] keyValues = {
			Keyboard.KEY_X,
			Keyboard.KEY_TAB,
			Keyboard.KEY_P,
			Keyboard.KEY_UP,
			Keyboard.KEY_LEFT,
			Keyboard.KEY_RIGHT,
			Keyboard.KEY_DOWN,
			Keyboard.KEY_RCONTROL
	};

	public static final KeyBindingHolder[] keys = new KeyBindingHolder[desc.length];

	public DSSKeyHandler() {
		this.mc = Minecraft.getMinecraft();
		for (int i = 0; i < desc.length; ++i) {
			KeyBinding key = null;
			if (Config.enableAdditionalControls() || i < KEY_ATTACK) {
				key = new KeyBinding("key.dss." + desc[i] + ".desc", keyValues[i], StatCollector.translateToLocal("key.dss.label"));
				ClientRegistry.registerKeyBinding(key);
			}
			keys[i] = new KeyBindingHolder(key);
		}
	}

	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {
		if (Keyboard.getEventKeyState()) {
			onKeyPressed(mc, Keyboard.getEventKey());
		} else {
			onKeyReleased(mc, Keyboard.getEventKey());
		}
	}

	/**
	 * Call for any key code, mouse or keyboard, to handle custom key bindings that may
	 * have been remapped to mouse. From MouseEvent, ONLY call this method when the mouse
	 * key is pressed, not when it is released (i.e. when event.buttonstate is true).
	 * @param mc	Pass in Minecraft instance, since this is a static method
	 * @param kb	The key code of the key pressed; for the mouse, this is the mouse button number minus 100
	 */
	public static void onKeyPressed(Minecraft mc, int kb) {
		if (mc.inGameHasFocus && mc.thePlayer != null) {
			DSSPlayerInfo skills = DSSPlayerInfo.get(mc.thePlayer);
			if (kb == keys[KEY_SKILL_ACTIVATE].getKeyCode()) {
				if (skills.hasSkill(SkillBase.swordBasic)) {
					PacketDispatcher.sendToServer(new ActivateSkillPacket(SkillBase.swordBasic, false));
				}
			} else if (kb == keys[KEY_SKILLS_GUI].getKeyCode()) {
				PacketDispatcher.sendToServer(new OpenGuiPacket(CommonProxy.GUI_SKILLS));
			} else {
				handleTargetingKeys(mc, kb, skills);
			}
		}
	}

	/**
	 * Call for any key code, mouse or keyboard, to handle custom key bindings that may
	 * have been remapped to mouse. From MouseEvent, ONLY call this method when the mouse
	 * key is released, not when it is pressed (i.e. when event.buttonstate is false).
	 * @param mc	Pass in Minecraft instance, since this is a static method
	 * @param kb	The key code of the key released; for the mouse, this is the mouse button number minus 100
	 */
	public static void onKeyReleased(Minecraft mc, int kb) {
		KeyBinding key = getKeyBindFromCode(mc, kb);
		if (key != null && mc.inGameHasFocus && mc.thePlayer != null) {
			DSSPlayerInfo.get(mc.thePlayer).onKeyReleased(mc, key);
			// Hack for custom block keybinding
			if (key.getKeyCode() == keys[KEY_BLOCK].getKeyCode()) {
				KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
			}
		}
	}

	/**
	 * All ILockOnTarget skill related keys are handled here
	 */
	private static void handleTargetingKeys(Minecraft mc, int kb, DSSPlayerInfo skills) {
		ILockOnTarget skill = skills.getTargetingSkill();
		boolean canInteract = skills.canInteract();

		if (skill == null || !skill.isLockedOn()) {
			return;
		}
		if (kb == keys[KEY_NEXT_TARGET].getKeyCode()) {
			skill.getNextTarget(mc.thePlayer);
		} else if (kb == keys[KEY_ATTACK].getKeyCode() || kb == mc.gameSettings.keyBindAttack.getKeyCode()) {
			KeyBinding key = (kb == keys[KEY_ATTACK].getKeyCode() ? keys[KEY_ATTACK].getKey() : mc.gameSettings.keyBindAttack);
			boolean canAttack = skills.canAttack();
			if (canInteract && canAttack) {
				KeyBinding.setKeyBindState(key.getKeyCode(), true);
			} else if (canAttack) {
				// hack for Super Spin Attack, as it requires key press to be passed while animation is in progress
				if (skills.isSkillActive(SkillBase.spinAttack)) {
					skills.getActiveSkill(SkillBase.spinAttack).keyPressed(mc, key, mc.thePlayer);
					return;
				} else if (skills.isSkillActive(SkillBase.backSlice)) {
					skills.getActiveSkill(SkillBase.backSlice).keyPressed(mc, key, mc.thePlayer);
					return;
				}
			}
			// Only allow attack key to continue processing if it was set to pressed
			if (key.isKeyDown()) {
				if (!skills.onKeyPressed(mc, key)) {
					DSSClientEvents.performComboAttack(mc, skill);
					// hack for Armor Break to begin charging without having to press attack again
					if (skills.hasSkill(SkillBase.armorBreak)) {
						skills.getActiveSkill(SkillBase.armorBreak).keyPressed(mc, key, mc.thePlayer);
					}
				}
			}
		} else if (canInteract) {
			// Only works for keys mapped to custom key bindings, which is fine for remapped mouse keys
			KeyBinding key = getKeyBindFromCode(mc, kb);
			if (key != null) {
				KeyBinding.setKeyBindState(kb, true);
				skills.onKeyPressed(mc, key);
			}
		}
	}

	/**
	 * Returns the KeyBinding corresponding to the key code given, or NULL if no key binding is found
	 * Currently handles all custom keys, plus the following vanilla keys:
	 * 	Always allowed: keyBindForward, keyBindJump
	 * 	{@link Config#allowVanillaControls}: keyBindLeft, keyBindRight, keyBindBack
	 * @param keyCode	Will be a negative number for mouse keys, or positive for keyboard
	 * @param mc		Pass in Minecraft instance as a workaround to get vanilla KeyBindings
	 */
	@SideOnly(Side.CLIENT)
	public static KeyBinding getKeyBindFromCode(Minecraft mc, int keyCode) {
		for (KeyBindingHolder k : keys) {
			if (k.getKeyCode() == keyCode) {
				return k.getKey();
			}
		}
		if (keyCode == mc.gameSettings.keyBindForward.getKeyCode()) {
			return mc.gameSettings.keyBindForward;
		} else if (keyCode == mc.gameSettings.keyBindJump.getKeyCode()) {
			return mc.gameSettings.keyBindJump;
		} else if (Config.allowVanillaControls()) {
			if (keyCode == mc.gameSettings.keyBindLeft.getKeyCode()) {
				return mc.gameSettings.keyBindLeft;
			} else if (keyCode == mc.gameSettings.keyBindRight.getKeyCode()) {
				return mc.gameSettings.keyBindRight;
			} else if (keyCode == mc.gameSettings.keyBindBack.getKeyCode()) {
				return mc.gameSettings.keyBindBack;
			}
		}
		return null;
	}
}
