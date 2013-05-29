import java.io.*;

/**
 * @author Steven Crake, 1117696
 */
public class xsort
{
	//Options
	private static String inputFilename;									//path to input file
	private static String outputFilename;									//path to output file
	private static String tmpDir = System.getProperty("java.io.tmpdir");	//path to system temporary directory
	private static int maxHeapSize = 7;										//the maximum size of the heap for R.S. and merging
	
	//Other
	private static final String EOR = (char)0x03 + "";			//char denoting end of run
	private static final String tmpDirName = "317sjc59sds14";	//name of temporary directory to store temporary files
	
	//Performance Metrics
	private static int numRuns = 0;
	private static int numPasses = 0;
	
	//'Tapes' - temporary files
	private static File[] tempFiles;
	private static File initialRunsFile;

	public static void main(String[] args)
	{
		//parse arguments
		for (int i = 0; i < args.length; i++)
		{
			if(args[i].startsWith("-"))
			{
				if(args[i].equals("-hs")) //heap size
				{
					try
					{
						maxHeapSize = Integer.parseInt(args[i + 1]);
						
						if(maxHeapSize < 2)
						{
							System.err.println("Heap size must be at least 2 or larger.");
							System.exit(1);
						}
					}
					catch(NumberFormatException e)
					{
						System.err.println("Invalid heap size provided. Using default of 7.");
						maxHeapSize = 7;
					}
				}
				else if(args[i].equals("-t")) //temp dir
					tmpDir = args[i + 1];
				else if(args[i].equals("-o")) //output file name
					outputFilename = args[i + 1];
				else
				{
					System.err.println("Usage: xsort inputFilename [-hs heapSize | -t tempDir | -o outputFilename]");
					System.exit(1);
				}
				
				i++;
			}
			//only bare arg is inputfilename
			else
				inputFilename = args[i];
		}
	
		makeTempDir();
		makeTempFiles();
		generateRuns();
		
		int[] initDist = getInitDist(numRuns);
		String initDistString = "";
		int numDummyRuns = 0;
		
		for(int i : initDist)
		{
			numDummyRuns += i; //perfect fibonacci distribution total
			initDistString += (i + " ");
		}
		
		//dummy runs equal to perfect fibonacci distribution total minus 
		//number of runs produced from replacement selection
		numDummyRuns -= numRuns;
		
		distributeRuns(initDist, numDummyRuns);
		mergeRuns(initDist);
		
		System.gc(); //run garbage collector
		deleteTempDir();
		System.err.println("Number of initial runs: " + numRuns);
		System.err.println("Initial run distribution: " + initDistString);
		System.err.println("Number of passes: " + numPasses);
	}
	
	/**
	 * Creates a temporary directory to store the files for merging
	 */
	private static void makeTempDir()
	{
		File path = new File(tmpDir, tmpDirName);
		path.deleteOnExit(); //in case of exceptions, remove temporary files
		
		if(path.exists())
			deleteTempDir(); //delete if existing
		
		boolean success = path.mkdir();
		if(!success)
		{
			System.err.println("Couldn't create temp directory.");
			System.exit(1);
		}
	}
	
	/**
	 * Creates the temporary files within the temporary directory
	 */
	private static void makeTempFiles()
	{
		tempFiles = new File[maxHeapSize + 1]; 
		File path = new File(tmpDir, tmpDirName);
		
		try
		{
			//create the temp files for the merge step
			for(int i = 0; i <= maxHeapSize; i++)
			{
				tempFiles[i] = new File(path, "file" + i);
				tempFiles[i].createNewFile();
				tempFiles[i].deleteOnExit();
			}
			
			//create the file for the replacement selection output
			initialRunsFile = new File(path, "runsfile");
		}
		catch(IOException e)
		{
			System.err.println("Error creating temporary files.");
			System.exit(1); //illegal program state so must exit
		}
	}
	
