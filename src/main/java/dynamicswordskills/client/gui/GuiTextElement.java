package dynamicswordskills.client.gui;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 *
 * Text element displays a text component wrapped to fit inside this element's
 * allotted width and truncated to not exceed the displayed height.
 * 
 * The style of the top-level text component will be applied to the entire text;
 * does not support independent styling of child components at this time. 
 * 
 * New-line (i.e. "\n") characters in the text are interpreted accordingly.
 * 
 * Any padding is applied to the element as a whole, not individual lines.
 * 
 * Supports vertical scrolling.
 *
 */
@SideOnly(Side.CLIENT)
public class GuiTextElement extends GuiContainedElement
{
	/** The text component */
	public final ITextComponent text;

	/** The default text color */
	public final int color;

	/** Flag to render the text in unicode font instead of the default */
	public final boolean isUnicode;

	/** Flag to center text */
	public boolean isTextCentered;

	/** Optional flag to force the "hovered" state, i.e. the text will render according to #hoverColor and #drawTextShadow */
	public boolean forceHover;

	/** Flag to render text using the hoverColor and/or text shadow when hovered */
	public boolean isHoverable;

	/** The text color when hovered */
	public int hoverColor;

	/** Draws the text with a shadow when hovered */
	public boolean drawTextShadow;

	/** Lines of text determined during setup based on current font and element width */
	protected List<String> lines;

	/** Height if all lines were displayed */
	protected int contentHeight;

	/** Actual height and width of displayed content */
	protected int displayedHeight, displayedWidth;

	/**
	 * Constructs this element using the parent's position and size values
	 * @param text Raw text, will be converted to {@link TextComponentString}
	 * @param color The default text color
	 * @param isUnicode Flag to render the text in unicode font instead of the default
	 */
	public GuiTextElement(GuiElement parent, String text, int color, boolean isUnicode) {
		this(parent, new TextComponentString(text), color, isUnicode);
	}

	/**
	 * Constructs this element using the parent's position and size values
	 * @param text The text component
	 * @param color The default text color
	 * @param isUnicode Flag to render the text in unicode font instead of the default
	 */
	public GuiTextElement(GuiElement parent, ITextComponent text, int color, boolean isUnicode) {
		this(parent.xPos, parent.yPos, parent.width, parent.height, text, color, isUnicode);
	}

	public GuiTextElement(int xPos, int yPos, int width, int height, ITextComponent text, int color, boolean isUnicode) {
		super(xPos, yPos, width, height);
		this.text = text;
		this.color = color;
		this.isUnicode = isUnicode;
		this.remainingHeight = height;
		this.remainingWidth = width - this.padding.width();
	}

	/**
	 * Sets the {@link #isTextCentered} flag and returns the current instance
	 */
	public GuiTextElement setCentered(boolean flag) {
		this.isTextCentered = flag;
		return this;
	}

	/**
	 * Sets the {@link #isHoverable} flag and returns the current instance
	 */
	public GuiTextElement setHoverable(boolean flag) {
		this.isHoverable = flag;
		return this;
	}

	/**
	 * Sets the hover color and returns the current instance
	 */
	public GuiTextElement setHoverColor(int color) {
		this.hoverColor = color;
		return this;
	}

	/**
	 * Sets the {@link #drawTextShadow} flag and returns the current instance
	 */
	public GuiTextElement setDrawTextShadown(boolean flag) {
		this.drawTextShadow = flag;
		return this;
	}

	@Override
	public int getContentHeight() {
		return this.contentHeight + this.padding.height();
	}

	@Override
	public int getContentWidth() {
		return this.width;
	}

	@Override
	public int getDisplayedHeight() {
		return this.displayedHeight;
	}

	@Override
	public int getDisplayedWidth() {
		return this.displayedWidth;
	}

	@Override
	public void setScrollableArea(int width, int height) {
		this.remainingHeight = height; // Vertically scrollable - do not subtract padding height here
		this.remainingWidth = width - this.padding.width();
		if (this.lines == null) {
			Minecraft mc = Minecraft.getMinecraft();
			boolean unicodeFlag = mc.fontRenderer.getUnicodeFlag();
			mc.fontRenderer.setUnicodeFlag(this.isUnicode);
			this.lines = Lists.<String>newArrayList();
			String formatting = this.text.getStyle().getFormattingCode();
			Stream.of(this.text.getUnformattedText().split("\\\\n")).forEach(line -> {
				mc.fontRenderer.listFormattedStringToWidth(line, this.remainingWidth).stream().forEach(s -> {
					this.lines.add(formatting + s + TextFormatting.RESET);
				});
			});
			this.contentHeight = (this.lines.size() * mc.fontRenderer.FONT_HEIGHT);
			mc.fontRenderer.setUnicodeFlag(unicodeFlag);
		}
	}

