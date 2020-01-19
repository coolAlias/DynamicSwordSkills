package dynamicswordskills.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ButtonTextElement extends GuiTextElement
{
	protected final GuiContainedButton parent;

	protected int fontHeight = 9;

	public ButtonTextElement(GuiContainedButton button, IChatComponent text) {
		super(button.xPosition, button.yPosition, button.width, button.height, text, button.getDefaultLabelColor(), button.isUnicode);
		this.parent = button;
		this.isTextCentered = true;
		this.setElementPosition(this.xPos, this.yPos);
	}

	@Override
	public void setElementPosition(int xPos, int yPos) {
		this.xPos = xPos;
		// Default button background has 2 pixels of border top and 3 bottom, so subtract 1 to compensate
		int dy = (this.parent.height / 2) - (this.fontHeight / 2) - 1;
		this.yPos = yPos + dy;
	}

	@Override
	protected void drawLine(Minecraft mc, int mouseX, int mouseY, final String line, final int x, final int y, boolean hovered) {
		this.fontHeight = mc.fontRendererObj.FONT_HEIGHT;
		int dy = (this.parent.height / 2) - (this.fontHeight / 2) - 1;
		if (this.remainingHeight - dy < this.fontHeight) {
			return; // don't draw if insufficient space remains
		}
		if (this.parent.drawLabelShadow) {
			mc.fontRendererObj.drawStringWithShadow(line, (float)x, (float)y, this.parent.getLabelColor());
		} else {
			mc.fontRendererObj.drawString(line, x, y, this.parent.getLabelColor());
		}
	}
}
