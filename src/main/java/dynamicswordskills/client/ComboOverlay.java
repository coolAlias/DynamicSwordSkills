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

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.server.EndComboPacket;
import dynamicswordskills.ref.Config;
import dynamicswordskills.skills.Combo;
import dynamicswordskills.skills.ICombo;
import dynamicswordskills.skills.ILockOnTarget;
import dynamicswordskills.skills.SkillBase;

/**
 * 
 * Displays current Combo information in upper-left corner 
 * 
 */
@SideOnly(Side.CLIENT)
public class ComboOverlay extends Gui
{
	private final Minecraft mc;

	/** Combo to display will update as combo updates, should fade after some time */
	private Combo combo = null;

	/** Used to detect changes in the combo size */
	private int lastComboSize = 0;

	/** Time at which the current combo first started displaying */
	private long displayStartTime;

	/** Length of time combo pop-up will display */
	private static final long DISPLAY_TIME = 5000;

	/** Whether combo overlay should display */
	public static boolean shouldDisplay;

	public ComboOverlay() {
		super();
		this.mc = Minecraft.getMinecraft();
		shouldDisplay = Config.isComboHudEnabled();
	}

	@SubscribeEvent
	public void onRenderExperienceBar(RenderGameOverlayEvent.Post event) {
		if (event.type != ElementType.HOTBAR) {
			return;
		}
		DSSPlayerInfo skills = DSSPlayerInfo.get(mc.thePlayer);
		ICombo iCombo = skills.getComboSkill();
		if (iCombo != null && iCombo.getCombo() != null) {
			if (combo != iCombo.getCombo()) {
				combo = iCombo.getCombo();
				lastComboSize = combo.getSize();
				displayStartTime = Minecraft.getSystemTime();
				if (iCombo.getCombo().isFinished()) {
					iCombo.setCombo(null);
					PacketDispatcher.sendToServer(new EndComboPacket((SkillBase) iCombo));
				}
			}
		}

		if (combo != null && combo.getSize() > 0) {
			// combo has changed, reset time
			if (lastComboSize != combo.getSize()) {
				lastComboSize = combo.getSize();
				displayStartTime = Minecraft.getSystemTime();
			}
			if ((Minecraft.getSystemTime() - displayStartTime) < DISPLAY_TIME) {
				if (shouldDisplay) {
					String s = (combo.isFinished() ? (StatCollector.translateToLocal("combo.finished") + "! ") : (StatCollector.translateToLocal("combo.combo") + ": "));
					mc.fontRenderer.drawString(s + combo.getLabel(), 10, 10, combo.isFinished() ? 0x9400D3 : 0xEEEE00, true);
					mc.fontRenderer.drawString(StatCollector.translateToLocal("combo.size") + ": " + combo.getSize() + "/" + combo.getMaxSize(), 10, 20, 0xFFFFFF, true);
					mc.fontRenderer.drawString(StatCollector.translateToLocal("combo.damage") + ": " + String.format("%.1f",combo.getDamage()), 10, 30, 0xFFFFFF, true);
					List<Float> damageList = combo.getDamageList();
					for (int i = 0; i < damageList.size() && i < Config.getHitsToDisplay(); ++i) {
						mc.fontRenderer.drawString(" +" + String.format("%.1f",damageList.get(damageList.size() - i - 1)), 10, 40 + 10 * i, 0xFFFFFF, true);
					}
				}
				if (skills.canUseSkill(SkillBase.endingBlow)) {
					ICombo skill = skills.getComboSkill();
					ILockOnTarget target = skills.getTargetingSkill();
					if (skill != null && skill.isComboInProgress() && target != null && target.getCurrentTarget() == skill.getCombo().getLastEntityHit()) {
						mc.fontRenderer.drawString(StatCollector.translateToLocal("combo.ending"), (event.resolution.getScaledWidth() / 2) - 15, 30, 0xFF0000, true);
					}
				}
			}
		}
	}
}
