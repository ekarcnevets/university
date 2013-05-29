import java.io.*;

/**
 * @author Steven Crake, 1117696
 */
public class RunFile implements Comparable<RunFile>
{
	private File runFile; 			//path to file used by an instance of RunFile
	private BufferedReader reader;  //reads runFile
	public String nextLine; 		//the next string read in from the runFile
	
	public RunFile(File file)
	{
		try
		{
			runFile = file;
			reader = new BufferedReader(new FileReader(runFile));
			getNext();
		}
		catch (FileNotFoundException e)
		{
			System.err.println("Error, run file not found).");
			System.exit(1);
		}
	}
	
	/**
	 * Consumes the current line of the file and ensures next line is read in from file
	 */
	public String consume()
	{
		String line = getNext();
		nextLine = null;
		return line;
	}
	
	/**
	 * If the current line has not yet been consumed, returns it. 
	 * Otherwise, reads the next line from the file. 
	 */
	public String getNext()
	{
		if(nextLine != null) return nextLine;
		else
		{
			try
			{
				nextLine = reader.readLine();
			}
			catch (IOException e)
			{
				System.err.println("Error reading line from run file.");
				e.printStackTrace();
				System.exit(1);
			}
			
			return nextLine;
		}
	}
	
	/**
	 * Uses default string comparison on the current line of each RunFile
	 */
	public int compareTo(RunFile otherFile)
	{
		return getNext().compareTo(otherFile.getNext());
	}
	public void closeReader()
	{
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public File getFile()
	{
		return runFile;
	}
}