	@Override
	public void drawElement(Minecraft mc, int mouseX, int mouseY) {
		if (this.lines == null) { // ensure element has been initialized properly
			this.setScrollableArea(this.width, this.height);
		}
		boolean unicodeFlag = mc.fontRenderer.getUnicodeFlag();
		mc.fontRenderer.setUnicodeFlag(this.isUnicode);
		int start = 0;
		int pY = this.padding.top;
		if (this.scrollY > 0) {
			pY = Math.max(0, pY - this.scrollY);
			this.scrollY -= this.padding.top;
			while (this.scrollY >= mc.fontRenderer.FONT_HEIGHT && start < this.lines.size()) {
				start++;
				this.scrollY -= mc.fontRenderer.FONT_HEIGHT;
			}
			// Account for partially scrolled line
			if (start < this.lines.size() && this.scrollY > 0 && this.scrollY < mc.fontRenderer.FONT_HEIGHT) {
				start++;
				pY += mc.fontRenderer.FONT_HEIGHT - this.scrollY;
				this.scrollY -= mc.fontRenderer.FONT_HEIGHT;
			}
			// If there is any remaining scroll, it will be taken from the bottom padding further below
		}
		this.displayedHeight = pY;
		this.displayedWidth = this.remainingWidth;
		int innerX = this.xPos + this.padding.left;
		GlStateManager.pushAttrib();
		GlStateManager.disableLighting();
		// Check if hovered over any displayed portion of this element
		int hoverHeight = Math.min(this.remainingHeight, this.padding.bottom + mc.fontRenderer.FONT_HEIGHT * (this.lines.size() - start));
		boolean hovered = this.isMouseOverElement(mouseX, mouseY, this.xPos, this.xPos + this.remainingWidth, this.yPos, this.yPos + pY + hoverHeight);
		for (int i = start; i < this.lines.size() && (this.displayedHeight + mc.fontRenderer.FONT_HEIGHT) <= this.remainingHeight; ++i) {
			String s = this.lines.get(i);
			int x = (this.isTextCentered ? innerX + ((this.remainingWidth - this.padding.width()) / 2) - (mc.fontRenderer.getStringWidth(s) / 2) : innerX);
			int y = this.yPos + this.displayedHeight;
			this.drawLine(mc, mouseX, mouseY, s, x, y, hovered);
			this.displayedHeight += mc.fontRenderer.FONT_HEIGHT;
		}
		this.displayedHeight += this.padding.bottom - Math.max(0, this.scrollY);
		GlStateManager.enableLighting();
		GlStateManager.popAttrib();
		mc.fontRenderer.setUnicodeFlag(unicodeFlag);
	}

	/**
	 * Draw the line of text at the specified coordinates
	 * @param mc Minecraft instance from the {@link #drawElement(Minecraft, int, int)} method
	 * @param mouseX Mouse X coordinate from the {@link #drawElement(Minecraft, int, int)} method
	 * @param mouseY Mouse Y coordinate from the {@link #drawElement(Minecraft, int, int)} method
	 * @param line The line of text to display should not be altered
	 * @param x Display coordinate for the line of text should not be altered
	 * @param y Display coordinate for the line of text should not be altered
	 * @param hovered True if the mouse is over any displayed portion of this element, including any padding
	 */
	protected void drawLine(Minecraft mc, int mouseX, int mouseY, final String line, final int x, final int y, boolean hovered) {
		if (this.forceHover) {
			if (this.drawTextShadow) {
				mc.fontRenderer.drawStringWithShadow(line, (float)x, (float)y, this.hoverColor);
			} else {
				mc.fontRenderer.drawString(line, x, y, this.hoverColor);
			}
		} else if (!hovered || !this.isHoverable) {
			mc.fontRenderer.drawString(line, x, y, this.color);
		} else if (this.drawTextShadow) {
			mc.fontRenderer.drawStringWithShadow(line, (float)x, (float)y, this.hoverColor);
		} else {
			mc.fontRenderer.drawString(line, x, y, this.hoverColor);
		}
	}

	/**
	 * Returns the text component with bold styling and the specified color, if any
	 */
	public static ITextComponent getBoldComponent(ITextComponent component, @Nullable TextFormatting color) {
		return component.setStyle(component.getStyle().setBold(true).setColor(color));
	}

	/**
	 * Returns the text component with italic styling and the specified color, if any
	 */
	public static ITextComponent getItalicComponent(ITextComponent component, @Nullable TextFormatting color) {
		return component.setStyle(component.getStyle().setItalic(true).setColor(color));
	}

	/**
	 * Returns the text component with strike-through styling and the specified color, if any
	 */
	public static ITextComponent getStrikethroughComponent(ITextComponent component, @Nullable TextFormatting color) {
		return component.setStyle(component.getStyle().setStrikethrough(true).setColor(color));
	}

	/**
	 * Returns the text component with underlined styling and the specified color, if any
	 */
	public static ITextComponent getUnderlinedComponent(ITextComponent component, @Nullable TextFormatting color) {
		return component.setStyle(component.getStyle().setUnderlined(true).setColor(color));
	}

	/**
	 * Returns the text component with styling per the method parameters
	 */
	public static ITextComponent getStyledComponent(ITextComponent component, @Nullable TextFormatting color, boolean bold, boolean italic, boolean underline, boolean strikethrough) {
		return component.setStyle(component.getStyle().setColor(color).setBold(bold).setItalic(italic).setUnderlined(underline).setStrikethrough(strikethrough));
	}
}