	/**
	 * Delete the temporary directory used and all files within it
	 */
	private static void deleteTempDir()
	{
		File path = new File(tmpDir, tmpDirName);
		
		//don't have to delete anything if it doesn't exist in the first place
		if(path.exists())
		{
			File[] files = path.listFiles();
			
			//delete each file inside the directory first
			for(int i = 0; i < files.length; i++)
				files[i].delete();

			//delete the directory itself
			path.delete();
		}
	}
	
	/**
	 * Takes the input data and creates runs of at least 15 in length separated over
	 * as many temporary files as specified
	 */
	public static void generateRuns()
	{
		try
		{
			BufferedReader inReader;
			BufferedWriter outWriter;
			
			if(inputFilename == null) //none specified as argument, use stdin
				inReader = new BufferedReader(new InputStreamReader(System.in));
			else
				inReader = new BufferedReader(new FileReader(inputFilename));
			
			//store all runs to last file to obtain a count
			//outWriter = new BufferedWriter(new FileWriter(tempFiles[tempFiles.length - 1]));
			outWriter = new BufferedWriter(new FileWriter(initialRunsFile));
		
			MinHeap<String> heap = new MinHeap<String>(maxHeapSize);
			
			//fill heap
			String next;
			while(!heap.full() && ((next = inReader.readLine()) != null))
				heap.insert(next);
			
			//used to handle unusable input
			String lastOutput = "";
			//write out first value
			outWriter.write(lastOutput = heap.removeMin());
			
			//heap not empty and input not exhausted
			while(heap.peek() != null)
			{
				//if the next input can be a part of current run
				if(heap.peek().compareTo(lastOutput) >= 0)
				{
					outWriter.newLine();
					outWriter.write(lastOutput = heap.removeMin());
				}
				//if next input can fit in heap then store as part of next run
				else if(heap.currentSize() > 0) 
					heap.minNextRun();
				
				//if heap not full and input not exhausted
				while(!heap.full() && ((next = inReader.readLine()) != null))
					heap.insert(next);
				
				if(heap.currentSize() < 1 || heap.peek() == null && heap.currentSize() != maxHeapSize)
				{
					//unusable items have reached top of heap, must start new run
					heap.setAllThisRun();
					lastOutput = "";
					outWriter.newLine();
					outWriter.write(EOR);
					numRuns++;
				}
			}
			outWriter.newLine();
			outWriter.write(EOR);
			numRuns++;
			
			//close replacement selection output file for writing
			outWriter.flush();
			outWriter.close();
			
			//close input file for reading
			inReader.close();
		}
		catch (FileNotFoundException e)
		{
			System.err.println("Couldn't locate specified input file.");
			System.exit(1);
		}
		catch(IOException e)
		{
			System.err.println("An error occured while reading the input file.");
			System.exit(1);
		}
	}
	
	/**
	 * Determines the initial run distribution for the merge
	 * Assumes the final merged data set will be in the last file once merge completes.
	 * @param numRuns the number of runs produced by replacement selection
	 * @return the number of runs to put in each file, as an integer array with indexes equal to file indexes
	 */
	private static int[] getInitDist(int numRuns)
	{
		int[] runDist = new int[maxHeapSize + 1];
		
		if(numRuns < maxHeapSize) //less runs than could fit in a heap
		{
			for(int i = 0; i < numRuns; i++) 
				runDist[i] = 1;
			
			for(int i = numRuns; i <= maxHeapSize; i++)
				runDist[i] = 0;
		}
		else //more runs than could fit in a heap
		{
			for(int i = 0; i < maxHeapSize; i++) 
				runDist[i] = 1; //second to last state [1 1 1 1...1 0]
			
			runDist[maxHeapSize] = 0;
		}
		
		int currRuns = maxHeapSize + 1;
		
		int currEmpty = maxHeapSize; //begin with last file empty
		int nextEmpty = maxHeapSize - 1;
		
		while(currRuns < numRuns)
		{
			currRuns = 0;
			
			for(int i = 0; i <= maxHeapSize; i++)
			{
				nextEmpty = currEmpty == 0 ? maxHeapSize : currEmpty - 1;
				
				if (i == nextEmpty)
					continue;
				
				runDist[i] = runDist[i] + runDist[nextEmpty];
				
				currRuns += runDist[i];
			}
			
			runDist[nextEmpty] = 0;
			currEmpty = nextEmpty;
		}
		
		return runDist;
	}
	
