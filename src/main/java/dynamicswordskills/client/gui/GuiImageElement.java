package dynamicswordskills.client.gui;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.client.RenderHelperQ;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

@SideOnly(Side.CLIENT)
public class GuiImageElement extends GuiContainedElement
{
	protected final ResourceLocation texture;

	/** The full dimensions of the actual texture file */
	protected final int imageHeight, imageWidth;

	/** The texture start coordinates */
	protected final int u, v;

	/** The dimensions of the texture to be drawn */
	protected final int textureHeight, textureWidth;

	/** Custom color profile */
	protected Color rgba = new Color(255, 255, 255, 255);

	/** The amount of scaling to apply to the image */
	protected float scale = 1.0F;

	/**
	 * Calls {@link #GuiImageElement(int, int, int, int, ResourceLocation, int, int, int, int, int, int)}
	 * using this element's width and height as the image file dimensions and texture size.
	 */
	public GuiImageElement(int xPos, int yPos, int width, int height, ResourceLocation texture, int u, int v) {
		this(xPos, yPos, width, height, texture, width, height, u, v, width, height);
	}

	/**
	 * Calls {@link #GuiImageElement(int, int, int, int, ResourceLocation, int, int, int, int, int, int)}
	 * using this element's width and height as the texture size.
	 */
	public GuiImageElement(int xPos, int yPos, int width, int height, ResourceLocation texture, int imageWidth, int imageHeight, int u, int v) {
		this(xPos, yPos, width, height, texture, imageWidth, imageHeight, u, v, width, height);
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
	public GuiImageElement(int xPos, int yPos, int width, int height, ResourceLocation texture, int imageWidth, int imageHeight, int u, int v, int textureWidth, int textureHeight) {
		super(xPos, yPos, width, height);
		this.texture = texture;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		this.u = u;
		this.v = v;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
	}

	/**
	 * Sets a custom color profile for this element; values should be between 0 and 255
	 */
	public GuiImageElement setColor(int r, int g, int b, int a) {
		this.rgba.set(r, g, b, a);
		return this;
	}

	/**
	 * Sets the scale to the specified value
	 * @param f Scale should generally be a factor of 2, e.g. 1.0F, 0.5F, 0.25F, 0.125F, etc.
	 */
	public GuiImageElement setScale(float f) {
		this.scale = f;
		return this;
	}

	/**
	 * Sets the scale automatically based on the texture height compared to the element's height
	 */
	public GuiImageElement autoScale() {
		this.scale = (float)this.height / (float)this.textureHeight;
		return this;
	}

	/**
	 * Allows sub-classes to return variable image locations - these must have the same texture size and coordinates!
	 * @return A null value will instead draw the default {@link #texture}
	 */
	protected ResourceLocation getTextureLocation() {
		return this.texture;
	}

	@Override
	public void setScrollableArea(int width, int height) {
		// no-op
	}

	@Override
	public void drawElement(Minecraft mc, int mouseX, int mouseY) {
		float f = (1.0F / this.scale);
		int dw = Math.round((float)Math.min(this.getDisplayedWidth(), this.remainingWidth) * f);
		int dh = Math.round((float)Math.min(this.getDisplayedHeight(), this.remainingHeight) * f);
		int dx = Math.round((float)this.scrollX * f);
		int dy = Math.round((float)this.scrollY * f);
		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glColor4f(this.rgba.getRed() / 255.0F, this.rgba.getGreen() / 255.0F, this.rgba.getBlue() / 255.0F, this.rgba.getAlpha() / 255.0F);
		GL11.glPushMatrix();
		GL11.glScalef(this.scale, this.scale, this.scale);
		this.drawTexture(mc, mouseX, mouseY, f, dx, dy, dw, dh);
		GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glPopAttrib();
	}

	/**
	 * Called from drawElement after setting up the GL state and factoring in scale and scroll amounts
	 * @param mc
	 * @param mouseX
	 * @param mouseY
	 * @param f  Inverted scale; this.xPos and yPos should be multiplied by this amount
	 * @param dx Scaled amount to add to {@link #u}
	 * @param dy Scaled amount to add to {@link #v}
	 * @param dw Scaled and scrolled width to render
	 * @param dh Scaled and scrolled height to render
	 */
	protected void drawTexture(Minecraft mc, int mouseX, int mouseY, float f, int dx, int dy, int dw, int dh) {
		ResourceLocation img = this.getTextureLocation();
		mc.renderEngine.bindTexture((img == null ? this.texture : img));
		RenderHelperQ.drawTexturedRect(this.xPos * f, this.yPos * f, this.u + dx, this.v + dy, dw, dh, this.imageWidth, this.imageHeight);
	}
}
