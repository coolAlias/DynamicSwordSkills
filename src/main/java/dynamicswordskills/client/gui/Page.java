package dynamicswordskills.client.gui;

import javax.annotation.Nullable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;

/**
 * 
 * A GUI element that contains a body element and optional header and / or footer elements.
 *
 * @param <T> The scrollable body element's contained element type
 */
@SideOnly(Side.CLIENT)
public class Page<T extends IGuiContainedElement> extends GuiElement
{
	/** The page index should generally match this element's index in an array or List, if any, and may be used as the page number */
	public final int index;

	public final GuiElementContainer<T> body;

	/** Optional header element, not considered part of the content */
	public final IGuiElement header;

	/** Optional header element, not considered part of the content */
	public final IGuiElement footer;

	/** Flag to control visibility of header element if it is present */
	public boolean isHeaderVisible;

	/** Flag to control visibility of footer element if it is present */
	public boolean isFooterVisible;

	public Page(int index, int xPos, int yPos, int width, int height, GuiElementContainer<T> body) {
		this(index, xPos, yPos, width, height, body, null, null);
	}

	public Page(int index, int xPos, int yPos, int width, int height, GuiElementContainer<T> body, IGuiElement header) {
		this(index, xPos, yPos, width, height, body, header, null);
	}

	public Page(int index, int xPos, int yPos, int width, int height, GuiElementContainer<T> body, @Nullable IGuiElement header, @Nullable IGuiElement footer) {
		super(xPos, yPos, width, height);
		this.index = index;
		this.body = body;
		this.header = header;
		this.isHeaderVisible = (header != null);
		this.footer = footer;
		this.isFooterVisible = (footer != null);
	}

	@Override
	public void drawElement(Minecraft mc, int mouseX, int mouseY) {
		if (this.header != null && this.isHeaderVisible) {
			this.header.drawElement(mc, mouseX, mouseY);
		}
		if (this.footer != null && this.isFooterVisible) {
			this.footer.drawElement(mc, mouseX, mouseY);
		}
		// Assume body may have hovering texts that may overlap header or footer and draw it last
		this.body.drawElement(mc, mouseX, mouseY);
	}
}
