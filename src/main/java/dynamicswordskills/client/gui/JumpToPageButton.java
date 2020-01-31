package dynamicswordskills.client.gui;

import dynamicswordskills.client.gui.IGuiPageButton.PageButtonText;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
		return new JumpToPageTextElement(this, new TextComponentString(label));
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY) {
		super.drawButton(mc, mouseX, mouseY);
		this.drawLabelShadow = false;
		// Reset hovered state based on actual mouse coordinates to avoid shenanigans
		if (this.forceHover) {
			this.hovered = (mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height);
		}
	}

	@Override
	public void drawBackground(Minecraft mc, int mouseX, int mouseY) {
		if (this.forceHover) {
			this.hovered = true;
		}
		super.drawBackground(mc, mouseX, mouseY);
		this.drawLabelShadow = this.hovered;
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
		public JumpToPageTextElement(JumpToPageButton button, ITextComponent text) {
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
				mc.fontRenderer.drawStringWithShadow(s, (float)(x + this.width - w), (float)y, this.parent.getLabelColor());
				mc.fontRenderer.drawStringWithShadow(line, (float)(x + this.parent.getPadding().left), (float)y, this.parent.getLabelColor());
			} else {
				mc.fontRenderer.drawString(s, x + this.width - w, y, this.parent.getLabelColor());
				mc.fontRenderer.drawString(line, x + this.parent.getPadding().left, y, this.parent.getLabelColor());
			}
		}
	}
}
