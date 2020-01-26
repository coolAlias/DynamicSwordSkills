package dynamicswordskills.client.gui;

import javax.annotation.Nullable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.ref.Config;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;

/**
 * 
 * This element does not support padding.
 *
 */
@SideOnly(Side.CLIENT)
public class SkillSlot extends GuiContainedElement
{
	/** Flag set by parent container when this element is "selected"; effect may vary by implementation */
	public boolean selected;

	/** Flag set by parent container if the contained skill is known by the current player */
	public boolean isSkillKnown;

	/** The contained skill, if any */
	@Nullable
	protected final SkillBase skill;

	/** Frame around the icon */
	protected final GuiImageElement frame;

	/** Skill icon to render when {@link #isSkillKnown} is true */
	protected final GuiImageElement iconKnown;

	/** Skill icon to render when {@link #isSkillKnown} is false */
	protected final GuiImageElement iconUnknown;

	public SkillSlot(@Nullable SkillBase skill, int width, int height) {
		this(skill, 0, 0, width, height);
	}

	public SkillSlot(@Nullable SkillBase skill, int xPos, int yPos, int width, int height) {
		super(xPos, yPos, width, height);
		this.skill = skill;
		this.frame = new GuiImageElement(this.xPos, this.yPos, 18, 18, GuiSkills.GUI_TEXTURE, 300, 180, 281, 0);
		this.iconUnknown = new SkillIconElement(this.xPos + 1, this.yPos + 1, SkillBase.DEFAULT_ICON);
		if (this.skill != null) {
			int res = this.skill.getIconResolution();
			this.iconKnown = new SkillIconElement(this.xPos + 1, this.yPos + 1, this.skill.getIconLocation(), res, res);
		} else {
			this.iconKnown = new SkillIconElement(this.xPos + 1, this.yPos + 1, SkillBase.DEFAULT_ICON);
		}
	}

	@Override
	public void setElementPosition(int xPos, int yPos) {
		super.setElementPosition(xPos, yPos);
		this.frame.setElementPosition(xPos, yPos);
		// Add 1 to yPos of icons if the frame's top row is displayed
		int dy = this.scrollY < 1 ? 1 : 0;
		this.iconKnown.setElementPosition(xPos + 1, yPos + dy);
		this.iconUnknown.setElementPosition(xPos + 1, yPos + dy);
	}

	@Override
	public boolean setRemainingDisplayArea(int width, int height) {
		super.setRemainingDisplayArea(width, height);
		this.frame.setRemainingDisplayArea(width, height);
		// Subtract 1 from remaining height if the frame's top row is displayed
		int dy = this.scrollY < 1 ? 1 : 0;
		this.iconKnown.setRemainingDisplayArea(width - 1, height - dy);
		this.iconUnknown.setRemainingDisplayArea(width - 1, height - dy);
		return true;
	}

	@Override
	public void setScrolledAmount(int scrollX, int scrollY) {
		super.setScrolledAmount(scrollX, scrollY);
		this.frame.setScrolledAmount(scrollX, scrollY);
		// Subtract 1 from scrollY if the frame's top row is NOT displayed
		int dy = this.scrollY > 0 && this.scrollY < this.frame.height ? 1 : 0;
		this.iconKnown.setScrolledAmount(scrollX, scrollY - dy);
		this.iconUnknown.setScrolledAmount(scrollX, scrollY - dy);
	}

	@Override
	public void setScrollableArea(int width, int height) {
		// no-op
	}

	@Override
	public void drawElement(Minecraft mc, int mouseX, int mouseY) {
		boolean grayScale = Config.isSkillDisabled(mc.thePlayer, this.skill);
		int rgb = (grayScale ? 136 : 255);
		this.frame.setColor(rgb, rgb, rgb, 255);
		this.frame.drawElement(mc, mouseX, mouseY);
		if (this.isSkillKnown || this.skill.showIconIfUnknown(mc.thePlayer)) {
			this.iconKnown.setColor(rgb, rgb, rgb, 255);
			this.iconKnown.drawElement(mc, mouseX, mouseY);
		} else {
			this.iconUnknown.setColor(rgb, rgb, rgb, 255);
			this.iconUnknown.drawElement(mc, mouseX, mouseY);
		}
	}

