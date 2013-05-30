/**
 * TallestStack.java
 * @date 19 May 2013
 * @author Steven Crake 1117696
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class TallestStack 
{
	private static List<Box> boxes;
	/**
	 * main method of The program to find the tallest possible stack from a list of boxes
	 * 
	 * This method reads in the box list from a file and 
	 */
	public static void main(String[] args) 
	{
		if(args.length != 1) //must pass in atleast one arg, a file path to a box list.
		{
			System.err.println("Usage: TallestStack [boxfile]");
			System.exit(1);
			return;
		}
		
		BufferedReader reader = null;
		
		try
		{
			//Initialize the reader to read in the file specified.
			reader = new BufferedReader(new FileReader(args[0]));
			boxes = new ArrayList<Box>();
			String line;
			int id = 0;
			
			//read each box line by line
			while((line = reader.readLine()) != null)
			{
				//get dimensions
				String[] dims = line.split(" ");
				Box b = new Box(++id, 1, Integer.parseInt(dims[0]), Integer.parseInt(dims[1]), Integer.parseInt(dims[2]));
				boxes.add(b);
			}
			reader.close();
			//Print out the tallest stack height
			System.out.println("The tallest stack possible has height '" + findTallestStack() + "'.");
		}
		catch(Exception ex)
		{
			System.err.println("Exception in TallestStack.main(). " + ex.getMessage());
			System.exit(1);
		}
	}
	
	/**
	 * Adds other two orientations of existing boxes to the list of boxes
	 */
	private static void addOtherOrientations()
	{
		int n = boxes.size();
		Box b = null;
		for(int i = 0; i < n; i++)
		{
			b = boxes.get(i);
			
			//second orientation
			boxes.add(new Box(b.getID(), 2, b.getW(), 
							  Math.max(b.getH(), b.getD()), 
							  Math.min(b.getH(), b.getD())));
			
			//third orientation
			boxes.add(new Box(b.getID(), 3, b.getD(), 
					  		  Math.max(b.getH(), b.getW()), 
					  		  Math.min(b.getH(), b.getW())));
		}
	}
	/**
	 * Computes the tallest stack of boxes given the constraints
	 * @return the height of the tallest stack.
	 */
	private static int findTallestStack()
	{
		addOtherOrientations();
		//sort by decreasing base area (w * d) using Box.compareTo(...) comparator
		Collections.sort(boxes);
		
		//maxStackHeight[i] is the maximum possible height of the stack if box i is on the top
		int[] maxStackHeight = new int[boxes.size()];
		String[] solutions = new String[boxes.size()];
		
		//initialise maxStackHeight for each box
		for(int i = 0; i < boxes.size(); i++){
			maxStackHeight[i] = boxes.get(i).getH();
			solutions[i] = "(" + boxes.get(i).getID() + "," + boxes.get(i).getOrientation() + ") ";
		}

		//Iterate through the sorted list of boxes from smallest to largest
		for(int i = 1; i < boxes.size(); i++)
		{
			//Iterate through the sorted list of boxes from smallest up to but not including the current box to add current(i-th) box 
			//to create the largest possible stack
			for(int j = 0; j < i; j++)
			{
				if(boxes.get(i).getW() < boxes.get(j).getW() 
						&& boxes.get(i).getD() < boxes.get(j).getD()  					//The base area of the box to add must be  
						&& maxStackHeight[i] < maxStackHeight[j] + boxes.get(i).getH()) //smaller than top of the current stack
				{
					maxStackHeight[i] = maxStackHeight[j] + boxes.get(i).getH();
					solutions[i] = solutions[j] + "(" + boxes.get(i).getID() + "," + boxes.get(i).getOrientation() + ") ";
				}
			}

		}
		
		//find the greatest maxStackHeight[i]
		int max = -1;
		int maxIndex = 0;
		for(int i = 0; i < boxes.size(); i++)
		{
			if(maxStackHeight[i] > max)
			{
				max = maxStackHeight[i];
				maxIndex = i;
			}
		}
		
		//output the string of tuples for optimum solution
		System.out.println(solutions[maxIndex]);
		
		return max;
	}
}
