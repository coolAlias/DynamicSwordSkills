package dynamicswordskills.client.gui;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.OpenGlHelper;

@SideOnly(Side.CLIENT)
public abstract class GuiButtonCustom extends GuiButton
{
	/** Default background image element */
	protected final ButtonBackgroundElement buttonElement;

	/** Flag to render the label text in unicode font */
	public final boolean isUnicode;

	/** Flag to draw the standard button background; if true, recommended height is 20 pixels */
	public boolean drawButtonBox;

	/** Flag to draw the text label */
	public boolean drawLabelText;

	/** Flag to draw the label text with a shadow */
	public boolean drawLabelShadow;

	public GuiButtonCustom(int id, int x, int y, int width, int height, String label) {
		this(id, x, y, width, height, label, true, false);
	}

	public GuiButtonCustom(int id, int x, int y, int width, int height, String label, boolean drawLabelShadow, boolean isUnicode) {
		super(id, x, y, width, height, label);
		this.drawLabelShadow = drawLabelShadow;
		this.isUnicode = isUnicode;
		this.buttonElement = new ButtonBackgroundElement(this);
	}

	/**
	 * Sets the {@link #drawButtonBox} flag
	 */
	public GuiButtonCustom setDrawButtonBox(boolean flag) {
		this.drawButtonBox = flag;
		return this;
	}

	/**
	 * Sets the {@link #drawLabelText} flag
	 */
	public GuiButtonCustom setDrawLabelText(boolean flag) {
		this.drawLabelText = flag;
		return this;
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY) {
		if (this.visible) {
			this.field_146123_n = (mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height);
			this.drawBackground(mc, mouseX, mouseY);
			this.mouseDragged(mc, mouseX, mouseY);
			this.drawLabelText(mc, mouseX, mouseY);
		}
	}

	/**
	 * Called from {@link #drawButton} if {@link #drawLabelText} is true to draw the label text;
	 * Default implementation draws the label text as a centered string if {@link #drawLabelText} is true.
	 */
	protected void drawLabelText(Minecraft mc, int mouseX, int mouseY) {
		if (this.drawLabelText) {
			boolean unicodeFlag = mc.fontRenderer.getUnicodeFlag();
			mc.fontRenderer.setUnicodeFlag(this.isUnicode);
			// Default button background has 2 pixels of border top and 3 bottom, so subtract 1 to compensate
			int dy = (this.height / 2) - (mc.fontRenderer.FONT_HEIGHT / 2) - 1;
			this.drawCenteredString(mc.fontRenderer, this.displayString, this.xPosition + this.width / 2, this.yPosition + dy, getLabelColor());
			mc.fontRenderer.setUnicodeFlag(unicodeFlag);
		}
	}

	/**
	 * Called when visible to draw the button background, if any.
	 * Default implementation draws the standard Minecraft button box if {@link #drawButtonBox} is true.
	 */
	protected void drawBackground(Minecraft mc, int mouseX, int mouseY) {
		if (this.drawButtonBox) {
			this.buttonElement.drawElement(mc, mouseX, mouseY);
		}
	}

	/**
	 * Draws a hovered gradient over the entire button
	 */
	protected void drawHoveredGradient(int gradientColorTop, int gradientColorBot) {
		this.buttonElement.drawHoveredGradient(gradientColorTop, gradientColorBot);
	}

	@Override
	public void drawCenteredString(FontRenderer fontRenderer, String text, int x, int y, int color) {
		this.drawString(fontRenderer, text, x - (fontRenderer.getStringWidth(text) / 2), y, color);
	}

	@Override
	public void drawString(FontRenderer fontRenderer, String text, int x, int y, int color) {
		if (this.drawLabelShadow) {
			fontRenderer.drawStringWithShadow(text, x, y, color);
		} else {
			fontRenderer.drawString(text, x, y, color);
		}
	}

	public final int getLabelColor() {
		if (this.packedFGColour != 0) {
			return this.packedFGColour;
		} else if (!this.enabled) {
			return this.getDisabledLabelColor();
		} else if (this.field_146123_n) {
			return this.getHoveredLabelColor();
		}
		return this.getDefaultLabelColor();
	}

	public int getDefaultLabelColor() {
		return 14737632;
	}

	public int getDisabledLabelColor() {
		return 10526880;
	}

	public int getHoveredLabelColor() {
		return 16777120;
	}

	public static class ButtonBackgroundElement extends GuiImageElement
	{
		protected final GuiButtonCustom parent;
		public ButtonBackgroundElement(GuiButtonCustom button) {
			super(button.xPosition, button.yPosition, button.width, button.height, buttonTextures, 256, 256, 0, 0);
			this.parent = button;
		}
		@Override
		protected void drawTexture(Minecraft mc, int mouseX, int mouseY, float f, int dx, int dy, int dw, int dh) {
			int i = this.parent.getHoverState(this.parent.field_146123_n);
			mc.getTextureManager().bindTexture(this.getTextureLocation());
			GL11.glEnable(GL11.GL_BLEND);
			OpenGlHelper.glBlendFunc(770, 771, 1, 0);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			int hw = this.width / 2;
			int ow = (hw * 2 < this.width ? 1 : 0);
			int ty = dy + (46 + i * 20);
			int by = Math.max(0, 2 - dy);
			// Draw top border for both halves if not scrolled past
			if (by > 0) {
				this.drawTexturedModalRect(this.xPos, this.yPos, 0, ty, hw + ow, by);
				this.drawTexturedModalRect(this.xPos + hw + ow, this.yPos, 200 - hw, ty, hw, by);
			}
			// Draw remainder for both halves
			ty += (20 - (this.height - by));
			this.drawTexturedModalRect(this.xPos, this.yPos + by, 0, ty, hw + ow, dh - by);
			this.drawTexturedModalRect(this.xPos + hw + ow, this.yPos + by, 200 - hw, ty, hw, dh - by);
		}
	}
}
