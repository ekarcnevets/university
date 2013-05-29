
public class Box implements Comparable<Box> 
{
	private int id;
	private int orientation;
	private int h; 
	private int w;
	private int d;
	
	public int getID() { return id; }
	public int getOrientation() { return orientation; }
	public int getH() { return h; }
	public int getW() { return w; }
	public int getD() { return d; }
	
	public Box(int id, int orientation, int h, int w, int d)
	{
		this.id = id;
		this.orientation = orientation;
		
		this.h = h;
		
		if(w <= d)
		{
			this.w = w;
			this.d = d;
		}
		else
		{
			this.w = d;
			this.d = w;
		}
	}
	
	/**
	 * Helper comparator function for sorting boxes by decreasing base area
	 * @param b The box to compare 'this' box to
	 * @return a positive number if Box 'b' has a larger base area,
	 * a negative number if this Box has a larger base area, 
	 * or 0 if they have the same base area.
	 */
	public int compareTo(Box b)
	{
		return (b.w * b.d) - (this.w * this.d); 
	}
}