	/**
	 * Distributes dummy and generated runs to the temporary files prior to the merge step
	 * @param initDist an integer array of number of runs in each tape for perfect fib distribution
	 * @param numDummyRuns the number of dummy runs needed to make up the perfect fib distribution
	 */
	private static void distributeRuns(int[] initDist, int numDummyRuns)
	{
		try
		{
			//dummy runs shared between non-empty files evenly
			BufferedWriter[] writers = new BufferedWriter[maxHeapSize + 1];
			BufferedReader reader = new BufferedReader(new FileReader(initialRunsFile));
			int[] counts = new int[maxHeapSize + 1];
			
			//Create file writers
			for(int i = 0; i < maxHeapSize + 1; i++)
				if(initDist[i] > 0)
					writers[i] = new BufferedWriter(new FileWriter(tempFiles[i])); //else null
			
			int fileNum = 0;
			
			//--------------------Distribute dummy runs-----------------------\\
			while(numDummyRuns > 0)
			{
				if (fileNum > maxHeapSize) fileNum = 0;
				
				if(writers[fileNum] != null && counts[fileNum] < initDist[fileNum])
				{
					writers[fileNum].write(EOR);
					counts[fileNum]++;
					
					if (counts[fileNum] < initDist[fileNum])
						writers[fileNum].newLine();
					
					numDummyRuns--;
				}
				
				fileNum++;
			}
			
			//--------------------Distribute real runs------------------------\\
			String nextLine;
			//loop through each file
			for(int i = 0; i < maxHeapSize + 1; i++)
			{
				if(initDist[i] > 0)
				{
					//not reached perfect count and input not exhausted
					while(counts[i] < initDist[i] && (nextLine = reader.readLine()) != null )
					{
						if(nextLine.equals(EOR)) //increment count for file if EOR char encountered
							counts[i]++;
						
						writers[i].write(nextLine);
						
						if(counts[i] < initDist[i]) //don't write new line if file is full
							writers[i].newLine();
					}
					
					writers[i].flush();
					writers[i].close();
				}
			}
			
			reader.close();
		}
		catch(IOException e)
		{
			System.err.println("Error occured while distributing initial runs to files.");
			System.exit(1);
		}
	}
	
	public static void mergeRuns(int[] distribution)
	{
		RunFile[] runFiles = new RunFile[distribution.length];
		for(int i =0; i < tempFiles.length; i++)
		{
			runFiles[i] = new RunFile(tempFiles[i]);
		}
		try
		{
			int emptyFiles = 0;
			//Get the empty file index
			int emptyFileIndex = 0;
			for(int i = 0; i < distribution.length; i++)
			{
				if(distribution[i] == 0)
				{
					emptyFileIndex = i;
					break;
				}
			}
			
			RunFile emptyFile = runFiles[emptyFileIndex];
			RunFile outFile = null;
			boolean newFile = true;
			//Create the writer to output to the target file.
			BufferedWriter outWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(emptyFile.getFile())));;
			boolean firstLineFlag = true; //Dont want to accidentally place characters on the same line or craete extra blank lines
			//While there is more than one file containing values runs must be merged
			