	/**
	 * Draws a boxed border of the specified ARGB color around this element
	 * @param border The number of pixels of padding between this element and the border box
	 * @param color ARGB color value for the border
	 */
	public void drawBorderBox(int border, int color) {
		if (this.scrollY < 1) {
			this.drawHoveredGradient(color, color, this.xPos - border + 1, this.yPos - border, this.xPos + this.width + border, this.yPos - border + 1);
		}
		if (this.scrollY < this.height + this.getPadding().top) {
			int y1 = this.yPos - Math.max(border, border - this.scrollY);
			int y2 = this.yPos + this.getDisplayedHeight() + border;
			if (this.remainingHeight >= this.height + this.padding.top) {
				this.drawHoveredGradient(color, color, this.xPos - border + 1, y2 - 1, this.xPos + this.width + border, y2);
			} else {
				y2 = this.yPos + this.remainingHeight;
			}
			this.drawHoveredGradient(color, color, this.xPos - border, y1, this.xPos - border + 1, y2);
			this.drawHoveredGradient(color, color, this.xPos + this.width + border, y1, this.xPos + this.width + border + 1, y2);
		}
	}

	public static class SkillIconElement extends GuiImageElement
	{
		/**
		 * Standard 16x16 icon
		 */
		public SkillIconElement(int xPos, int yPos, ResourceLocation texture) {
			this(xPos, yPos, texture, 16, 16);
		}

		/**
		 * Icon with a custom resolution; imageWidth and imageHeight should be equal and a
		 * multiple of 16; texture width and height are assumed to be the full resolution.
		 */
		public SkillIconElement(int xPos, int yPos, ResourceLocation texture, int imageWidth, int imageHeight) {
			super(xPos, yPos, 16, 16, texture, imageWidth, imageHeight, 0, 0, imageWidth, imageHeight);
			if (imageWidth != 16) {
				this.autoScale();
			}
		}
	}

	public static class TitledSkillSlot extends SkillSlot
	{
		/** Title text when {@link #isSkillKnown} is true */
		protected final GuiTextElement titleKnown;

		/** Title text when {@link #isSkillKnown} is false */
		protected final GuiTextElement titleUnknown;

		public boolean isTitleVisible = true;

		public TitledSkillSlot(SkillBase skill, int width, int height) {
			this(skill, 0, 0, width, height);
		}

		public TitledSkillSlot(SkillBase skill, int xPos, int yPos, int width, int height) {
			super(skill, xPos, yPos, width, height);
			if (this.skill == null) {
				this.titleKnown = null;
				this.titleUnknown = null;
			} else {
				this.titleKnown = new GuiTextElement(xPos, yPos, this.width, this.height, new ChatComponentTranslation(this.skill.getNameTranslationKey()), GuiSkills.TEXT_COLOR, false).setHoverable(true).setHoverColor(GuiSkills.HOVER_COLOR).setDrawTextShadown(true);
				this.titleUnknown = new GuiTextElement(xPos, yPos, this.width, this.height, new ChatComponentTranslation("skill.dss.unknown.name"), GuiSkills.TEXT_COLOR, false);
			}
		}

		@Override
		public void setElementPosition(int xPos, int yPos) {
			super.setElementPosition(xPos, yPos);
			if (this.skill != null) {
				this.titleKnown.setElementPosition(xPos, yPos);
				this.titleUnknown.setElementPosition(xPos, yPos);
			}
		}

		@Override
		public boolean setRemainingDisplayArea(int width, int height) {
			super.setRemainingDisplayArea(width, height);
			if (this.skill != null) {
				this.titleKnown.setRemainingDisplayArea(width, height);
				this.titleUnknown.setRemainingDisplayArea(width, height);
			}
			return true;
		}

		@Override
		public void setScrolledAmount(int scrollX, int scrollY) {
			super.setScrolledAmount(scrollX, scrollY);
			if (this.skill != null) {
				this.titleKnown.setScrolledAmount(scrollX, scrollY);
				this.titleUnknown.setScrolledAmount(scrollX, scrollY);
			}
		}

		@Override
		public void drawElement(Minecraft mc, int mouseX, int mouseY) {
			if (this.isTitleVisible && this.skill != null) {
				GuiTextElement title = (this.isSkillKnown || this.skill.showNameIfUnknown(mc.thePlayer) ? this.titleKnown : this.titleUnknown);
				title.setHoverable(this.isSkillKnown);
				int ht = (title.getContentHeight() - title.getPadding().height()) / 2;
				int dy = (mc.fontRenderer.FONT_HEIGHT - ht);
				title.forceHover = this.selected;
				if (this.selected) {
					title.pad(dy, 0, dy - 1, 0); // hack to allow gradient across full width despite padding
					int c = (92 << 24);
					title.drawHoveredGradient(c, c, new Padding(-1, -2, -1, -1));
				}
				title.pad(dy, 0, dy - 1, 22);
				title.drawElement(mc, mouseX, mouseY);
			}
			super.drawElement(mc, mouseX, mouseY);
		}
	}
}
