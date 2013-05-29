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
public class Bitpacker 
{
	private static DataOutputStream output = new DataOutputStream(System.out);
	private static int buffer = 0; //used to pack bits into and output when becomes full
	private static int numFreeBits = 32; //number of free bits in the buffer
	
	public static void main(String args[])
	{
		pack();
	}
	
	/**
	 * Read input and pack into the smallest number of bits possible
	 * Outputs bits packed into 32 bit integers to stdout as they become full
	 */
	private static void pack()
	{
		boolean trieFull = false;
		int dictEntries = 3; //how many entries in LZ78 dictionary (start at 3 -> 0 is reset, 1 is EOF, 2 is empty string)
		int bitCount = 2; //how many bits needed to encode phrase number into
		int phraseNum, mmByte;
		int nextByte; //used to read into from input
		
		try
		{
			while((nextByte = System.in.read()) != -1)
			{
				phraseNum = 0;
				
				//read 4 bytes and construct phraseNum
				for(int i = 1; i <= 4; i++)
				{
					phraseNum |= nextByte << (4 - i) * 8;
					
					nextByte = System.in.read();
					
					if(nextByte == -1)
						throw new Exception("Input stream terminated unexpectedly while reading phrase number. bitCount was: " + bitCount + ", dictEntries was: " + dictEntries);
				}
				
				//read mismatch byte (immediately after phrase number, no delimiter)
				mmByte = nextByte;
					
				if(phraseNum == 0 && mmByte == 0) 
				{
					//second encounter means trie was discarded
					if(trieFull)
					{
						packIntsForOutput(bitCount, phraseNum, mmByte);

						dictEntries = 3; //start over
						bitCount = 2;
						
						trieFull = false;
						continue;
					}
					
					trieFull = true;
				}
				else if (!trieFull)
				{
					dictEntries++; //only incremented if trie not full or discarded 
					
					//if can't fit next possible phrase num into 'bitCount' bits, increment bitCount
					if(31 - Integer.numberOfLeadingZeros(dictEntries - 1) >= bitCount)
						bitCount++;
				}
				
				packIntsForOutput(bitCount, phraseNum, mmByte);
			}
			
			dictEntries++;
			if(31 - Integer.numberOfLeadingZeros(dictEntries - 1) >= bitCount)
					bitCount++;
			
			phraseNum = 1; //EOF
			mmByte = 0x04; //doesn't matter what this is
			packIntsForOutput(bitCount, phraseNum, mmByte);
			
			if(numFreeBits < 32)
				output.writeInt(buffer);
		}
		catch(Exception ex)
		{
			System.err.println("Error occured in Bitpacker.pack(). " + ex.getMessage());
			System.exit(1);
		}
		
		closeOutput();
	}

	/**
	 * Packs phraseNum in bitCount bits and mmByte in 8 bits into ints, outputting when they become full
	 */
	private static void packIntsForOutput(int bitCount, int phraseNum, int mmByte) throws IOException 
	{
		//pack phrase number and mismatched byte into int(s) and output if/when it gets full
		if(bitCount + 8 < numFreeBits)
		{ //can fit all of phrasenum and mismatchbyte with room to spare
			
			//write phraseNumAndMMLen bits
			buffer |= phraseNum << (numFreeBits - bitCount);
			buffer |= mmByte << (numFreeBits - (bitCount + 8));
			
			//calculate new numFreeBits
			numFreeBits -= bitCount + 8 ;
		}
		else if(bitCount + 8 == numFreeBits)
		{ //can fit phrasenum and mismatchbyte exactly
			
			//write phraseNumAndMMLen bits
			buffer |= phraseNum << 8;
			buffer |= mmByte;
			
			//output and clear buffer
			output.writeInt(buffer);
			buffer = 0;
			numFreeBits = 32;
		}
		else
		{ //can't fit all of phrasenum and mismatch byte, need to break up somewhere
			
			//decide where to break
			if(bitCount > numFreeBits)
			{ //can fit some of phrasenum
				
				//shift non-fitting bits off the right side and write remaining to buffer
				buffer |= (phraseNum >>> (bitCount - numFreeBits));
				
				//output and clear buffer
				output.writeInt(buffer);
				buffer = 0;
				
				//write remaining bits of phraseNum + mmByte to new buffer
				buffer |= phraseNum << (32 - bitCount) + numFreeBits;
				buffer |= mmByte << (32 - (bitCount + 8)) + numFreeBits;
				
				//calculate new free bits
				numFreeBits = (32 - (bitCount + 8)) + numFreeBits;
			}
			else if (bitCount == numFreeBits)
			{ //can fit all of phrasenum but no more

				//write phrasenum
				buffer |= phraseNum;
				
				//output and clear buffer
				output.writeInt(buffer);
				buffer = 0;
		
				//write mmByte to new buffer
				buffer |= mmByte << 24;
				numFreeBits = 24; //since we know mmByte always 8 bits long
			}
			else
			{ //can fit all of phrasenum and some of mmByte

				//figure out how many bits of mmByte can fit
				int numCanFit = numFreeBits - bitCount;
				
				buffer |= phraseNum << numCanFit;
				
				//shift non-fitting mmByte bits off right side and write remainder to buffer
				buffer |= mmByte >>> 8 - numCanFit;
				
				//output and clear buffer
				output.writeInt(buffer);
				buffer = 0;
				
				//write remaining bits to new buffer
				buffer |= mmByte << (32 - (8 - numCanFit));
				
				numFreeBits = (32 - (8 - numCanFit));
			}
		}
	}
	
	/**
	 * Closes the output stream in case of exception or finishing bit packing
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
}
