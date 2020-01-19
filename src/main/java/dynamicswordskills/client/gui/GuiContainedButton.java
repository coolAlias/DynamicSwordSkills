package dynamicswordskills.client.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

/**
 * 
 * A scrollable button that assumes the text fits entirely within the button's inner dimensions
 *
 */
@SideOnly(Side.CLIENT)
public class GuiContainedButton extends GuiButtonCustom implements IGuiContainedElement
{
	/** Implementation-dependent amount of {@link Padding} for this element, usually between its borders and its content */
	protected Padding padding = new Padding(0);

	/** Default label text element will display centered both horizontally and vertically */
	protected final GuiTextElement labelElement;

	/** Remaining height and width available for display */
	protected int remainingHeight, remainingWidth;

	/** Amount of content to skip based on scroll progress */
	protected int scrollX, scrollY;

	public GuiContainedButton(int id, int x, int y, int width, int height, String label) {
		this(id, x, y, width, height, label, true, false);
	}

	public GuiContainedButton(int id, int x, int y, int width, int height, String label, boolean drawLabelShadow, boolean isUnicode) {
		super(id, x, y, width, height, label, drawLabelShadow, isUnicode);
		this.labelElement = this.getButtonTextElement(label);
	}

	/**
	 * @return the GuiTextElement object to use for this button's label
	 */
	protected GuiTextElement getButtonTextElement(String label) {
		return new ButtonTextElement(this, new ChatComponentText(label));
	}

	@Override
	public Padding getPadding() {
		return this.padding;
	}

	/**
	 * Sets the {@link #padding} amount for this element
	 */
	public GuiContainedButton pad(int amount) {
		this.padding = new Padding(amount);
		return this;
	}

	/**
	 * Sets the {@link #padding} amount for this element
	 * @param x Amount of padding for the left and right sides
	 * @param y Amount of padding for the top and bottom sides
	 */
	public GuiContainedButton pad(int x, int y) {
		this.padding = new Padding(x, y);
		return this;
	}

	/**
	 * Sets the {@link #padding} amount for this element
	 */
	public GuiContainedButton pad(int top, int right, int bottom, int left) {
		this.padding = new Padding(top, right, bottom, left);
		return this;
	}

	@Override
	public boolean isElementExcluded() {
		return false;
	}

	@Override
	public int getContentHeight() {
		return this.height;
	}

	@Override
	public int getContentWidth() {
		return this.width;
	}

	@Override
	public int getDisplayedHeight() {
		return this.getContentHeight() - this.scrollY;
	}

	@Override
	public int getDisplayedWidth() {
		return this.getContentWidth() - this.scrollX;
	}

	@Override
	public int getElementPosX() {
		return this.xPosition;
	}

	@Override
	public int getElementPosY() {
		return this.yPosition;
	}

	@Override
	public void setElementPosition(int xPos, int yPos) {
		this.xPosition = xPos;
		this.yPosition = yPos;
		this.buttonElement.setElementPosition(xPos, yPos);
		this.labelElement.setElementPosition(xPos, yPos);
	}

	@Override
	public boolean setRemainingDisplayArea(int width, int height) {
		this.remainingHeight = height;
		this.remainingWidth = width;
		this.buttonElement.setRemainingDisplayArea(width, height);
		this.labelElement.setRemainingDisplayArea(width, height);
		return true;
	}

	@Override
	public void setScrolledAmount(int scrollX, int scrollY) {
		this.scrollX = Math.max(0, scrollX);
		this.scrollY = Math.max(0, scrollY);
		this.buttonElement.setScrolledAmount(scrollX, scrollY);
		this.labelElement.setScrolledAmount(scrollX, scrollY);
	}

	@Override
	public void setScrollableArea(int width, int height) {
		// Container will re-enable and set visible accordingly
		this.enabled = false;
		this.visible = false;
	}

	@Override
	public void drawElement(Minecraft mc, int mouseX, int mouseY) {
		super.drawButton(mc, mouseX, mouseY);
	}

	@Override
	protected void drawLabelText(Minecraft mc, int mouseX, int mouseY) {
		if (this.drawLabelText) {
			this.labelElement.drawElement(mc, mouseX, mouseY);
		}
	}

	/**
	 * Called from the container when displaying the button to determine if the button should
	 * be enabled or not; generally this should only be true if a the displayed portion of the
	 * element is sufficient for the user to understand the button's function if clicked.
	 * Default implementation enables the button if the text is visible.
	 * @return True to allow the button to be enabled
	 */
	public boolean canEnableDisplayedPortion() {
		int dy = (this.height / 2) - (9 / 2) - 1;
		return this.scrollY < 1 && this.remainingHeight - dy > 8;
	}

	public static class GuiButtonContainer extends GuiElementContainer<GuiContainedButton>
	{
		/** Offset amounts for translated screens */
		protected final int guiLeft, guiTop;

		public GuiButtonContainer(int xPos, int yPos, int width, int height, int lineHeight, int guiLeft, int guiTop) {
			super(xPos, yPos, width, height);
			this.lineHeight = lineHeight;
			this.guiLeft = guiLeft;
			this.guiTop = guiTop;
		}
		@Override
		public int getScrollableWheelSpeed(boolean isHorizontal) {
			return this.lineHeight + this.elementPadY;
		}
		@Override
		protected void drawElement(GuiContainedButton button, Minecraft mc, int mouseX, int mouseY) {
			// button.xPosition += this.guiLeft; // not needed if button's xPos already set properly
			button.yPosition += this.guiTop;
			button.enabled = button.canEnableDisplayedPortion();
			button.visible = true;
			// Assume the parent screen is already rendering visible buttons in GuiScreen#buttonList
		}
	}
}
