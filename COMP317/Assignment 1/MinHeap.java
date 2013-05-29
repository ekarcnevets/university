/**
 * @author Steven Crake, 1117696
 */
public class MinHeap<T extends Comparable<? super T>>
{
	private T[] heapData; //our data set for the heap
	private int nextFree = 0; //next free index in heap
	private int maxSize = 0;
	
	@SuppressWarnings("unchecked")
	public MinHeap(int maxSize)
	{
		heapData = (T[]) new Comparable[maxSize];
		this.maxSize = maxSize;
	}
	
	/**
	 * @return the index for the left child of element at 'elemIndex'
	 */
	private int leftChild(int elemIndex)
	{
		return elemIndex * 2 + 1;
	}

	/**
	 * @return the index for the right child of element at 'elemIndex'
	 */
	private int rightChild(int elemIndex)
	{
		return elemIndex * 2 + 2;
	}

	/**
	 * @return the index for the parent of element at 'elemIndex'
	 */
	private int parent(int elemIndex)
	{
		return (elemIndex - 1) / 2;
	}
	
	/**
	 * Swaps an element down the heap until it is in heap order
	 */
	public void downHeap(int elemIndex)
	{
		int smallestChild = 0;
		
		if(elemExists(leftChild(elemIndex)) && elemExists(rightChild(elemIndex))) //if both child of node exist 
		{
			if(heapData[leftChild(elemIndex)].compareTo(heapData[rightChild(elemIndex)]) > 0) //tests to find the smallest child
			{
				smallestChild = rightChild(elemIndex); // sets child to be tested against parent
			}
			else
			{
				smallestChild = leftChild(elemIndex); // sets child to be tested against parent
			}
		}
		else if(elemExists(leftChild(elemIndex)) && !elemExists(rightChild(elemIndex))) //tests to see if right child doesnt exist
		{
			smallestChild = leftChild(elemIndex); // sets child to be tested against parent
		}
		else if(!elemExists(leftChild(elemIndex)) && elemExists(rightChild(elemIndex))) //tests to see if left child doesnt exist
		{
			smallestChild = rightChild(elemIndex); // sets child to be tested against parent
		}

		if(smallestChild > 0 && compareSwap(smallestChild, elemIndex))//tests child against parent
		{
			downHeap(smallestChild); // recursively tests next node
		}
	}
	
	/**
	 * Compares two elements and swaps them if necessary
	 * @return true if child and parent were swapped, false otherwise.
	 */
	private boolean compareSwap(int c, int p)
	{
		//System.out.println("Child: " + c + " parent: " + p);
		//if child exists and is less than its parent
		if(heapData[c] != null && heapData[c].compareTo(heapData[p]) < 0) 
		{
			swap(c, p); //swap child and parent
			return true;
		}
		else return false;
	}
	
	/**
	 * Swaps child and parent
	 */
	private void swap(int c, int p)
	{
		T tmp = heapData[c];
		heapData[c] = heapData[p];
		heapData[p] = tmp;
	}
	
	/**
	 * Swaps an element up the heap until it is in heap order
	 */
	private void upHeap(int elemIndex)
	{
		if(elemIndex != 0) //if its not the top of the tree
		{
			if(compareSwap(elemIndex,parent(elemIndex))) //compares child to parent
			{
				upHeap(parent(elemIndex)); //calls itself on the next node if required
			}
		}
	}
	
	/**
	 * Put heap in heap order
	 */
	private void heapify(int elemIndex)
	{
		upHeap(elemIndex);
		
		if(elemExists(leftChild(elemIndex)))
			heapify(leftChild(elemIndex));
		
		if(elemExists(rightChild(elemIndex)))
			heapify(rightChild(elemIndex));
	}
	
	/**
	 * Calls heapify() on root element
	 */
	public void heapify()
	{
		heapify(0);
	}
	
	private boolean elemExists(int elemIndex)
	{
		return elemIndex < maxSize && heapData[elemIndex] != null;
	}
    
	/**
	 * Returns the minimum usable value in the heap
	 */
    public T peek()
    {
    	return heapData[0];
	}
    
    /**
     * Removes the minimum element from the heap and returns it
     */
    public T removeMin()
    {
    	nextFree--;
    	T temp = heapData[0];
    	
    	heapData[0] = heapData[nextFree];
    	heapData[nextFree] = null;
    	
    	downHeap(0);
    	
    	return temp;
	}
    
    public boolean full()
    {
    	return (nextFree + 1 > maxSize);
    }
    
    /**
     * Inserts new data 
     */
    public boolean insert(T elem)
    {
    	if(nextFree + 1 > maxSize || elem == null)
    	{
    		return false;
    	}
    	else
    	{
	    	heapData[nextFree] = elem;
	    	upHeap(nextFree);
	    	nextFree++;
	    	return true;
    	}
	}
    
    /**
     * Deletes the element at the provided index
     */
    public void delete(int elemIndex)
    {
    	heapData[elemIndex] = heapData[nextFree - 1];
    	heapData[nextFree - 1] = null;

    	if(compareSwap(elemIndex, parent(elemIndex)))
    		upHeap(parent(elemIndex));
    	else
    		downHeap(elemIndex);
    	
    	nextFree--;
    }
    
    /**
     * Puts a currently unusable minimum value at the end of the heap
     */
    public void minNextRun()
    {
    	if(heapData[0] == null) //method call makes no sense
    		return;
    	else
    	{
    		T temp = removeMin();
    		maxSize--;
    		heapData[maxSize] = temp;
    	}
    }
    
    /**
     * Sets all elements in the heap as usable and part of the current run
     */
    @SuppressWarnings("unchecked")
	public void setAllThisRun()
    {
		maxSize = heapData.length;
		 
		T[] tempData = heapData;
		heapData = (T[]) new Comparable[tempData.length];
		 
		nextFree = heapData.length;
		 
		for(int i = 0; i < heapData.length; i++)
		{
			heapData[i] = tempData[heapData.length - i - 1];
			  
			if(heapData[i] == null)
			{
			nextFree = i;
			break;
			}
		}
		
		heapify();
    }
    
    /**
     * Returns the size of the usable heap
     */
    public int currentSize()
    {
    	return maxSize;
    }
    public int realSize()
    {
    	return nextFree;
    }
    
    /**
     * Prints the heap to standard out
     */
	public void printOut()
	{
    	String temp = "" + heapData[0];
    	
    	for(int x = 1; x < heapData.length; x++)
    	{
    		temp += "-" + heapData[x];
    	}
    	
    	System.out.println(temp);
    }
	
	/**
	 * Returns the elements of the heap as a string
	 */
	@Override
	public String toString()
	{
		String temp = "" + heapData[0];
    	
    	for(int x = 1; x < heapData.length; x++)
    	{
    		temp += "-" + heapData[x];
    	}
    	
    	return temp;
	}
}
