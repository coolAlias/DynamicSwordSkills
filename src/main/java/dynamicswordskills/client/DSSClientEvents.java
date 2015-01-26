/**
    Copyright (C) <2015> <coolAlias>

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

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.MouseEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.DSSCombatEvents;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.ref.Config;
import dynamicswordskills.skills.ICombo;
import dynamicswordskills.skills.ILockOnTarget;
import dynamicswordskills.skills.SkillBase;
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
		DSSPlayerInfo skills = DSSPlayerInfo.get(mc.thePlayer);
		if (event.buttonstate || event.dwheel != 0) {
			if (isAttackKey) {
				// hack for spin attack: allows key press information to be received while animating
				if (skills.isSkillActive(SkillBase.spinAttack) && skills.getActiveSkill(SkillBase.spinAttack).isAnimating()) {
					skills.getActiveSkill(SkillBase.spinAttack).keyPressed(mc, mc.gameSettings.keyBindAttack, mc.thePlayer);
					event.setCanceled(true);
				} else if (skills.isSkillActive(SkillBase.backSlice) && skills.getActiveSkill(SkillBase.backSlice).isAnimating()) {
					skills.getActiveSkill(SkillBase.backSlice).keyPressed(mc, mc.gameSettings.keyBindAttack, mc.thePlayer);
					event.setCanceled(true);
				} else {
					event.setCanceled(!skills.canInteract() || mc.thePlayer.attackTime > 0);
				}
			} else { // cancel mouse wheel and use key while animations are in progress
				event.setCanceled(!skills.canInteract());
			}
		}
		if (event.isCanceled() || !event.buttonstate) {
			return;
		}

		ILockOnTarget skill = skills.getTargetingSkill();
		if (skill != null && skill.isLockedOn()) {
			if (isAttackKey) {
				// mouse attack will always be canceled while locked on, as the click has been handled
				if (Config.allowVanillaControls()) {
					if (!skills.onKeyPressed(mc, mc.gameSettings.keyBindAttack)) {
						// no skill activated - perform a 'standard' attack
						performComboAttack(mc, skill);
					}
					// hack for Armor Break: allows charging to begin without having to press attack key a second time
					if (skills.hasSkill(SkillBase.armorBreak)) {
						skills.getActiveSkill(SkillBase.armorBreak).keyPressed(mc, mc.gameSettings.keyBindAttack, mc.thePlayer);
					}
				}

				// if vanilla controls not enabled, mouse click is ignored (i.e. canceled)
				// if vanilla controls enabled, mouse click was already handled - cancel
				event.setCanceled(true);
			} else if (isUseKey && Config.allowVanillaControls()) {
				// is this case even possible?
				event.setCanceled(!skills.canInteract());
			}
		} else  if (isAttackKey) { // not locked on to a target, normal item swing: set attack time only
			DSSCombatEvents.setPlayerAttackTime(mc.thePlayer);
		}
	}
}
