package dynamicswordskills.client.gui;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;

/**
 * 
 * A scrollable GUI element containing other elements.
 * 
 * The default {@link #drawElement(Minecraft, int, int)} implementation only supports vertical
 * scrolling and assumes each element is displayed on its own line.
 * 
 */
@SideOnly(Side.CLIENT)
public class GuiElementContainer<T extends IGuiContainedElement> extends GuiElement implements IGuiElementScrollable
{
	/** The main contents of this container */
	protected final List<T> elements = Lists.<T>newArrayList();

	/** The actual height required to display all content, see {@link #getContentHeight()} */
	protected int contentHeight;

	/** The actual width required to display all content, see {@link #getContentWidth()} */
	protected int contentWidth;

	/** The minimum height required to display a single contained element plus any padding between elements */
	protected int lineHeight;

	/** The minimum width required to display a single contained element plus any padding between elements */
	protected int lineWidth;

	/** Converts the vertical scroll bar amount into pixels based on the content and element height */
	protected int scrolledPixelsY;

	/** Converts the horizontal scroll bar amount into pixels based on the content and element width */
	protected int scrolledPixelsX;

	/** Internal horizontal and vertical padding between elements */
	protected int elementPadX, elementPadY;

	/** Flag indicating content dimensions should be recalculated */
	protected boolean dirty;

	public GuiElementContainer(int xPos, int yPos, int width, int height) {
		super(xPos, yPos, width, height);
		this.markDirty();
	}

	/**
	 * Sets the {@link #dirty} flag to true, forcing inner content dimensions to be recalculated next render cycle
	 */
	public void markDirty() {
		this.dirty = true;
	}

	/**
	 * @return the total height available for displaying content within this element
	 */
	public int getViewportHeight() {
		return this.height - this.padding.height();
	}

	/**
	 * @return the total width available for displaying content within this element
	 */
	public int getViewportWidth() {
		return this.width - this.padding.width();
	}

	@Override
	public int getScrollableWheelSpeed(boolean isHorizontal) {
		return Math.max(1, isHorizontal ? this.lineWidth : this.lineHeight);
	}

	@Override
	public boolean isScrollableByWheel(int mouseX, int mouseY, boolean isHorizontal) {
		return this.isMouseOverElement(mouseX, mouseY, 0, 0);
	}

	@Override
	public boolean isScrollControlRequired(boolean isHorizontal) {
		return (isHorizontal ? this.getMaxScrollWidth() > 0 : this.getMaxScrollHeight() > 0);
	}

	/**
	 * If true, {@link #getMaxScrollHeight()} will be rounded up to the next whole line height increment;
	 * this is recommended if the contained element does not support partial vertical display.
	 * @return Default return value is false
	 */
	protected boolean clampMaxScrollToLineHeight() {
		return false;
	}

	/**
	 * If true, {@link #getMaxScrollWidth()} will be rounded up to the next whole line width increment;
	 * this is recommended if the contained element does not support partial horizontal display.
	 * @return Default return value is false
	 */
	protected boolean clampMaxScrollToLineWidth() {
		return false;
	}

	@Override
	public int getMaxScrollHeight() {
		int i = this.contentHeight - this.padding.height() - this.getViewportHeight();
		if (i > 0 && this.lineHeight > 1 && this.clampMaxScrollToLineHeight()) {
			i = this.lineHeight * ((i + this.lineHeight - 1) / this.lineHeight);
		}
		return i;
	}

	@Override
	public int getMaxScrollWidth() {
		int i = this.contentWidth - this.padding.width() - this.getViewportWidth();
		if (i > 0 && this.lineWidth > 1 && this.clampMaxScrollToLineWidth()) {
			i = this.lineWidth * ((i + this.lineWidth - 1) / this.lineWidth);
		}
		return i;
	}

	@Override
	public void scrollElementTo(float scroll, boolean isHorizontal) {
		if (isHorizontal) {
			this.scrolledPixelsX = (int)(scroll * this.getMaxScrollWidth());
		} else {
			this.scrolledPixelsY = (int)(scroll * this.getMaxScrollHeight());
		}
	}

	/**
	 * Called from {@link #drawElement(Minecraft, int, int)} prior to drawing any elements
	 * to allow them to set up their content height etc. based on the current viewport.
	 */
	protected void setupScrollableContainer(Minecraft mc, int mouseX, int mouseY) {
		for (T e : this.elements) {
			e.setScrollableArea(this.getViewportWidth(), this.getViewportHeight());
		};
		if (this.dirty) {
			this.contentHeight = calculateContentHeight();
			this.contentWidth = calculateContentWidth();
			this.scrollElementTo(0, false);
			this.scrollElementTo(0, true);
			this.dirty = false;
		}
	}

	/**
	 * Sets the minimum width and height required to display a single element in its entirety, typically
	 * including any padding between elements.
	 * This ensures there is sufficient scroll space to display the last element.
	 * @param width A value of 1 or less uses the unmodified sum of the contained elements' content width
	 * @param height A value of 1 or less uses the unmodified sum of the contained elements' content height
	 */
	public void setContainedElementSize(int width, int height) {
		if (this.lineWidth != width) {
			this.lineWidth = width;
			this.contentWidth = this.calculateContentWidth();
		}
		if (this.lineHeight != height) {
			this.lineHeight = height;
			this.contentHeight = this.calculateContentHeight();
		}
	}

