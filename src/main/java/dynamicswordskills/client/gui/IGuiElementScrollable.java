package dynamicswordskills.client.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiScreen;

/**
 * 
 * Interface for GUI elements with a scrollable viewport containing variable amounts of content.
 *
 */
@SideOnly(Side.CLIENT)
public interface IGuiElementScrollable extends IGuiElement
{
	/**
	 * Typical implementation is (total content height - padding height - viewport height);
	 * if the contained element type does not support partial display, round the max scroll
	 * amount up to the next whole line increment, including expected padding between elements.
	 * @return the maximum scroll amount required to view all content displayed vertically, in pixels
	 */
	public int getMaxScrollHeight();

	/**
	 * Typical implementation is (total content width - padding width - viewport width);
	 * if the contained element type does not support partial display, round the max scroll
	 * amount up to the next whole column increment, including expected padding between elements.
	 * @return the maximum scroll amount required to view all content displayed horizontally, in pixels
	 */
	public int getMaxScrollWidth();

	/**
	 * @param isHorizontal True if called from a horizontal scroll bar
	 * @return the scroll speed when controlled via the mouse wheel, usually the amount required to traverse a single element
	 */
	int getScrollableWheelSpeed(boolean isHorizontal);

	/**
	 * Whether this element's scroll amount can be controlled by mouse wheel based on the current mouse position
	 * @param mouseX The actual X coordinate of the mouse on the screen, usually from {@link GuiScreen#drawScreen}
	 * @param mouseY The actual X coordinate of the mouse on the screen, usually from {@link GuiScreen#drawScreen}
	 * @param isHorizontal true if checking from a horizontal scroll bar control
	 * @return usually true if the mouse is within this element's bounds
	 */
	boolean isScrollableByWheel(int mouseX, int mouseY, boolean isHorizontal);

	/**
	 * Whether this element requires a scroll control to display all of its content
	 * @param isHorizontal true if called from a horizontal scroll bar, usually indicating to check content width instead of height
	 * @return usually true if either {@link #getMaxScrollHeight()} or {@link #getMaxScrollWidth()} is greater than 0
	 */
	boolean isScrollControlRequired(boolean isHorizontal);

	/**
	 * Called from the scroll bar control to set the scroll progress on this element
	 * @param scroll Value from 0.0F to 1.0F
	 * @param isHorizontal True if called from a horizontal scroll bar, usually indicating to scroll the content's width instead of height
	 */
	void scrollElementTo(float scroll, boolean isHorizontal);

}
