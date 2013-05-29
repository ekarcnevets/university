import java.io.DataOutputStream;
import java.util.HashMap;
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
public class Decoder {
	private static int dictionarySize;
	private static boolean maxTrieSizeReached;
	private static DataOutputStream out;
	private static final int dictResetValue = 3;
	//emptyCharValue = 2;
	static byte[] buffer;
	/**
	 * Main method just calls decode
	 * @param args
	 */
	public static void main(String[] args) {
			decode();
	}
	/**
	 * Decodes a file piped in from System.in
	 * 
	 * Decoding is done through the use of a hashmap dictionary.
	 * 
	 * Hashmap was found to be more efficient than using a AVL tree by a runtime factor of 200:1
	 */
	public static void decode(){
		try {
			HashMap<Integer, Node> hash = new HashMap<Integer, Node>();
			dictionarySize = dictResetValue;
			maxTrieSizeReached = false;
			out = new DataOutputStream(System.out);
			int phraseNum;
			int phrase;
			int nextByte;
			while((nextByte = System.in.read()) != -1) {
				    phraseNum = 0;
				    Node newNode = null;
				    
				    //read 4 bytes and construct phraseNum
				    for(int i = 1; i <= 4; i++)
				    {
					     phraseNum |= nextByte << (4 - i) * 8; //Bitshift 8 times to the left
					     nextByte = System.in.read();
					     
					     if(nextByte == -1) //Premature end to file
					     {
					      throw new Exception("Input stream terminated unexpectedly while reading phrase number.");
					     }
				    }
				    phrase = nextByte;
				    if(phraseNum == 0){//Check if the phraseNum and Phrase are for reset
						if(phrase == 0){
							if(maxTrieSizeReached){ //If maxSize is already reached, ie a previous '0 0' has been read, then reset
								hash = new HashMap<Integer, Node>();
								dictionarySize = dictResetValue;
								maxTrieSizeReached = false;
							}else{// maxSize not yet reached so set it to true
								maxTrieSizeReached = true;
							}
						}
					}else{ //The phraseNum and phrase are not reset so write out the pattern and add the value to the current dictionary
						Node Parent = (Node) hash.get(phraseNum);
						newNode = new Node(phrase,dictionarySize,Parent);
						if(!maxTrieSizeReached){ //If the maxTrieSize has not been reached yet then add the phrase to the dictionary
						hash.put(newNode.index, newNode);
						dictionarySize++;
						}
						newNode.decode(out);//Recursively decode the phrase out
					}
			}
			//Writing anything left in the buffer out and then close it
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
