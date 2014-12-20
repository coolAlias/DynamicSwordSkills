/**
    Copyright (C) <2014> <coolAlias>

    This file is part of coolAlias' Zelda Sword Skills Minecraft Mod; as such,
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

import java.util.EnumSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.CommonProxy;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.inventory.ContainerSkills;
import dynamicswordskills.lib.Config;
import dynamicswordskills.network.ActivateSkillPacket;
import dynamicswordskills.network.OpenGuiPacket;
import dynamicswordskills.skills.ArmorBreak;
import dynamicswordskills.skills.Dodge;
import dynamicswordskills.skills.ILockOnTarget;
import dynamicswordskills.skills.Parry;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.skills.SpinAttack;
import dynamicswordskills.skills.SwordBreak;
import dynamicswordskills.util.PlayerUtils;

@SideOnly(Side.CLIENT)
public class DSSKeyHandler extends KeyHandler
{
	private final Minecraft mc;

	/** Key index for easy handling and retrieval of keys and key descriptions */
	public static final byte KEY_SKILL_ACTIVATE = 0, KEY_NEXT_TARGET = 1, KEY_ATTACK = 2,
			KEY_LEFT = 3, KEY_RIGHT = 4, KEY_DOWN = 5, KEY_BLOCK = 6, KEY_TOGGLE_AUTOTARGET = 7,
			KEY_SKILLS_GUI = 8;

	/** Key descriptions - this is what the player sees when changing key bindings in-game */
	public static final String[] desc = { "activate","next","attack","left","right","down",
		"block","toggleat","skills_gui"};

	/** Default key values */
	private static final int[] keyValues = {Keyboard.KEY_X, Keyboard.KEY_TAB, Keyboard.KEY_UP,
		Keyboard.KEY_LEFT, Keyboard.KEY_RIGHT, Keyboard.KEY_DOWN, Keyboard.KEY_RCONTROL,
		Keyboard.KEY_PERIOD, Keyboard.KEY_P};

	public static final KeyBinding[] keys = new KeyBinding[desc.length];

	/**
	 * Initializes keybindings and registers a new KeyHandler instance
	 */
	public static final void init() {
		boolean[] repeat = new boolean[desc.length];
		for (int i = 0; i < desc.length; ++i) {
			keys[i] = new KeyBinding("key.dss." + desc[i] + ".desc", keyValues[i]);
			repeat[i] = false;
		}
		KeyBindingRegistry.registerKeyBinding(new DSSKeyHandler(keys, repeat));
	}

	private DSSKeyHandler(KeyBinding[] keys, boolean[] repeat) {
		super(keys, repeat);
		this.mc = Minecraft.getMinecraft();
	}

	@Override
	public String getLabel() {
		return StatCollector.translateToLocal("key.dss.label");
	}

	@Override
	public EnumSet<TickType> ticks() {
		return EnumSet.of(TickType.CLIENT);
	}

	@Override
	public void keyDown(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd, boolean isRepeat) {
		if (tickEnd && mc.thePlayer != null) {
			DSSPlayerInfo skills = DSSPlayerInfo.get(mc.thePlayer);
			if (mc.inGameHasFocus && skills != null) {
				if (kb == keys[KEY_SKILL_ACTIVATE]) {
					if (skills.hasSkill(SkillBase.swordBasic)) {
						PacketDispatcher.sendPacketToServer(new ActivateSkillPacket(SkillBase.swordBasic).makePacket());
					}
				} else if (kb == keys[KEY_TOGGLE_AUTOTARGET]) {
					if (mc.thePlayer.isSneaking()) {
						mc.thePlayer.addChatMessage(StatCollector.translateToLocalFormatted("key.dss.togglept",
								(Config.toggleTargetPlayers() ? StatCollector.translateToLocal("key.dss.enable") : StatCollector.translateToLocal("key.dss.disable"))));
					} else {
						mc.thePlayer.addChatMessage(StatCollector.translateToLocalFormatted("key.dss.toggleat",
								(Config.toggleAutoTarget() ? StatCollector.translateToLocal("key.dss.enable") : StatCollector.translateToLocal("key.dss.disable"))));
					}
				} else if (kb == keys[KEY_SKILLS_GUI]) {
					PacketDispatcher.sendPacketToServer(new OpenGuiPacket(CommonProxy.GUI_SKILLS).makePacket());
				} else {
					handleTargetingKeys(mc, kb, skills);
				}
			} else if (kb == keys[KEY_SKILLS_GUI] && mc.thePlayer.openContainer instanceof ContainerSkills) {
				KeyBinding.setKeyBindState(kb.keyCode, false);
				mc.thePlayer.closeScreen();
			}
		}
	}

	@Override
	public void keyUp(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd) {
		if (tickEnd) {
			if (kb == keys[KEY_BLOCK]) {
				keys[KEY_BLOCK].pressed = false;
			} else if (kb == keys[KEY_ATTACK]) {
				keys[KEY_ATTACK].pressed = false;
				DSSPlayerInfo skills = DSSPlayerInfo.get(mc.thePlayer);
				if (skills.hasSkill(SkillBase.armorBreak)) {
					((ArmorBreak) skills.getPlayerSkill(SkillBase.armorBreak)).keyPressed(mc.thePlayer, false);
				}
			} else if (kb == keys[KEY_LEFT]) {
				keys[KEY_LEFT].pressed = false;
			} else if (kb == keys[KEY_RIGHT]) {
				keys[KEY_RIGHT].pressed = false;
			}
		}
	}

	/**
	 * All ILockOnTarget skill related keys are handled here
	 */
	private static void handleTargetingKeys(Minecraft mc, KeyBinding kb, DSSPlayerInfo skills) {
		ILockOnTarget skill = skills.getTargetingSkill();
		boolean canInteract = skills.canInteract();

		if (skill == null || !skill.isLockedOn()) {
			return;
		}
		if (kb == keys[KEY_NEXT_TARGET]) {
			skill.getNextTarget(mc.thePlayer);
		} else if (kb == keys[KEY_ATTACK]) {
			if (canInteract && mc.thePlayer.attackTime == 0) {
				keys[KEY_ATTACK].pressed = true;
			} else {
				if (skills.isSkillActive(SkillBase.spinAttack)) {
					((SpinAttack) skills.getPlayerSkill(SkillBase.spinAttack)).keyPressed(kb, mc.thePlayer);
				}
				return;
			}
			if (mc.thePlayer.getHeldItem() != null) {
				if (skills.shouldSkillActivate(SkillBase.dash)) {
					PacketDispatcher.sendPacketToServer(new ActivateSkillPacket(SkillBase.dash).makePacket());
				} else if (skills.shouldSkillActivate(SkillBase.risingCut)) {
					PacketDispatcher.sendPacketToServer(new ActivateSkillPacket(SkillBase.risingCut).makePacket());
					DSSClientEvents.performComboAttack(mc, skill);
				} else if (skills.shouldSkillActivate(SkillBase.endingBlow)) {
					PacketDispatcher.sendPacketToServer(new ActivateSkillPacket(SkillBase.endingBlow).makePacket());
					DSSClientEvents.performComboAttack(mc, skill);
				} else {
					DSSClientEvents.performComboAttack(mc, skill);
				}
				// handle separately so can attack and begin charging without pressing key twice
				if (skills.hasSkill(SkillBase.armorBreak)) {
					((ArmorBreak) skills.getPlayerSkill(SkillBase.armorBreak)).keyPressed(mc.thePlayer, true);
				}
			} else if (skills.shouldSkillActivate(SkillBase.mortalDraw)) {
				PacketDispatcher.sendPacketToServer(new ActivateSkillPacket(SkillBase.mortalDraw).makePacket());
			} else {
				DSSClientEvents.performComboAttack(mc, skill);
			}
		} else if (kb == keys[KEY_LEFT] || kb == keys[KEY_RIGHT]) {
			if (kb == keys[KEY_RIGHT]) {
				keys[KEY_RIGHT].pressed = true;
			} else {
				keys[KEY_LEFT].pressed = true;
			}
			if (canInteract) {
				if (skills.hasSkill(SkillBase.dodge) && mc.thePlayer.onGround) {
					((Dodge) skills.getPlayerSkill(SkillBase.dodge)).keyPressed(kb, mc.thePlayer);
				}
				if (skills.hasSkill(SkillBase.spinAttack)) {
					((SpinAttack) skills.getPlayerSkill(SkillBase.spinAttack)).keyPressed(kb, mc.thePlayer);
				}
			}
		} else if (kb == keys[KEY_DOWN] && canInteract) {
			if (PlayerUtils.isUsingItem(mc.thePlayer) && skills.hasSkill(SkillBase.swordBreak)) {
				((SwordBreak) skills.getPlayerSkill(SkillBase.swordBreak)).keyPressed(mc.thePlayer);
			} else if (skills.hasSkill(SkillBase.parry)) {
				((Parry) skills.getPlayerSkill(SkillBase.parry)).keyPressed(mc.thePlayer);
			}
		} else if (kb == keys[KEY_BLOCK] && canInteract) {
			keys[KEY_BLOCK].pressed = true;
		}
	}
}
