package pa4;



import java.util.PriorityQueue;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Huffman {
	
	public PriorityQueue<HuffmanNode> pQueue;
    public HashMap<Character, String> charToCode;
    public HashMap<String, Character> codeToChar;
    HashMap<Character, Integer> frequency;
    String text = "";
    String uniqueText = "";
    String compressedText = "";

    private void addToMap() {
    	for (int i = 0; i < text.length(); i++) {
    		char cursor = text.charAt(i);
    		if (!frequency.containsKey(cursor)) {
    			frequency.put(cursor, 1);
    			uniqueText += cursor;
    		}
    		else
    			frequency.put(cursor, frequency.get(cursor) + 1);
    	}
    }
    
    private void createNodes() {
    	for (int i = 0; i < uniqueText.length(); i++) {
    		char cursor = uniqueText.charAt(i);
    		HuffmanNode node = new HuffmanNode(cursor, frequency.get(cursor));
    		pQueue.add(node);
    	}
    }
    
    // Constructor
    public Huffman(File file) {
		pQueue = new PriorityQueue<HuffmanNode>();
		charToCode = new HashMap<Character, String>();
		codeToChar = new HashMap<String, Character>();
		frequency = new HashMap<Character, Integer>();
		text = getText(file);
		addToMap();
		createNodes();
		HuffmanNode root = buildTree(pQueue.size());
		buildTable(root);
    }

 // This method assumes that the tree and dictionary are already built
    public String compress() {
    	for (int i = 0; i < text.length(); i++)
    		compressedText += charToCode.get(text.charAt(i));
        return compressedText;
    }

    // This method assumes that the tree and dictionary are already built
    public String decompress() {
    	String decompressed = "";
    	String cursor = "";
    	for (int i = 0; i < compressedText.length(); i++) {
    		cursor += compressedText.charAt(i);
    		if (codeToChar.containsKey(cursor)) {
    			decompressed += codeToChar.get(cursor);
    			cursor = "";
    		}
    	}
        return decompressed;
    }

    // This method builds the table for the compression and decompression
    private void buildTable(HuffmanNode root) {
    	postorder(root, "");
    }

    // This method builds the tree based on the frequency of every characters
    private HuffmanNode buildTree(int n) {
    	for (int i = 1; i <= n; i++) {
    		HuffmanNode node = new HuffmanNode();
    		node.left = pQueue.poll();
    		node.right = pQueue.poll();
    		node.frequency = node.left.frequency + node.right.frequency;
    		pQueue.add(node);
    	}
        return pQueue.poll();
    }

    public String getText(File file) {
    	String entireFile = "";
		try {
			FileReader fr = new FileReader(file);
	    	int i;
	    	while((i = fr.read()) != -1) {
	    		char temp = (char)i;
	    		entireFile = entireFile + temp;
	    	}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return entireFile;
    }

    // This method is used to create the codes starting at root
    private void postorder(HuffmanNode n, String s) {
    	if (n == null)
    		return;
    	if (n.left == null && n.right == null) {
    		charToCode.put(n.character, s);
    		codeToChar.put(s, n.character);
    	}
    	postorder(n.left, s + '0');
		postorder(n.right, s + '1');
    }
   
    // Internal class
    // !!! DO NOT MAKE ANY CHANGES HERE!!!
    class HuffmanNode implements Comparable<HuffmanNode> {
      
      public char character;
      public int frequency;
      public HuffmanNode left, right;
      
      public HuffmanNode() { }
      
      public HuffmanNode(char character, int frequency) {
        this.character = character;
        this.frequency = frequency;
      }
      
      public String toString() {
        return character + " " + frequency;
      }
      
      public int compareTo(HuffmanNode that) {
        return this.frequency - that.frequency;
      }

    }

}