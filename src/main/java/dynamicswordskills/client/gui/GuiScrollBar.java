package dynamicswordskills.client.gui;

import org.lwjgl.input.Mouse;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.MathHelper;

/**
 * 
 * Composed of two elements, the track and the ball, with the position controlled by the track.
 *
 */
@SideOnly(Side.CLIENT)
public class GuiScrollBar extends GuiElement
{
	public final IGuiElementScrollable parent;

	public final GuiElement track;

	public final GuiElement ball;

	/** Additional width, if any, provided to the track element by the ball for determining if the mouse is over the track */
	public final int ballOffset;

	/** True if the scroll bar control is horizontal i.e. moves along the x-axis */
	public final boolean isHorizontal;

	/** The maximum scroll amount based on the track and ball height */
	protected final float maxScroll;

	/** Current position of the scroll bar, as a float (0 is top, 1 is bottom) */
	protected float currentScroll;

	/** True if the scroll bar is being dragged */
	protected boolean isScrolling;

	/** Whether left mouse button is held down */
	protected boolean wasClicking;

	/** {@link Mouse#getEventDWheel} value from the last input tick */
	protected int wheel = 0;

	/**
	 * Creates a vertical scroll bar
	 */
	public GuiScrollBar(IGuiElementScrollable parent, GuiElement track, GuiElement ball) {
		this(parent, track, ball, false);
	}

	public GuiScrollBar(IGuiElementScrollable parent, GuiElement track, GuiElement ball, boolean isHorizontal) {
		this(parent, track, ball, isHorizontal, Math.min(track.xPos, ball.xPos), Math.min(track.yPos, ball.yPos), track.width, track.height);
	}

	/**
	 * Creates a scroll bar with additional elements to render and the specified width and height
	 * @param parent The element to be controlled by the scroll bar
	 * @param track The background element
	 * @param ball The movable element
	 * @param isHorizontal Whether the scroll bar control is horizontal i.e. moves along the x-axis
	 * @param width  Must be at least as wide as the track if horizontal, and at least as wide as the ball element
	 * @param height Must be at least as tall as the track if vertical, and at least as tall as the ball element
	 * @param width This element's actual width must be at least as wide as the widest of the ball or track
	 * @param height This element's actual height must be at least as tall as the track
	 */
	public GuiScrollBar(IGuiElementScrollable parent, GuiElement track, GuiElement ball, boolean isHorizontal, int xPos, int yPos, int width, int height) {
		super(xPos, yPos, Math.max(width, Math.max(track.width, ball.width)), Math.max(height, Math.max(track.height, ball.height)));
		this.parent = parent;
		this.track = track;
		this.ball = ball;
		this.isHorizontal = isHorizontal;
		if (this.isHorizontal) {
			this.ballOffset = Math.abs(ball.height - track.height) / 2;
			this.ball.xPos = this.track.xPos;
			this.ball.yPos = this.track.yPos - this.ballOffset;
			this.maxScroll = (float)(track.width - ball.width);
		} else {
			this.ballOffset = Math.abs(ball.width - track.width) / 2;
			this.ball.xPos = this.track.xPos - this.ballOffset;
			this.ball.yPos = this.track.yPos;
			this.maxScroll = (float)(track.height - ball.height);
		}
	}

	/**
	 * Returns the current scroll amount as a float value between 0.0F and 1.0F
	 */
	public float getScrollAmount() {
		return this.currentScroll;
	}

	/**
	 * Resets the current scroll amount
	 */
	public void resetScroll() {
		this.currentScroll = 0.0F;
	}

	/**
	 * Call this method from {@link GuiScreen#handleMouseInput} to enable scrolling via the mouse wheel
	 */
	public void handleMouseInput() {
		int i = Mouse.getEventDWheel();
		if (i != 0) {
			this.wheel = i;
		}
	}

	@Override
	public void drawElement(Minecraft mc, int mouseX, int mouseY) {
		this.updateScrollState(mouseX, mouseY);
		this.track.drawElement(mc, mouseX, mouseY);
		if (this.isHorizontal) {
			this.ball.xPos = this.track.xPos + (int)(this.currentScroll * this.maxScroll);
		} else {
			this.ball.yPos = this.track.yPos + (int)(this.currentScroll * this.maxScroll);
		}
		this.ball.drawElement(mc, mouseX, mouseY);
	}

	protected void updateScrollState(int mouseX, int mouseY) {
		boolean flag = Mouse.isButtonDown(0);
		if (this.isMouseOverElement(mouseX, mouseY) && !this.wasClicking && flag) {
			this.isScrolling = this.parent.isScrollControlRequired(this.isHorizontal);
		}
		if (!flag) {
			this.isScrolling = false;
		}
		this.wasClicking = flag;
		if (this.isScrolling) {
			float f = (float)(this.ball.height / 2);
			this.currentScroll = ((float)(mouseY - this.track.yPos) - f) / this.maxScroll;
			this.currentScroll = MathHelper.clamp_float(this.currentScroll, 0.0F, 1.0F);
		}
		if (this.wheel != 0 && this.parent.isScrollControlRequired(this.isHorizontal)) {
			this.handleMouseWheel(mouseX, mouseY);
		}
	}

	/**
	 * Default implementation assumes horizontal scroll bar controls the x-axis, and vertical the y-axis
	 * @return the amount of content that cannot be displayed in the allotted scrollable space
	 */
	protected int getMaxScroll() {
		return (this.isHorizontal ? this.parent.getMaxScrollWidth() : this.parent.getMaxScrollHeight());
	}

	/**
	 * Updates the scroll amount based on the last mouse wheel input
	 */
	protected void handleMouseWheel(int mouseX, int mouseY) {
		int dx = (this.isHorizontal ? 0 : -this.ballOffset);
		int dy = (this.isHorizontal ? -this.ballOffset : 0);
		if (this.track.isMouseOverElement(mouseX, mouseY, dx, dy) || this.parent.isScrollableByWheel(mouseX, mouseY, this.isHorizontal)) {
			int i = this.parent.getScrollableWheelSpeed(this.isHorizontal) * (this.wheel < 0 ? -1 : 1);
			int j = Math.max(this.getMaxScroll(), 1);
			this.currentScroll -= ((float)i / (float)j);
			this.currentScroll = MathHelper.clamp_float(this.currentScroll, 0.0F, 1.0F);
		}
		this.wheel = 0;
	}
}
