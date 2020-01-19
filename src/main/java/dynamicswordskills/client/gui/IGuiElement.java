package dynamicswordskills.client.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

@SideOnly(Side.CLIENT)
public interface IGuiElement
{
	/**
	 * Implementation-dependent amount of {@link Padding} for this element, usually between its borders and its content
	 * @return Padding instance
	 */
	public Padding getPadding();

	/**
	 * Draw the gui element
	 * @param mc
	 * @param mouseX The actual X coordinate of the mouse on the screen, usually from {@link GuiScreen#drawScreen}
	 * @param mouseY The actual X coordinate of the mouse on the screen, usually from {@link GuiScreen#drawScreen}
	 */
	void drawElement(Minecraft mc, int mouseX, int mouseY);

}
