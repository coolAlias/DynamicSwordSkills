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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.DSSCombatEvents;
import dynamicswordskills.client.gui.ComboOverlay;
import dynamicswordskills.client.gui.GuiEndingBlowOverlay;
import dynamicswordskills.client.gui.IGuiOverlay;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.server.ReachAttackSkillPacket;
import dynamicswordskills.skills.IComboSkill;
import dynamicswordskills.skills.IReachAttackSkill;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.DirtyEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

/**
 * 
 * Handles all client-sided events, such as render events, mouse event, etc.
 *
 */
@SideOnly(Side.CLIENT)
public class DSSClientEvents
{
	private final Minecraft mc;

	/** List of all GUI Overlays that may need rendering */
	private final List<IGuiOverlay> overlays = new ArrayList<IGuiOverlay>();

	/** List of GUI overlays that have rendered this tick */
	private final List<IGuiOverlay> rendered = new ArrayList<IGuiOverlay>();

	/** Accessible reference to {@code Minecraft#debugFPS */
	private static Field debugFPS;

	/** @return the current {@link #debugFPS} value */
	public static int getDebugFPS() {
		if (debugFPS == null) {
			debugFPS = ReflectionHelper.findField(Minecraft.class, "field_71470_ab", "debugFPS");
		}
		try {
			return debugFPS.getInt(Minecraft.getMinecraft());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 30;
	}

	public DSSClientEvents() {
		this.mc = Minecraft.getMinecraft();
		overlays.add(new ComboOverlay(mc));
		overlays.add(new GuiEndingBlowOverlay(mc));
	}

	@SubscribeEvent
	public void onRenderExperienceBar(RenderGameOverlayEvent.Post event) {
		if (event.type != RenderGameOverlayEvent.ElementType.EXPERIENCE) {
			return;
		}
		for (IGuiOverlay overlay : this.overlays) {
			if (overlay.shouldRender() && overlay.renderOverlay(event.resolution, this.rendered)) {
				this.rendered.add(overlay);
			}
		}
		this.rendered.clear();
	}

	/**
	 * Attacks the current mouse over entity or calls {@link #handlePlayerMiss} as appropriate.
	 * Modeled after Minecraft#clickMouse with a BLOCK hit being counted as a missed attack.
	 */
	public static void handlePlayerAttack(Minecraft mc) {
		if (mc.thePlayer.isUsingItem()) {
			return;
		}
		if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
			mc.playerController.attackEntity(mc.thePlayer, mc.objectMouseOver.entityHit);
		} else {
			handlePlayerMiss(mc);
		}
		mc.thePlayer.swingItem();
		DSSCombatEvents.setPlayerAttackTime(mc.thePlayer);
	}

	/**
	 * Call when a player attacked but did not hit an entity to call {@link IComboSkill#onMiss} if applicable
	 */
	public static void handlePlayerMiss(Minecraft mc) {
		IComboSkill combo = DSSPlayerInfo.get(mc.thePlayer).getComboSkill();
		if (combo != null) {
			combo.onMiss(mc.thePlayer);
		}
	}

	/**
	 * Equivalent of {@link PlayerControllerMP#attackEntity(EntityPlayer, Entity)} but sends a custom attack packet to bypass the hard-coded vanilla reach values.
	 * @param <T> The IReachAttackSkill type
	 * @param mc Current Minecraft player will be used as the attacker
	 * @param target The target to attack
	 * @param skill The IReachAttackSkill skill
	 */
	@SideOnly(Side.CLIENT)
	public static <T extends SkillBase & IReachAttackSkill> void attackEntity(Minecraft mc, Entity target, T skill) {
		DirtyEntityAccessor.syncCurrentPlayItem(mc.playerController);
		DSSClientEvents.multiAttack(mc, target, skill);
	}

	/**
	 * Use this method when attacking multiple entities e.g. in a loop.
	 * Be sure to call {@link DirtyEntityAccessor#syncCurrentPlayItem(PlayerControllerMP)} first.
	 * Same parameters as {@link DSSClientEvents#attackEntity(Minecraft, Entity, SkillBase)}.
	 */
	@SideOnly(Side.CLIENT)
	public static <T extends SkillBase & IReachAttackSkill> void multiAttack(Minecraft mc, Entity target, T skill) {
		PacketDispatcher.sendToServer(new ReachAttackSkillPacket(skill, target));
		mc.thePlayer.attackTargetEntityWithCurrentItem(target);
	}

	/**
	 * Passes mouse key presses and releases to the appropriate DSSKeyHandler method
	 * for skill usage and possibly event cancellation
	 */
	@SubscribeEvent
	public void onMouseChanged(MouseEvent event) {
		if ((event.button == -1 && event.dwheel == 0)) {
			return;
		} else if (event.dwheel != 0) {
			// Cancel mouse wheel while animations are in progress
			event.setCanceled(!DSSPlayerInfo.get(mc.thePlayer).canInteract());
		} else {
			// Corresponding KeyBinding key codes for mouse buttons are (event.button - 100)
			int mouseKey = event.button - 100;
			if (event.buttonstate) {
				event.setCanceled(DSSKeyHandler.onKeyPressed(mc, mouseKey));
			} else {
				DSSKeyHandler.onKeyReleased(mc, mouseKey);
			}
		}
	}
}
