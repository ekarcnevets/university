import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Steven Crake 1117696
 */
public class Encoder {
	private static int maxBits;
	private static List <Node> topLevel;
	private static int dictionarySize;
	private static boolean maxTrieSizeReached;
	private static DataOutputStream out;
	private static final int dictResetValue = 3;
	private static final int emptyCharValue = 2;
	/**
	 * LZ78 Encoder
	 * 
	 * Checks arguments and then encodes the file piped in from system.in
	 * @param args, The arguments passed in
	 */
	public static void main(String[] args) {
		//parse arguments
		maxBits = 20;
		for (int i = 0; i < args.length; i++)
		{
			if(args[i].startsWith("-"))
			{
				if(args[i].equals("-b"))
				{
					try
					{
						//Set the max bits to the phrase following the -b argument.
						maxBits = Integer.parseInt(args[i + 1]);
						if(maxBits < 8){ //Max bits cannot be less than 8
							System.err.println("Maximum Bits cannot be less than 8. Using 20 Bit encoding.");
							maxBits = 20;
						}else if(maxBits > 32){ // Max bits cannot be greater than 32
							System.err.println("Maximum Bits cannot be greater than 32. Using 20 Bit encoding.");
							maxBits = 20;
						}
					}
					catch(NumberFormatException e)//Catches invalid format exceptions, ie if the user enter -b word
					{
						System.err.println("Invalid Maximum Bits provided. Using default of 20.");
						maxBits = 20;
					}
				}
				else// An argument was attempted to be entered that does not match the allowed modules
				{
					System.err.println("Usage: Encoder [-b maximumBitCount]");
					System.exit(1);
				}
				
				i++;
			}
		}
			readInFile(); //The actual compression algorithm
			try {
				out.flush(); //Ensure the writer is flushed and closed
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}


	}
	/**
	 * Read in the file passed in by the system.in pipe to encode using lz78
	 * 
	 */
	public static void readInFile(){
		//Set everything up
		dictionarySize = dictResetValue;
		maxTrieSizeReached = false;
		topLevel = new ArrayList<Node>();
		Node container;
		try {

			out = new DataOutputStream(System.out);
		       /*
		        * To read content of the file in byte array, use
		        * int read(byte[] byteArray) method of java FileInputStream class.
		        *
		        * This method was ignored due to high memory consumption
		        */
			int in;
		       while((in = System.in.read()) != -1){
				container = null;
				
				if(!maxTrieSizeReached){
					//Check if current phrase is in dictionary
					for(Node top : topLevel){
						if(top.phrase == in){
							container = top;
							break;
						}
					}
					//Add to dictionary
					if(container == null){//Value is not in dictionary
						Node newNode = new Node(in,dictionarySize, null);
						print(emptyCharValue ,in);
						topLevel.add(newNode);
						dictionarySize++;
						
					}else{ //Value is in dictionary, keep reading bytes until a new pattern is found
						boolean readAnythingThisPass = false;
						boolean foundFlag = false;
						Node getFinalNode = null;
						int finalHolder = in;
						while(!foundFlag && (in = System.in.read()) != -1){//Keep reading until either a new pattern is found or the file ends
							readAnythingThisPass = true;
							finalHolder = in;
							getFinalNode = container;
							container = container.returnChildWithValue(in);
							if(container == null){ //Found new unrecognized byte pattern
								foundFlag = true;
								Node newNode = new Node(in,dictionarySize, getFinalNode);
								getFinalNode.addChild(newNode);
								print(getFinalNode.index,in);
								dictionarySize++;
							}
						}
						if(!foundFlag && readAnythingThisPass){//Catchment case for end of file part way through a recognized pattern
							print(getFinalNode.index,finalHolder);
						}
						else if(!foundFlag){//End of file and 1 byte read which has been seen before
							print(emptyCharValue,finalHolder);
						}
					}
					if((32-Integer.numberOfLeadingZeros(dictionarySize+1))>= maxBits + 1){
						print(0, (byte)0);
						maxTrieSizeReached = true;
					}
				}else{ //Max trie size has been reached so can no longer add values to the dicitonary
					int inBits = 8;
					//Check if current phrase is in dictionary
					for(Node top : topLevel){
						if(top.phrase == in){
							container = top;
							break;
						}
					}
					if(container == null){//Value is not in dictionary, must reset trie to not go over max bit usage.
						resetTrie();
						Node newNode = new Node(in,dictionarySize, null);
						print(emptyCharValue,in);
						topLevel.add(newNode);
						dictionarySize++;
						
					}else{ //Value is in dictionary, keep reading bytes until a new pattern is found
						boolean readAnythingThisPass = false;
						boolean foundFlag = false;
						Node getFinalNode = null;
						int finalHolder = in;
						while(!foundFlag && (in = System.in.read()) != -1){ // Next byte is read in to the 'in' variable
							readAnythingThisPass = true;
							finalHolder = in;
							inBits += 8; //Used to make sure compression ratio is not lost.
							getFinalNode = container;//Used in the case of the next byte being the end of the file
							container = container.returnChildWithValue(in);
							if(container == null){ //Found new unrecognized byte pattern
								foundFlag = true;
								print(getFinalNode.index,in);
								
							}
						}
						if(!foundFlag && readAnythingThisPass){//Catchment case for end of file part way through a recognized pattern
							print(getFinalNode.index,finalHolder);
						}
						else if(!foundFlag){//End of file and 1 byte read which has been seen before
							print(emptyCharValue,finalHolder);
						}
						if((32-Integer.numberOfLeadingZeros(dictionarySize -1 )) + 8 > inBits){//Ensure the compression ratio is not lower that 1:1, otherwise reset the trie
							resetTrie();
						}
					}
					
				}
				
					
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Print out the latest pattern mismatch
	 * @param outIndex, the dictionary index of the parent which can be read recursively throught the parent bytes to decode
	 * @param outPhrase, the new mistmatch byte
	 */
	public static void print(int outIndex, int outPhrase){
	try {
		out.writeInt(outIndex);
		out.writeByte(outPhrase);
	} catch (IOException e) {
	}
		
	
	}
	/**
	 * Prints out the '0 0' tuple and resets all necessary variables to reset the Trie
	 */
	public static void resetTrie(){
		print(0, (byte)0);
		topLevel = new ArrayList<Node>();
		dictionarySize = dictResetValue;
		maxTrieSizeReached = false;
	}
	

}
