import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * Node for encoding and decoding using LZ78 compression
 * 
 * The node stores various attributes regarding a phrase including a link to the parent a child and the next sibling
 *
 * @author Steven Crake 1117696
 */
public class Node {
	Node nextSibling;
	Node child;
	int phrase;
	int index;
	Node parent;
	/**
	 * Constructor for Node
	 * @param newPhrase, the Phrase of this node
	 * @param newIndex, The Index of this node
	 * @param newPrecursorIndex, The parent Index of this node
	 */
	Node(int newPhrase, int newIndex, Node newPrecursorIndex){
		phrase = newPhrase;
		index = newIndex;
		parent = newPrecursorIndex;
		child = null;
		nextSibling = null;
	}
	/**
	 * Finds a child of this node with a given phrase and returns it if it exists, otherwise returns null
	 * @param wantedPhrase
	 * @return
	 */
	public Node returnChildWithValue(int wantedPhrase){
		if(child == null) return null;
		return child.findPhraseOnLevel(wantedPhrase);
	}
	
	/**
	 * Used by returnChildWithValue to find a phrase node if it exists otherwise returns null
	 * @param wantedPhrase
	 * @return
	 */
	public Node findPhraseOnLevel(int wantedPhrase){
		if (phrase == wantedPhrase) return this;
		else if(nextSibling == null) return null;
		else{
			return nextSibling.findPhraseOnLevel(wantedPhrase);
		}
	}
	/**
	 * Adds a child
	 * If this node has no children then the passed in node is set as the main child.
	 * Otherwise the passed in node is recursively passed through the children until an empty spot is found
	 */
	public void addChild(Node newNode){
		if(child == null)child = newNode;
		else child.addSibling(newNode);
	}
	/**
	 * Adds a Sibling
	 * Recursively moves through sibling nodes until an open space is found
	 * @param newNode, the new node to be set as a sibling
	 */
	public void addSibling(Node newNode){
		if(nextSibling == null) nextSibling = newNode;
		else nextSibling.addSibling(newNode);
	}
	/**
	 * Decodes this node recursively through its parents to output the full phrase of the pattern this node represents
	 * @param outWriter, the output Stream to write to
	 */
	public void decode(DataOutputStream outWriter){
		if(parent!= null) parent.decode(outWriter);
		try {
			outWriter.writeByte(phrase);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
