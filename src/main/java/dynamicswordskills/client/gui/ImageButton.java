package dynamicswordskills.client.gui;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.client.RenderHelperQ;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/**
 * 
 * A button that renders with a custom image texture and usually no label text
 *
 */
@SideOnly(Side.CLIENT)
public class ImageButton extends GuiButtonCustom
{
	protected final ResourceLocation texture;

	/** The full dimensions of the actual texture file */
	protected final int imageHeight, imageWidth;

	/** The texture start coordinates */
	protected final int u, v;

	/** The dimensions of the texture to be drawn */
	protected final int textureHeight, textureWidth;

	/** Flag to draw a colored gradient (i.e. highlight) when this button is hovered over */
	public boolean drawHoverGradient = true;

	public int gradientColorTop = -2130706433;

	public int gradientColorBot = -2130706433;

	/**
	 * Calls {@link #ImageButton(int, int, int, int, int, String, ResourceLocation, int, int, int, int, int, int)}
	 * using this element's width and height as the texture size.
	 */
	public ImageButton(int buttonId, int x, int y, int width, int height, String buttonText, ResourceLocation texture, int imageWidth, int imageHeight, int u, int v) {
		this(buttonId, x, y, width, height, buttonText, texture, imageWidth, imageHeight, u, v, width, height);
	}

	/**
	 * @param texture The location of the texture file to be rendered
	 * @param imageWidth  Full pixel width of the texture file
	 * @param imageHeight Full pixel height of the texture file
	 * @param u Texture x coordinate to start drawing from
	 * @param v Texture y coordinate to start drawing from
	 * @param textureWidth  The width of the texture to draw
	 * @param textureHeight The height of the texture to draw
	 */
	public ImageButton(int buttonId, int x, int y, int width, int height, String buttonText, ResourceLocation texture, int imageWidth, int imageHeight, int u, int v, int textureWidth, int textureHeight) {
		super(buttonId, x, y, width, height, buttonText);
		this.texture = texture;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		this.u = u;
		this.v = v;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
	}

	@Override
	public void drawBackground(Minecraft mc, int mouseX, int mouseY) {
		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.renderEngine.bindTexture(this.texture);
		RenderHelperQ.drawTexturedRect(this.xPosition, this.yPosition, this.u, this.v, this.textureWidth, this.textureHeight, this.imageWidth, this.imageHeight);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glPopAttrib();
		if (this.field_146123_n && this.enabled && this.drawHoverGradient) {
			this.drawHoveredGradient(this.gradientColorTop, this.gradientColorBot);
		}
	}
}
