package dynamicswordskills.client.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.client.gui.IGuiPageButton.PageButtonText;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

@SideOnly(Side.CLIENT)
public class JumpToPageButton extends PageButtonText
{
	/** The target page index */
	public final int pageIndex;

	/** Flag to force hovered state */
	public boolean forceHover;

	public JumpToPageButton(int id, int x, int y, int width, int height, String label, int pageIndex) {
		super(id, x, y, width, height, label, false, true);
		this.pageIndex = pageIndex;
	}

	@Override
	protected GuiTextElement getButtonTextElement(String label) {
		return new JumpToPageTextElement(this, new ChatComponentText(label));
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY) {
		super.drawButton(mc, mouseX, mouseY);
		this.drawLabelShadow = false;
		// Reset hovered state based on actual mouse coordinates to avoid shenanigans
		if (this.forceHover) {
			this.field_146123_n = (mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height);
		}
	}

	@Override
	public void drawBackground(Minecraft mc, int mouseX, int mouseY) {
		if (this.forceHover) {
			this.field_146123_n = true;
		}
		super.drawBackground(mc, mouseX, mouseY);
		this.drawLabelShadow = this.field_146123_n;
	}

	@Override
	public int getPageIndex(int current, int numPages) {
		return this.pageIndex;
	}

	@Override
	public int getDefaultLabelColor() {
		return (this.drawButtonBox ? super.getDefaultLabelColor() : GuiSkills.TEXT_COLOR);
	}

	public static class JumpToPageTextElement extends ButtonTextElement
	{
		public JumpToPageTextElement(JumpToPageButton button, IChatComponent text) {
			super(button, text);
			this.isTextCentered = false;
		}

		@Override
		protected void drawLine(Minecraft mc, int mouseX, int mouseY, final String line, final int x, final int y, boolean hovered) {
			this.fontHeight = mc.fontRenderer.FONT_HEIGHT;
			int dy = (this.parent.height / 2) - (this.fontHeight / 2) - 1;
			if (this.remainingHeight - dy < this.fontHeight) {
				return; // don't draw if insufficient space remains
			}
			String s = String.valueOf(((JumpToPageButton) this.parent).pageIndex + 1);
			int w = this.parent.getPadding().right + mc.fontRenderer.getStringWidth(s);
			if (this.parent.drawLabelShadow) {
				mc.fontRenderer.drawStringWithShadow(s, (x + this.width - w), y, this.parent.getLabelColor());
				mc.fontRenderer.drawStringWithShadow(line, (x + this.parent.getPadding().left),  y, this.parent.getLabelColor());
			} else {
				mc.fontRenderer.drawString(s, x + this.width - w, y, this.parent.getLabelColor());
				mc.fontRenderer.drawString(line, x + this.parent.getPadding().left, y, this.parent.getLabelColor());
			}
		}
	}
}
