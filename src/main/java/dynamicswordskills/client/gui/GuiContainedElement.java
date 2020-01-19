package dynamicswordskills.client.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 
 * Default contained element implementation assumes content size is the full height and width
 * of the element and this content is perfectly scrollable, i.e. display height or width is
 * the content height or width less the corresponding scroll amount.
 *
 */
@SideOnly(Side.CLIENT)
public abstract class GuiContainedElement extends GuiElement implements IGuiContainedElement
{
	/** Remaining height and width available for display */
	protected int remainingHeight, remainingWidth;

	/** Amount of content to skip based on scroll progress */
	protected int scrollX, scrollY;

	/** Flag indicating whether the element is disabled; see {@link IGuiContainedElement#isElementExcluded()} */
	protected boolean disabled = false;

	public GuiContainedElement(int xPos, int yPos, int width, int height) {
		super(xPos, yPos, width, height);
		this.remainingHeight = height;
		this.remainingWidth = width;
	}

	@Override
	public boolean isElementExcluded() {
		return this.disabled;
	}

	/**
	 * Sets this element's {@link #disabled} flag; container content dimensions should be recalculated if this changes
	 */
	public void setDisabled(boolean flag) {
		this.disabled = flag;
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
		return this.xPos;
	}

	@Override
	public int getElementPosY() {
		return this.yPos;
	}

	@Override
	public void setElementPosition(int xPos, int yPos) {
		this.xPos = xPos;
		this.yPos = yPos;
	}

	@Override
	public boolean setRemainingDisplayArea(int width, int height) {
		this.remainingHeight = height;
		this.remainingWidth = width;
		return true;
	}

	@Override
	public void setScrolledAmount(int scrollX, int scrollY) {
		this.scrollX = Math.max(0, scrollX);
		this.scrollY = Math.max(0, scrollY);
	}

	@Override
	public void drawHoveredGradient(int gradientColorTop, int gradientColorBot, int padX, int padY) {
		this.drawHoveredGradient(gradientColorTop, gradientColorBot, new Padding(padY, padX, padY, padX));
	}

	@Override
	public void drawHoveredGradient(int gradientColorTop, int gradientColorBot, Padding pad) {
		int x1 = this.xPos + (this.scrollX > 0 ? Math.max(0, pad.left - this.scrollX) : pad.left);
		boolean fx = this.remainingWidth >= this.width + pad.left;
		int x2 = this.xPos - Math.max(0, this.scrollX) + Math.min(this.remainingWidth - (fx ? pad.right : 0), this.width - pad.right);
		int y1 = this.yPos + (this.scrollY > 0 ? Math.max(0, pad.top - this.scrollY) : pad.top);
		boolean fy = this.remainingHeight >= this.height + pad.top;
		int y2 = this.yPos - Math.max(0, this.scrollY) + Math.min(this.remainingHeight - (fy ? pad.bottom : 0), this.height - pad.bottom);
		if (x1 < x2 && y1 < y2) {
			this.drawHoveredGradient(gradientColorTop, gradientColorBot, x1, y1, x2, y2);
		}
	}

	@Override
	public boolean isMouseOverElement(int mouseX, int mouseY, int borderX, int borderY) {
		if (this.disabled) {
			return false;
		}
		int x1 = this.xPos + (this.scrollX > 0 ? Math.max(0, borderX - this.scrollX) : borderX);
		boolean fx = this.remainingWidth >= this.width + borderX;
		int x2 = this.xPos - Math.max(0, this.scrollX) + Math.min(this.remainingWidth - (fx ? borderX : 0), this.width - borderX);
		int y1 = this.yPos + (this.scrollY > 0 ? Math.max(0, borderY - this.scrollY) : borderY);
		boolean fy = this.remainingHeight >= this.height + borderY;
		int y2 = this.yPos - Math.max(0, this.scrollY) + Math.min(this.remainingHeight - (fy ? borderY : 0), this.height - borderY);
		return this.isMouseOverElement(mouseX, mouseY, x1, x2, y1, y2);
	}
}