	/**
	 * Sets the internal padding between elements
	 * @param x The amount of padding between each column of elements
	 * @param y The amount of padding between each row of elements
	 * @return The current instance
	 */
	public GuiElementContainer<T> setElementPadding(int x, int y) {
		if (x != this.elementPadX) {
			this.markDirty();
			this.elementPadX = x;
		}
		if (y != this.elementPadY) {
			this.markDirty();
			this.elementPadY = y;
		}
		return this;
	}

	/**
	 * Called from setup when contentHeight is 0 to calculate the combined height of all contained elements.
	 * Default implementation assumes all elements are displayed vertically.
	 */
	protected int calculateContentHeight() {
		int i = 0;
		for (T e : this.elements) {
			if (!e.isElementExcluded()) {
				i += e.getContentHeight() + this.elementPadY;
			}
		}
		if (i > 0) {
			i -= this.elementPadY; // subtract padding for last element
		}
		return Math.max(this.height, this.padding.height() + i);
	}

	/**
	 * Called from setup when contentWidth is 0 to calculate the combined width of all contained elements.
	 * Default implementation assumes content fills the entire width of this element and does not extend beyond that.
	 */
	protected int calculateContentWidth() {
		return this.width;
	}

	@Override
	public void drawElement(Minecraft mc, int mouseX, int mouseY) {
		this.setupScrollableContainer(mc, mouseX, mouseY);
		final int baseY = this.yPos + this.padding.top;
		int dy = 0;
		int scrollY = this.scrolledPixelsY;
		for (int i = 0; i < this.elements.size() && dy < this.getViewportHeight(); ++i) {
			T e = this.elements.get(i);
			if (e.isElementExcluded()) {
				continue;
			}
			// Subtract full content height of each element until the remaining scroll is insufficient to display the entire element
			if (scrollY >= e.getContentHeight() + this.elementPadY) {
				scrollY -= e.getContentHeight() + this.elementPadY;
				continue;
			}
			if (!e.setRemainingDisplayArea(this.getViewportWidth(), this.getViewportHeight() - dy)) {
				dy += Math.max(0, e.getContentHeight() + this.elementPadY - Math.max(0, scrollY));
				continue;
			}
			e.setScrolledAmount(0, scrollY);
			scrollY -= e.getContentHeight() + this.elementPadY;
			// Apply left-padding to element posX
			e.setElementPosition(e.getElementPosX() + this.padding.left, baseY + dy);
			//e.setElementPosition(e.getElementPosX(), baseY + dy);
			this.drawElement(e, mc, mouseX, mouseY);
			dy += e.getDisplayedHeight() + (i < this.elements.size() - 1 ? this.elementPadY : 0);
			// Restore original element xPos to prevent the great migration
			e.setElementPosition(e.getElementPosX() - this.padding.left, e.getElementPosY());
		}
	}

	/**
	 * Expected to call {@link IGuiElement#drawElement(Minecraft, int, int)} on the contained element;
	 * the element's position and other such fields should not be modified at this time.
	 */
	protected void drawElement(T element, Minecraft mc, int mouseX, int mouseY) {
		element.drawElement(mc, mouseX, mouseY);
	}

	/**
	 * @return true if the element can be added to this container's contents
	 */
	public boolean canAdd(@Nullable T element) {
		return element != null;
	}

	/**
	 * Adds the element to this container's contents if allowed by {@link #canAdd(Object)}
	 * @return true if the element was added
	 */
	public boolean add(@Nullable T element) {
		if (this.canAdd(element) && this.elements.add(element)) {
			this.onElementAdded(element);
			return true;
		}
		return false;
	}

	/**
	 * Adds elements from the list until {@link #add(Object)} returns false or no elements remain
	 * @return The first object that could not be added or null if all elements were added
	 */
	public T addAll(List<T> elements) {
		for (T t : elements) {
			if (!this.add(t)) {
				return t;
			}
		}
		return null;
	}

	/**
	 * Called when an element is added to allow e.g. setting the element's position;
	 */
	protected void onElementAdded(@Nullable T element) {
	}

	/**
	 * @return true if this container is considered empty, usually if it has no contents
	 */
	public boolean isEmpty() {
		return this.elements.isEmpty();
	}

	/**
	 * @return an unmodifiable list of this elements contents
	 */
	public final List<T> getElements() {
		return Collections.unmodifiableList(this.elements);
	}

	/**
	 * 
	 * Container for text elements that calculates height based on lines of text
	 *
	 */
	public static class GuiTextElementContainer extends GuiElementContainer<GuiTextElement>
	{
		public GuiTextElementContainer(int xPos, int yPos, int width, int height) {
			super(xPos, yPos, width, height);
		}

		@Override
		public void setupScrollableContainer(Minecraft mc, int mouseX, int mouseY) {
			super.setupScrollableContainer(mc, mouseX, mouseY);
			this.setContainedElementSize(this.width, mc.fontRenderer.FONT_HEIGHT);
		}

		@Override
		public int getScrollableWheelSpeed(boolean isHorizontal) {
			return this.lineHeight;
		}
	}
}
