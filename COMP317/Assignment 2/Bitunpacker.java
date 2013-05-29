import java.io.*;

/*
 * phraseNums
 * 0 - reset
 * 1 - Used in bitpacker
 * 2 - Empty character value
 * 3 - This is the index of the first new byte
 */

/**
 * @author Steven Crake 1117696
 */
public class Bitunpacker
{
	private static DataOutputStream output = new DataOutputStream(System.out);
	private static int phraseNum = 0; //used to unpack bits from input into
	private static int mmByte = 0; //mismatched byte
	
	private static boolean trieFull = false;
	private static int dictEntries = 3; //how many entries in LZ78 dictionary (start at 3 -> 0 is reset, 1 is EOF, 2 is empty string)
	private static int bitCount = 2; //how many bits needed to read phrase number 
	
	private static int remainingPhraseNumBits = bitCount;
	private static int remainingMMByteBits = 8;
	
	public static void main(String args[])
	{
		unpack();
	}
	
	/**
	 * Read bitpacked input and unpack into original bytestream (identical to encoder output)
	 * Outputs unpacked tuples to stdout as they are unpacked
	 */
	private static void unpack()
	{
		int inBuffer = 0; //holds bytes read in from input stream
		int nextByte;
		boolean prevTupleMissingBits = false; 
		int unusedBits = 32;
		
		try
		{
			//while we can get read 1 byte, there is at least 4 bytes still readable (input is multiple of 4 bytes long)
			while((nextByte = System.in.read()) != -1) 
			{
				inBuffer = 0;
				unusedBits = 32;
				
				//read 4 bytes and fill buffer
				for(int i = 1; i <= 4; i++)
				{
					inBuffer |= nextByte << (4 - i) * 8;
					
					if(i != 4)
					{
						nextByte = System.in.read();
						
						if(nextByte == -1)
							throw new Exception("Input stream terminated unexpectedly while reading into buffer. bitCount was: " + bitCount + ", dictEntries was: " + dictEntries);
					}
				}
				
				//if the tuple currently being unpacked is still missing bits from the last packed int
				if(prevTupleMissingBits)
				{
					if(remainingPhraseNumBits == 0) //phraseNum already finished
					{
						// read remaining mmByte then output and continue
						unusedBits -= remainingMMByteBits;
						mmByte |= inBuffer >>> unusedBits;
					
						//delete bits just read
						inBuffer <<= (32 - unusedBits);
						inBuffer >>>= (32 - unusedBits);
						
						checkValuesAndOutput(); //finished tuple so can now output
						prevTupleMissingBits = false;
					}
					else if(remainingPhraseNumBits > 0) //phraseNum not finished
					{
						//finish phraseNum
						unusedBits -= remainingPhraseNumBits;
						phraseNum |= inBuffer >>> unusedBits;
						//int tmpRPNB = remainingPhraseNumBits;
						remainingPhraseNumBits = 0;
				
						//delete bits just read
						inBuffer <<= (32 - unusedBits);
						inBuffer >>>= (32 - unusedBits);
						
						//can always read rest of phraseNum and some/all of mmByte (at least 1 bit of mmByte)
						if(remainingMMByteBits <= unusedBits)
						{
							//can read all of mmByte
							unusedBits -= remainingMMByteBits;
							mmByte |= inBuffer >>> unusedBits; //unusedBits could be 0 so no shift
							
							//delete bits just read
							inBuffer <<= (32 - unusedBits);
							inBuffer >>>= (32 - unusedBits);
							
							checkValuesAndOutput(); //finished tuple so can now output
							prevTupleMissingBits = false;
						}
						else
						{
							//can only read some of mmByte - find out how much
							remainingMMByteBits -= (8 - unusedBits);
							mmByte |= inBuffer << remainingMMByteBits; //need to leave room for rest
							unusedBits = 0; //need to skip the loop below
							
							prevTupleMissingBits = true;
						}
					}
					else throw new Exception("Bitunpacker: Unexpected value of remainingPhraseNumBits '" + remainingPhraseNumBits + "'.");
				}
				
				//while there are still bits left to use in the packed int
				while(unusedBits > 0)
				{
					if(remainingPhraseNumBits + remainingMMByteBits <= unusedBits) //can read both
					{
						//read all of phraseNum
						unusedBits -= remainingPhraseNumBits;
						phraseNum = inBuffer >>> unusedBits;
							
						//delete bits just read
						inBuffer <<= (32 - unusedBits);
						inBuffer >>>= (32 - unusedBits);
						
						// read all of mmByte
						unusedBits -= remainingMMByteBits;
						mmByte = inBuffer >>> unusedBits; //unusedBits could be 0 so no shift
						
						//delete bits just read
						inBuffer <<= (32 - unusedBits);
						inBuffer >>>= (32 - unusedBits);
						
						checkValuesAndOutput(); //finished tuple so can now output
						prevTupleMissingBits = false;
					}
					else if(remainingPhraseNumBits <= unusedBits) //can read all phraseNum and maybe some of mmByte
					{
						//read all of phraseNum
						unusedBits -= remainingPhraseNumBits;
						phraseNum |= inBuffer >>> unusedBits;
						remainingPhraseNumBits = 0;
						
						if(unusedBits > 0) //can still read some of mmByte
						{
							//delete bits just read
							inBuffer <<= (32 - unusedBits);
							inBuffer >>>= (32 - unusedBits);
							
							//find out how much mmByte will be left over to read
							remainingMMByteBits -= unusedBits;
							
							mmByte = inBuffer << remainingMMByteBits; //need to leave room for rest
							unusedBits = 0;
						}
				
						prevTupleMissingBits = true;
					}
					else //can only read some of phraseNum
					{
						remainingPhraseNumBits -= unusedBits;
						
						phraseNum |= inBuffer << remainingPhraseNumBits;
						
						unusedBits = 0;
						prevTupleMissingBits = true;
					}
				}
			}
		}
		catch(Exception ex)
		{
			System.err.println("Error occured in Bitunpacker.unpack(). " + ex.getMessage());
			System.exit(1);
		}
		
		closeOutput();
	}

	/**
	 * Closes the output stream in case of exception or finishing bit unpacking
	 */
	private static void closeOutput()
	{
		try 
		{
			output.flush();
			output.close();
		}
		catch(Exception e){}
	}
	
	/**
	 * Check if the phrase number and mismatched byte are special (0, 1 - trie full/reset and EOF respectively)
	 * Output them - phrase number in 4 bytes (int) and mismatched byte in 1 byte
	 */
	private static void checkValuesAndOutput() throws IOException
	{
		if(phraseNum == 0 && mmByte == 0) 
		{
			//second encounter means trie was discarded
			if(trieFull)
			{
				dictEntries = 3; //start over
				bitCount = 2;
			}
			
			trieFull = !trieFull;
		}
		else if(phraseNum == 1) //EOF, finished unpacking, can exit
		{
			closeOutput();
			System.exit(0);
		}
		else if (!trieFull)
		{
			dictEntries++; //only incremented if trie not full or discarded
			
			//if can't fit next possible phrase num into 'bitCount' bits, increment bitCount
			if(31 - Integer.numberOfLeadingZeros(dictEntries) >= bitCount)
				bitCount++;
		}
		
		output.writeInt(phraseNum);
		output.writeByte(mmByte);
		
		phraseNum = 0;
		mmByte = 0;
		
		remainingPhraseNumBits = bitCount;
		remainingMMByteBits = 8;
	}
}
