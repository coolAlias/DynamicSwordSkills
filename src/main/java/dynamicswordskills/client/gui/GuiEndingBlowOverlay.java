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

package dynamicswordskills.client.gui;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.client.RenderHelperQ;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.EndingBlow;
import dynamicswordskills.skills.SkillActive;
import dynamicswordskills.skills.Skills;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

@SideOnly(Side.CLIENT)
public class GuiEndingBlowOverlay extends AbstractGuiOverlay
{
	/** Time at which the current combo first started displaying */
	private long displayStartTime;

	private static final ResourceLocation HUD_ICONS = new ResourceLocation(ModInfo.ID, "textures/gui/hud_icons.png");

	private static final int ICON_SIZE = 16;

	private int iconIndex;

	private String text;

	public GuiEndingBlowOverlay(Minecraft mc) {
		super(mc);
	}

	@Override
	public HALIGN getHorizontalAlignment() {
		return Config.endingBlowHudXAlign;
	}

	@Override
	public VALIGN getVerticalAlignment() {
		return Config.endingBlowHudYAlign;
	}

	@Override
	public boolean shouldRender() {
		if (Config.isSkillDisabled(this.mc.thePlayer, Skills.endingBlow) || Config.endingBlowHudDisplayTime < 1) {
			return false;
		}
		SkillActive skill = DSSPlayerInfo.get(mc.thePlayer).getActiveSkill(Skills.endingBlow);
		if (skill == null) {
			this.displayStartTime = 0;
		} else if (skill.canUse(this.mc.thePlayer)) {
			this.displayStartTime = Minecraft.getSystemTime();
			((EndingBlow) skill).skillResult = 0;
		} else if (((EndingBlow) skill).getLastActivationTime() < this.displayStartTime) {
			this.displayStartTime = 0; // unable to use and was not activated during this opportunity window
		}
		if (skill instanceof EndingBlow) {
			byte i = ((EndingBlow) skill).skillResult;
			this.iconIndex = (i < 0 ? 2 : i);
		}
		if (!Config.endingBlowHudResult && this.iconIndex != 0) {
			return false;
		}
		return ((Minecraft.getSystemTime() - this.displayStartTime) < Config.endingBlowHudDisplayTime);
	}

	@Override
	protected void setup(ScaledResolution resolution) {
		String textKey = Skills.endingBlow.getTranslationKey() + (this.iconIndex == 2 ? ".hud.failure" : (this.iconIndex == 1 ? "hud.success" : "hud.activate"));
		this.text = StatCollector.translateToLocal(textKey);
		this.height = (Config.endingBlowHudText ? this.mc.fontRenderer.FONT_HEIGHT : ICON_SIZE);
		this.width = (Config.endingBlowHudText ? this.mc.fontRenderer.getStringWidth(this.text) : ICON_SIZE);
		this.setPosX(resolution, Config.endingBlowHudXOffset);
		this.setPosY(resolution, Config.endingBlowHudYOffset);
	}

	@Override
	protected void render(ScaledResolution resolution) {
		if (Config.endingBlowHudText) {
			this.mc.fontRenderer.drawString(this.text, this.x, this.y, 0XFF0000, true);
		} else {
			GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
			GL11.glEnable(GL11.GL_BLEND);
			float r = this.iconIndex == 1 ? 0.0F : 1.0F;
			float g = this.iconIndex == 2 ? 0.0F : 1.0F;
			float b = 0.0F;
			GL11.glColor4f(r, g, b, 1.0F);
			this.mc.getTextureManager().bindTexture(HUD_ICONS);
			RenderHelperQ.drawTexturedRect(this.x, this.y, this.iconIndex * ICON_SIZE, 0, ICON_SIZE, ICON_SIZE, 256, 256);
			RenderHelperQ.drawTexturedRect(this.x, this.y, this.iconIndex * ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, 256, 256);
			GL11.glPopAttrib();
		}
	}
}
