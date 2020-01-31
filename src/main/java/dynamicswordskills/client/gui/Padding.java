package dynamicswordskills.client.gui;

/**
 * 
 * Class may be used to represent either internal or external padding.
 *
 */
public class Padding
{
	public final int top, right, bottom, left;

	/**
	 * Creates a an instance with equal padding on all sides
	 */
	public Padding(int amount) {
		this(amount, amount, amount, amount);
	}

	/**
	 * @param x Amount of padding for the left and right sides
	 * @param y Amount of padding for the top and bottom sides
	 */
	public Padding(int x, int y) {
		this(y, x, y, x);
	}

	public Padding(int top, int right, int bottom, int left) {
		this.top = top;
		this.right = right;
		this.bottom = bottom;
		this.left = left;
	}

	/**
	 * See {@link #add(int, int, int, int)}
	 * @param amount Amount of padding to add to all sides
	 */
	public Padding add(int amount) {
		return add(amount, amount, amount, amount);
	}

	/**
	 * See {@link #add(int, int, int, int)}
	 * @param x Amount of padding added to the left and right sides
	 * @param y Amount of padding added to the top and bottom sides
	 */
	public Padding add(int x, int y) {
		return add(y, x, y, x);
	}

	/**
	 * Returns a new instance with padding equal to the current instance plus the indicated amounts 
	 */
	public Padding add(int top, int right, int bottom, int left) {
		return new Padding(this.top + top, this.right + right, this.bottom + bottom, this.left + left);
	}

	/**
	 * @return The total amount of padding for both the top and bottom side
	 */
	public int height() {
		return this.top + this.bottom;
	}

	/**
	 * @return The total amount of padding for both the left and right side
	 */
	public int width() {
		return this.left + this.right;
	}
}
