package dynamicswordskills.client.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/**
 * 
 * Paginating control button
 *
 */
@SideOnly(Side.CLIENT)
public interface IGuiPageButton
{
	/**
	 * Called when the button is clicked to determine the current page index
	 * @param current  Current page index
	 * @param numPages Total number of pages
	 * @return The current page index
	 */
	int getPageIndex(int current, int numPages);

	public static abstract class PageButtonImage extends ImageButton implements IGuiPageButton
	{
		public PageButtonImage(int id, int x, int y, int width, int height, String label, ResourceLocation texture, int imageWidth, int imageHeight, int u, int v) {
			this(id, x, y, width, height, label, texture, imageWidth, imageHeight, u, v, width, height);
		}
		public PageButtonImage(int id, int x, int y, int width, int height, String label, ResourceLocation texture, int imageWidth, int imageHeight, int u, int v, int textureWidth, int textureHeight) {
			super(id, x, y, width, height, label, texture, imageWidth, imageHeight, u, v, textureWidth, textureHeight);
		}
		@Override
		protected void drawLabelText(Minecraft mc, int mouseX, int mouseY) {
			if (this.drawLabelText) {
				this.drawCenteredString(mc.fontRenderer, this.displayString, this.xPosition + this.width / 2, getLabelY(), getLabelColor());
			}
		}
		/**
		 * @return the text label's yPos; default implementation assumes it displays beneath the image button
		 */
		protected int getLabelY() {
			return this.yPosition + this.textureHeight;
		}
	}

	public static abstract class PageButtonText extends GuiContainedButton implements IGuiPageButton
	{
		public PageButtonText(int id, int x, int y, int width, int height, String label, boolean drawLabelShadow, boolean isUnicode) {
			super(id, x, y, width, height, label, drawLabelShadow, isUnicode);
			this.drawButtonBox = false;
			this.drawLabelText = true;
		}
		@Override
		public void drawBackground(Minecraft mc, int mouseX, int mouseY) {
			super.drawBackground(mc, mouseX, mouseY);
			if (this.field_146123_n && this.enabled) {
				this.drawHoveredGradient(-2130706433, -2130706433);
			}
		}
	}
}
