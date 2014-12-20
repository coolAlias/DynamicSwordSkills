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

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.MouseEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.DSSCombatEvents;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.lib.Config;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.bidirectional.ActivateSkillPacket;
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
	 * Attacks current target if player not currently using an item and {@link ICombo#onAttack}
	 * doesn't return false (i.e. doesn't miss)
	 * @param skill must implement BOTH {@link ILockOnTarget} AND {@link ICombo}
	 */
	@SideOnly(Side.CLIENT)
	public static void performComboAttack(Minecraft mc, ILockOnTarget skill) {
		if (!mc.thePlayer.isUsingItem()) {
			mc.thePlayer.swingItem();
			DSSCombatEvents.setPlayerAttackTime(mc.thePlayer);
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
	@SubscribeEvent
	public void onMouseChanged(MouseEvent event) {
		mouseKey = event.button - 100;
		isAttackKey = (mouseKey == mc.gameSettings.keyBindAttack.getKeyCode());
		isUseKey = (mouseKey == mc.gameSettings.keyBindUseItem.getKeyCode());
		if ((event.button == -1 && event.dwheel == 0)) {
			return;
		} else if ((!isAttackKey && !isUseKey)) {
			// pass mouse clicks to custom key handler when pressed, as KeyInputEvent no longer receives these
			if (event.buttonstate) {
				DSSKeyHandler.onKeyPressed(mc, mouseKey);
			}
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
						PacketDispatcher.sendToServer(new ActivateSkillPacket(SkillBase.dash));
					} else if (skills.shouldSkillActivate(SkillBase.risingCut)) {
						PacketDispatcher.sendToServer(new ActivateSkillPacket(SkillBase.risingCut));
						performComboAttack(mc, skill);
					} else if (skills.shouldSkillActivate(SkillBase.endingBlow)) {
						PacketDispatcher.sendToServer(new ActivateSkillPacket(SkillBase.endingBlow));
						performComboAttack(mc, skill);
					} else {
						performComboAttack(mc, skill);
					}
					// handle separately so can attack and begin charging without pressing key twice
					if (skills.hasSkill(SkillBase.armorBreak)) {
						((ArmorBreak) skills.getPlayerSkill(SkillBase.armorBreak)).keyPressed(player, true);
					}
				} else if (skills.shouldSkillActivate(SkillBase.mortalDraw)) {
					PacketDispatcher.sendToServer(new ActivateSkillPacket(SkillBase.mortalDraw));
				} else { // Vanilla controls not enabled simply attacks; handles possibility of being ICombo
					performComboAttack(mc, skill);
				}
				// always cancel left click to prevent weapons taking double durability damage
				event.setCanceled(true);
			} else if (isUseKey && Config.allowVanillaControls()) {
				if (!skills.canInteract() && event.buttonstate) {
					event.setCanceled(true);
				}
			}
		} else { // not locked on to a target, normal item swing
			if (isAttackKey && event.buttonstate) {
				DSSCombatEvents.setPlayerAttackTime(player);
			}
		}
	}
}
