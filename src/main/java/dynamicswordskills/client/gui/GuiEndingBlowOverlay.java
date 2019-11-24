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

import dynamicswordskills.client.RenderHelperQ;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.EndingBlow;
import dynamicswordskills.skills.SkillActive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiEndingBlowOverlay extends AbstractGuiOverlay
{
	/** Time at which the current combo first started displaying */
	private long displayStartTime;

	/** Length of time combo pop-up will display */
	private static final long DISPLAY_TIME = 1000;

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
		if (!Config.endingBlowHudEnabled) {
			return false;
		}
		SkillActive skill = DSSPlayerInfo.get(mc.thePlayer).getActiveSkill(SkillActive.endingBlow);
		if (skill == null) {
			this.displayStartTime = 0;
		} else if (skill.canUse(this.mc.thePlayer)) {
			this.displayStartTime = Minecraft.getSystemTime();
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
		return ((Minecraft.getSystemTime() - this.displayStartTime) < DISPLAY_TIME);
	}

	@Override
	protected void setup(ScaledResolution resolution) {
		String textKey = (this.iconIndex == 2 ? "dss.hud.endingblow.failure" : (this.iconIndex == 1 ? "dss.hud.endingblow.success" : "dss.hud.endingblow.activate"));
		this.text = new TextComponentTranslation(textKey).getUnformattedText();
		this.height = (Config.endingBlowHudText ? this.mc.fontRendererObj.FONT_HEIGHT : ICON_SIZE);
		this.width = (Config.endingBlowHudText ? this.mc.fontRendererObj.getStringWidth(this.text) : ICON_SIZE);
		this.setPosX(resolution, Config.endingBlowHudXOffset);
		this.setPosY(resolution, Config.endingBlowHudYOffset);
	}

	@Override
	protected void render(ScaledResolution resolution) {
		if (Config.endingBlowHudText) {
			this.mc.fontRendererObj.drawString(this.text, this.x, this.y, 0XFF0000, true);
		} else {
			GlStateManager.pushAttrib();
			GlStateManager.disableLighting();
			GlStateManager.enableAlpha();
			GlStateManager.enableBlend();
			float r = this.iconIndex == 1 ? 0.0F : 1.0F;
			float g = this.iconIndex == 2 ? 0.0F : 1.0F;
			float b = 0.0F;
			GlStateManager.color(r, g, b, 1.0F);
			this.mc.getTextureManager().bindTexture(HUD_ICONS);
			RenderHelperQ.drawTexturedRect(this.x, this.y, this.iconIndex * ICON_SIZE, 0, ICON_SIZE, ICON_SIZE, 256, 256);
			RenderHelperQ.drawTexturedRect(this.x, this.y, this.iconIndex * ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, 256, 256);
			GlStateManager.popAttrib();
		}
	}
}
