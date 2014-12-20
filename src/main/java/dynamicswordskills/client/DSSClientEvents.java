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

import java.util.Iterator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.ForgeSubscribe;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.DSSCombatEvents;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.lib.Config;
import dynamicswordskills.network.ActivateSkillPacket;
import dynamicswordskills.skills.ArmorBreak;
import dynamicswordskills.skills.ICombo;
import dynamicswordskills.skills.ILockOnTarget;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.skills.SpinAttack;
import dynamicswordskills.util.TargetUtils;

/**
 * 
 * Handles all client-sided events, such as render events, mouse event, etc.
 *
 */
@SideOnly(Side.CLIENT)
public class DSSClientEvents
{
	private final Minecraft mc;
	/** Store the current key code for mouse buttons */
	private int mouseKey;
	/** Whether the button during mouse event is Minecraft's keyBindAttack */
	private boolean isAttackKey;
	/** Whether the button during mouse event is Minecraft's keyBindUseItem*/
	private boolean isUseKey;

	public DSSClientEvents() {
		this.mc = Minecraft.getMinecraft();
	}

	/**
	 * Returns the KeyBinding corresponding to the key code given, or NULL if no key binding is found
	 * @param keyCode Will be a negative number for mouse keys, or positive for keyboard
	 */
	@SideOnly(Side.CLIENT)
	public static KeyBinding getKeyBindFromCode(int keyCode) {
		// Doesn't seem to be an easy way to get the KeyBinding from the key code...
		Iterator iterator = KeyBinding.keybindArray.iterator();
		while (iterator.hasNext()) {
			KeyBinding kb = (KeyBinding) iterator.next();
			if (kb.keyCode == keyCode) {
				return kb;
			}
		}
		return null;
	}

	/**
	 * Attacks current target if player is not currently using an item and {@link ICombo#onAttack}
	 * doesn't return false (i.e. doesn't miss)
	 * @param skill must implement BOTH {@link ILockOnTarget} AND {@link ICombo}
	 */
	@SideOnly(Side.CLIENT)
	public static void performComboAttack(Minecraft mc, ILockOnTarget skill) {
		if (!mc.thePlayer.isUsingItem()) {
			mc.thePlayer.swingItem();
			DSSCombatEvents.setPlayerAttackTime(mc.thePlayer);
			// TODO if (DynamicSwordSkills.isZeldaLoaded) { ZSSCombatEvents.setPlayerAttackTime(mc.thePlayer);
			if (skill instanceof ICombo && ((ICombo) skill).onAttack(mc.thePlayer)) {
				Entity entity = TargetUtils.getMouseOverEntity();
				mc.playerController.attackEntity(mc.thePlayer, (entity != null ? entity : skill.getCurrentTarget()));
			}
		}
	}

	/**
	 * Handles mouse clicks for skills, canceling where appropriate; note that left click will
	 * ALWAYS be canceled, as the attack is passed to {@link #performComboAttack(Minecraft, ILockOnTarget) performComboAttack};
	 * allowing left click results in the attack processing twice, doubling durability damage to weapons
	 * no button clicked -1, left button 0, right click 1, middle click 2, possibly 3+ for other buttons
	 * NOTE: Corresponding key codes for the mouse in Minecraft are (event.button -100)
	 */
	@ForgeSubscribe
	public void onMouseChanged(MouseEvent event) {
		mouseKey = event.button - 100;
		isAttackKey = (mouseKey == mc.gameSettings.keyBindAttack.keyCode);
		isUseKey = (mouseKey == mc.gameSettings.keyBindUseItem.keyCode);
		if ((event.button == -1 && event.dwheel == 0) || (!isAttackKey && !isUseKey)) {
			return;
		}
		EntityPlayer player = mc.thePlayer;
		DSSPlayerInfo skills = DSSPlayerInfo.get(player);
		ILockOnTarget skill = DSSPlayerInfo.get(player).getTargetingSkill();
		if (event.buttonstate || event.dwheel != 0) {
			if (skills.isSkillActive(SkillBase.mortalDraw)) {
				event.setCanceled(true);
			} else if (isAttackKey) {
				event.setCanceled(player.attackTime > 0);
			}
			/* TODO
			else if (DynamicSwordSkills.isZeldaLoaded) {
			if (event.button == 0) {
				Item heldItem = (player.getHeldItem() != null ? player.getHeldItem().getItem() : null);
				event.setCanceled(ZSSEntityInfo.get(player).isBuffActive(Buff.STUN) || heldItem instanceof ItemHeldBlock ||
						(player.attackTime > 0 && (Config.affectAllSwings() || heldItem instanceof ISwingSpeed)));
			} else if (event.button == 1) {
				event.setCanceled(ZSSEntityInfo.get(player).isBuffActive(Buff.STUN));
			}
			}
			 */
		} else if (!event.buttonstate && isAttackKey) {
			if (skills.hasSkill(SkillBase.armorBreak)) {
				((ArmorBreak) skills.getPlayerSkill(SkillBase.armorBreak)).keyPressed(player, false);
			}
		}

		if (event.isCanceled()) {
			return;
		}

		if (skill != null && skill.isLockedOn()) {
			if (isAttackKey && event.buttonstate) {
				if (!skills.canInteract()) {
					if (skills.isSkillActive(SkillBase.spinAttack)) {
						((SpinAttack) skills.getPlayerSkill(SkillBase.spinAttack)).keyPressed(mc.gameSettings.keyBindAttack, mc.thePlayer);
					}
					event.setCanceled(true);
					return;
				}
				if (Config.allowVanillaControls() && player.getHeldItem() != null) {
					if (skills.shouldSkillActivate(SkillBase.dash)) {
						PacketDispatcher.sendPacketToServer(new ActivateSkillPacket(SkillBase.dash).makePacket());
					} else if (skills.shouldSkillActivate(SkillBase.risingCut)) {
						PacketDispatcher.sendPacketToServer(new ActivateSkillPacket(SkillBase.risingCut).makePacket());
						performComboAttack(mc, skill);
					} // TODO swordBeam
					else if (skills.shouldSkillActivate(SkillBase.endingBlow)) {
						PacketDispatcher.sendPacketToServer(new ActivateSkillPacket(SkillBase.endingBlow).makePacket());
						performComboAttack(mc, skill);
					} else {
						performComboAttack(mc, skill);
					}
					// handle separately so can attack and begin charging without pressing key twice
					if (skills.hasSkill(SkillBase.armorBreak)) {
						((ArmorBreak) skills.getPlayerSkill(SkillBase.armorBreak)).keyPressed(player, true);
					}
				} else if (skills.shouldSkillActivate(SkillBase.mortalDraw)) {
					PacketDispatcher.sendPacketToServer(new ActivateSkillPacket(SkillBase.mortalDraw).makePacket());
					//event.setCanceled(true);
				} else {
					performComboAttack(mc, skill);
				}
				event.setCanceled(true);
			} else if (event.button == 1 && Config.allowVanillaControls()) {
				if (!skills.canInteract() && event.buttonstate) {
					event.setCanceled(true);
				}
			}
		} else { // regular left-click
			if (isAttackKey && event.buttonstate) {
				DSSCombatEvents.setPlayerAttackTime(player);
			}
		}
	}
}
