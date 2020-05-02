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

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.CommonProxy;
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.ref.Config;
import dynamicswordskills.skills.ILockOnTarget;
import dynamicswordskills.skills.SkillActive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;

@SideOnly(Side.CLIENT)
public class DSSKeyHandler
{
	private final Minecraft mc;

	/** Key index for easy handling and retrieval of keys and key descriptions */
	public static final byte 
	KEY_SKILL_ACTIVATE = 0,
	KEY_NEXT_TARGET = 1,
	KEY_SKILLS_GUI = 2,
	KEY_FORWARD = 3,
	KEY_BACK = 4,
	KEY_LEFT = 5,
	KEY_RIGHT = 6;

	/** Key descriptions - this is what the player sees when changing key bindings in-game */
	private static final String[] desc = {
			"activate",
			"next",
			"skills_gui",
			"forward",
			"back",
			"left",
			"right"
	};

	/** Default key values */
	private static final int[] keyValues = {
			Keyboard.KEY_X,
			Keyboard.KEY_TAB,
			Keyboard.KEY_P,
			Keyboard.KEY_UP,
			Keyboard.KEY_DOWN,
			Keyboard.KEY_LEFT,
			Keyboard.KEY_RIGHT
	};

	public static final KeyBindingHolder[] keys = new KeyBindingHolder[desc.length];

	public DSSKeyHandler() {
		this.mc = Minecraft.getMinecraft();
		for (int i = 0; i < desc.length; ++i) {
			KeyBinding key = null;
			if (Config.enableAdditionalControls() || i < KEY_FORWARD) {
				key = new KeyBinding("key.dss." + desc[i] + ".desc", keyValues[i], StatCollector.translateToLocal("key.dss.label"));
				ClientRegistry.registerKeyBinding(key);
			}
			keys[i] = new KeyBindingHolder(key);
		}
	}

	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {
		if (Keyboard.getEventKeyState()) {
			if (!Keyboard.isRepeatEvent()) {
				onKeyPressed(mc, Keyboard.getEventKey());
			}
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
	 * @return true if the key press was 'handled'
	 */
	public static boolean onKeyPressed(Minecraft mc, int kb) {
		if (mc.inGameHasFocus && mc.thePlayer != null) {
			if (kb == keys[KEY_SKILLS_GUI].getKeyCode()) {
				mc.thePlayer.openGui(DynamicSwordSkills.instance, CommonProxy.GUI_SKILLS, mc.thePlayer.worldObj, (int) mc.thePlayer.posX, (int) mc.thePlayer.posY, (int) mc.thePlayer.posZ);
			} else {
				return handleSkillKeys(mc, kb);
			}
		}
		return false;
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
		}
	}

	private static boolean handleSkillKeys(Minecraft mc, int kb) {
		DSSPlayerInfo skills = DSSPlayerInfo.get(mc.thePlayer);
		ILockOnTarget lock = skills.getTargetingSkill();
		boolean canInteract = skills.canInteract();
		boolean isLockedOn = (lock != null && lock.isLockedOn());
		if (kb == keys[KEY_SKILL_ACTIVATE].getKeyCode()) {
			if (lock instanceof SkillActive && ((SkillActive) lock).isActive()) {
				skills.deactivateTargetingSkill();
			} else {
				skills.activateTargetingSkill();
			}
		} else if (kb == keys[KEY_NEXT_TARGET].getKeyCode()) {
			if (isLockedOn) {
				lock.getNextTarget(mc.thePlayer);
			}
		} else if (kb == mc.gameSettings.keyBindAttack.getKeyCode()) {
			if (!skills.canAttack()) {
				return true;
			} else if (!canInteract) {
				skills.onKeyPressedWhileAnimating(mc, mc.gameSettings.keyBindAttack);
				return true;
			} else if (skills.onKeyPressed(mc, mc.gameSettings.keyBindAttack)) {
				return true;
			}
			KeyBinding.setKeyBindState(kb, true);
			if (isLockedOn) {
				DSSClientEvents.handlePlayerAttack(mc);
			} else if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY) {
				DSSClientEvents.handlePlayerMiss(mc);
			}
			return isLockedOn;
		} else {
			KeyBinding key = getKeyBindFromCode(mc, kb);
			if (key != null) {
				if (!canInteract) {
					if (skills.canUseItem() || kb != mc.gameSettings.keyBindUseItem.getKeyCode()) {
						skills.onKeyPressedWhileAnimating(mc, key);
					}
					return true;
				} else if (skills.onKeyPressed(mc, key)) {
					return true;
				}
				KeyBinding.setKeyBindState(kb, true);
			}
		}
		return false;
	}

	/**
	 * Returns the KeyBinding corresponding to the key code given, or NULL if no key binding is found
	 * Certain vanilla keys may return null depending on config settings, see {@link #isVanillaControl(Minecraft, KeyBinding)}
	 * @param keyCode	Will be a negative number for mouse keys, or positive for keyboard
	 * @param mc		Pass in Minecraft instance as a workaround to get vanilla KeyBindings
	 */
	public static KeyBinding getKeyBindFromCode(Minecraft mc, int keyCode) {
		for (KeyBinding k : mc.gameSettings.keyBindings) {
			if (k.getKeyCode() == keyCode) {
				if (!Config.allowVanillaControls() && isVanillaControl(mc, k)) {
					return null;
				}
				return k;
			}
		}
		return null;
	}

	/**
	 * Returns whether the key usage is controlled by the Config#allowVanillaControls setting, i.e. WASD
	 */
	public static boolean isVanillaControl(Minecraft mc, KeyBinding key) {
		return (key == mc.gameSettings.keyBindLeft 
				|| key == mc.gameSettings.keyBindRight
				|| key == mc.gameSettings.keyBindForward
				|| key == mc.gameSettings.keyBindBack);
	}
}
