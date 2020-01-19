package dynamicswordskills.client.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;

/**
 * 
 * An element contained within a {@link IGuiElementScrollable scrollable} element
 *
 */
@SideOnly(Side.CLIENT)
public interface IGuiContainedElement extends IGuiElement
{
	/**
	 * Elements that could be displayed with sufficient scroll and remaining area should always return false;
	 * only return true for an element that is no longer displayable under any condition.
	 * @return True if this element should be ignored when drawing the screen and calculating content dimensions
	 */
	public boolean isElementExcluded();

	/**
	 * @return the total height of this element's content if it were displayed in its entirety, including any padding
	 */
	int getContentHeight();

	/**
	 * @return the total width of this element's content if it were displayed in its entirety, including any padding
	 */
	int getContentWidth();

	/**
	 * @return the height of this element based on the content that was actually drawn to the screen
	 */
	int getDisplayedHeight();

	/**
	 * @return the width of this element based on the content that was actually drawn to the screen
	 */
	int getDisplayedWidth();

	/**
	 * Return this element's current X position
	 */
	int getElementPosX();

	/**
	 * Return this element's current Y position
	 */
	int getElementPosY();

	/**
	 * Sets this element's position to the new coordinates
	 */
	void setElementPosition(int xPos, int yPos);

	/**
	 * Sets the remaining display height and width available to this element, in pixels.
	 * Should be called before each call to {@link #drawElement(Minecraft, int, int) draw} this element.
	 * @return true if the element has content that can be displayed, otherwise it will be assumed the
	 *         full content height or width was consumed and further elements may not be rendered
	 */
	boolean setRemainingDisplayArea(int width, int height);

	/**
	 * Sets the scrolled amount for both x and y, i.e. the amount of content to skip, in pixels.
	 * Should be called before each call to {@link #drawElement(Minecraft, int, int) draw} this element.
	 * @param scrollX The amount of content to skip on the x-axis; negative values should be ignored
	 * @param scrollY The amount of content to skip on the y-axis; negative values should be ignored
	 */
	void setScrolledAmount(int scrollX, int scrollY);

	/**
	 * Should be called by the parent container for all contained scrollable elements prior
	 * to making any calls to {@link #drawElement(Minecraft, int, int)}; this allows elements
	 * with dynamic content height or width to recalculate so the container can as well.
	 * This should be called each time the viewport changes, but may be called more frequently,
	 * so consider caching the results for the current viewport.
	 * @param width  The usable display width of the scrollable viewport
	 * @param height The usable display height of the scrollable viewport
	 */
	void setScrollableArea(int width, int height);

}
