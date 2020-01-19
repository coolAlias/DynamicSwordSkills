package dynamicswordskills.client.gui;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

@SideOnly(Side.CLIENT)
public abstract class GuiElement extends Gui implements IGuiElement
{
	public int xPos, yPos;

	public final int width, height;

	/** Implementation-dependent amount of {@link Padding} for this element, usually between its borders and its content */
	protected Padding padding = new Padding(0);

	public GuiElement(int xPos, int yPos, int width, int height) {
		this.xPos = xPos;
		this.yPos = yPos;
		this.width = width;
		this.height = height;
	}

	@Override
	public Padding getPadding() {
		return this.padding;
	}

	/**
	 * Sets the {@link #padding} amount for this element
	 */
	public GuiElement pad(int amount) {
		this.padding = new Padding(amount);
		return this;
	}

	/**
	 * Sets the {@link #padding} amount for this element
	 * @param x Amount of padding for the left and right sides
	 * @param y Amount of padding for the top and bottom sides
	 */
	public GuiElement pad(int x, int y) {
		this.padding = new Padding(x, y);
		return this;
	}

	/**
	 * Sets the {@link #padding} amount for this element
	 */
	public GuiElement pad(int top, int right, int bottom, int left) {
		this.padding = new Padding(top, right, bottom, left);
		return this;
	}

	/**
	 * Draws a hovered gradient over the entire element
	 */
	public void drawHoveredGradient(int gradientColorTop, int gradientColorBot) {
		this.drawHoveredGradient(gradientColorTop, gradientColorBot, 0);
	}

	/**
	 * Draws a hovered gradient over the element less the specified padding amount
	 * @param padding The number of pixels by which to reduce the covered area; negative values will expand it instead
	 */
	public void drawHoveredGradient(int gradientColorTop, int gradientColorBot, int padding) {
		this.drawHoveredGradient(gradientColorTop, gradientColorBot, padding, padding);
	}

	/**
	 * Draws a hovered gradient over the element less the specified padding amounts
	 * @param padX The number of pixels by which to reduce the covered area horizontally; negative values will expand it instead
	 * @param padY The number of pixels by which to reduce the covered area vertically; negative values will expand it instead
	 */
	public void drawHoveredGradient(int gradientColorTop, int gradientColorBot, int padX, int padY) {
		this.drawHoveredGradient(gradientColorTop, gradientColorBot, this.xPos + padX, this.yPos + padY, this.xPos + this.width - padX, this.yPos + this.height - padY);
	}

	/**
	 * Draws a hovered gradient over the element less the specified padding amounts
	 * @param pad The amount by which to reduce the covered area on each side; negative values will expand it instead
	 */
	public void drawHoveredGradient(int gradientColorTop, int gradientColorBot, Padding pad) {
		this.drawHoveredGradient(gradientColorTop, gradientColorBot, this.xPos + pad.left, this.yPos + pad.top, this.xPos + this.width - pad.right, this.yPos + this.height - pad.bottom);
	}

	/**
	 * Draws a hovered gradient over the specified coordinates
	 */
	public void drawHoveredGradient(int gradientColorTop, int gradientColorBot, int x1, int y1, int x2, int y2) {
		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glColorMask(true, true, true, false);
		this.drawGradientRect(x1, y1, x2, y2, gradientColorTop, gradientColorBot);
		GL11.glColorMask(true, true, true, true);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glPopAttrib();
	}

	/**
	 * Returns true if the mouse coordinates are within this element's bounds
	 * @param mouseX The actual X coordinate of the mouse on the screen, usually from {@link GuiScreen#drawScreen}
	 * @param mouseY The actual X coordinate of the mouse on the screen, usually from {@link GuiScreen#drawScreen}
	 */
	public boolean isMouseOverElement(int mouseX, int mouseY) {
		return this.isMouseOverElement(mouseX, mouseY, 0, 0);
	}

	/**
	 * Returns true if the mouse coordinates are within this element's bounds
	 * @param mouseX The actual X coordinate of the mouse on the screen, usually from {@link GuiScreen#drawScreen}
	 * @param mouseY The actual X coordinate of the mouse on the screen, usually from {@link GuiScreen#drawScreen}
	 * @param borderX Border width to exclude (or expand) from the normal X bounds
	 * @param borderY Border width to exclude (or expand) from the normal Y bounds
	 */
	public boolean isMouseOverElement(int mouseX, int mouseY, int borderX, int borderY) {
		return this.isMouseOverElement(mouseX, mouseY, this.xPos + borderX, this.xPos + this.width - borderX, this.yPos + borderY, this.yPos + this.height - borderY);
	}

	/**
	 * Returns true if the mouse coordinates are within the specified coordinates
	 * @param mouseX The actual X coordinate of the mouse on the screen, usually from {@link GuiScreen#drawScreen}
	 * @param mouseY The actual X coordinate of the mouse on the screen, usually from {@link GuiScreen#drawScreen}
	 */
	public boolean isMouseOverElement(int mouseX, int mouseY, int x1, int x2, int y1, int y2) {
		return (mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2);
	}

	/**
	 * 
	 * Simple class for composite GUI elements, i.e. elements composed of multiple child elements.
	 *
	 */
	public static class GuiCompositeElement extends GuiElement
	{
		protected final List<IGuiElement> components;

		public GuiCompositeElement(int xPos, int yPos, int width, int height, IGuiElement... components) {
			super(xPos, yPos, width, height);
			this.components = (components == null ? Lists.<IGuiElement>newArrayList() : Lists.<IGuiElement>newArrayList(components));
		}

		@Override
		public void drawElement(Minecraft mc, int mouseX, int mouseY) {
			for (IGuiElement e : this.components) {
				e.drawElement(mc, mouseX, mouseY);
			}
		}
	}
}