			while(emptyFiles != maxHeapSize)
			{
				//Build heap.
				MinHeap<RunFile> myHeap = new MinHeap<RunFile>(maxHeapSize+1);
				for(int i = 0; i < distribution.length; i++)
				{
					if(i != emptyFileIndex)
					{
						if(runFiles[i].getNext() != null)
						{
							myHeap.insert(runFiles[i]);
						}
					}
					
				}
				
				String lastout = null;
				if (newFile){
					runFiles[emptyFileIndex] = new RunFile(tempFiles[emptyFileIndex]);
					outWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(runFiles[emptyFileIndex].getFile())));
					newFile = false;
				}
				//Work through the current set of runs in each file
				while(myHeap.realSize() != 0 && myHeap.peek().getNext() != null)
				{
					while(!(myHeap.peek().getNext().equals(EOR)))
					{
						lastout = myHeap.peek().consume();
						if(!firstLineFlag)
							outWriter.newLine();
						else
							firstLineFlag = false;
						outWriter.write(lastout);
						myHeap.peek().getNext();
						myHeap.heapify();
					}
					
					distribution[Integer.parseInt(myHeap.peek().getFile().getName().substring(4))] --; //Decrement the file the end of run was just removed from run counter

					myHeap.peek().consume();
					tempFiles[Integer.parseInt(myHeap.peek().getFile().getName().substring(4))] = myHeap.peek().getFile();
					myHeap.removeMin();
					
					//Continue for each of the currently open runs.
					while(myHeap.realSize() != 0)
					{
						while(myHeap.peek().getNext() != null && !(myHeap.peek().getNext().equals(EOR)))
						{	
							if(lastout == null || lastout.compareTo(myHeap.peek().getNext()) <= 0)
							{
								lastout = myHeap.peek().consume();
								if(!firstLineFlag)
									outWriter.newLine();
								else
									firstLineFlag = false;
								outWriter.write(lastout);
								outWriter.flush();
								if(myHeap.peek().getNext() != null)
									myHeap.heapify();
							}else{ //This should never ocur but is cautionary
								lastout = myHeap.peek().consume();
								if(!firstLineFlag)
									outWriter.newLine();
								else
									firstLineFlag = false;
								outWriter.write(EOR);
								outWriter.write(lastout);
							}
							
						}
						distribution[Integer.parseInt(myHeap.peek().getFile().getName().substring(4))] --; //Decrement the file the end of run was just removed froms run counter
						myHeap.peek().consume();
						tempFiles[Integer.parseInt(myHeap.peek().getFile().getName().substring(4))] = myHeap.peek().getFile();
						myHeap.removeMin();
						
					}	
					distribution[Integer.parseInt(emptyFile.getFile().getName().substring(4))]++;
					
				}
				if(!firstLineFlag)
					outWriter.newLine();
				else
					firstLineFlag = false;
				outWriter.write(EOR);//Add the end of line character so that future merges will detect the end.	
				int oldEmpty = emptyFileIndex;
				emptyFiles = 0;
				for(int i = 0; i < distribution.length; i++)
				{
					if(distribution[i] == 0)
					{
						newFile = true;
						emptyFileIndex = i;
						emptyFiles++;
					}
				}
				//If an input file has reached the end of its file then we need to start writing to that file instead
				if(newFile)
				{
					runFiles[emptyFileIndex].closeReader();
					numPasses ++; //Used  to calculate Passes
					outWriter.flush();
					outWriter.close();
					runFiles[oldEmpty] = new RunFile(tempFiles[oldEmpty]);
					//runFiles[Integer.parseInt(emptyFile.getFile().getName().substring(4))] = emptyFile;
					outFile = emptyFile;
					emptyFile = runFiles[emptyFileIndex];
					firstLineFlag = true;
				}
			}
			BufferedReader finalReader = new BufferedReader(new FileReader(outFile.getFile()));
			String line;
			if(outputFilename != null)
			{
				
				outWriter = new BufferedWriter(new FileWriter(outputFilename));
				
				//Write out first value
				if((line = finalReader.readLine()) != null)
				{
					outWriter.write(line);
				}
				//Run through the file to write out to final file
				while((line = finalReader.readLine()) != null)
				{
					if(!line.equals(EOR))
					{
						outWriter.newLine();
						outWriter.write(line);
					}
				}
				outWriter.flush();
				outWriter.close();
			}
			else
			{
				while((line = finalReader.readLine()) != null)
				{
					if(!line.equals(EOR))
					{
						System.out.println(line);
					}
				}
			}
			finalReader.close();
		}
		catch(FileNotFoundException fnfe)
		{
			System.err.println("An error occured while initialising input file reader. " + fnfe.getMessage());
		}
		catch(IOException ioex)
		{
			System.err.println("An error occured while creating the output file. " + ioex.getMessage());
		}
	}
}