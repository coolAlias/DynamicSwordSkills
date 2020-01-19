package dynamicswordskills.client.gui;

import java.util.List;

import com.google.common.collect.Lists;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;

/**
 * 
 * A scroll bar with additional gui elements to render
 *
 */
@SideOnly(Side.CLIENT)
public class GuiCompositeScrollBar extends GuiScrollBar
{
	protected final List<GuiElement> extra;

	/**
	 * Creates a vertical scroll bar with additional elements to render and the specified width and height
	 */
	public GuiCompositeScrollBar(IGuiElementScrollable parent, GuiElement track, GuiElement ball, int xPos, int yPos, int width, int height, GuiElement... elements) {
		this(parent, track, ball, false, xPos, yPos, width, height, elements);
	}

	/**
	 * Creates a scroll bar with additional elements to render and the specified width and height
	 */
	public GuiCompositeScrollBar(IGuiElementScrollable parent, GuiElement track, GuiElement ball, boolean isHorizontal, int xPos, int yPos, int width, int height, GuiElement... elements) {
		super(parent, track, ball, isHorizontal, xPos, yPos, width, height);
		this.extra = (elements == null ? Lists.<GuiElement>newArrayList() : Lists.newArrayList(elements));
	}

	@Override
	public void drawElement(Minecraft mc, int mouseX, int mouseY) {
		super.drawElement(mc, mouseX, mouseY);
		for (GuiElement e : this.extra) { 
			e.drawElement(mc, mouseX, mouseY);
		}
	}
}
