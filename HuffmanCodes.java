package pa4;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;

import javax.xml.soap.Text;

import pa4.ArgsParser.Bindings;

public class HuffmanCodes {
	//maps and variables for compressing
	private PriorityQueue<Node> store = new PriorityQueue<>();
	private HashMap<Character,Integer> frequency = new HashMap<>();
	private HashMap<Character, String> encode = new HashMap<>();
	private HashMap<String,Character> decode = new HashMap<>();
	private String main = "";
	private String unique = "";
	private String out = "";
	private String in = "";
	
	private int inSize = 0;
	private int outSize = 0;

	 private final Option ENCODE, DECODE;
	  private final Option SHOW_FREQUENCY, SHOW_CODES, SHOW_BINARY;
	  private final ArgsParser parser;
	  private final Operand<File> IN, OUT;

	  private ArgsParser.Bindings settings;

	  public HuffmanCodes() {    
	    this.ENCODE = Option.create("-e, --encode").summary("Encodes IN to OUT");
	    this.DECODE = Option.create("-d, --decode").summary("Decodes IN to OUT");
	    this.SHOW_FREQUENCY = Option.create("--show-frequency")
	      .associatedWith(ENCODE).summary("Output byte frequencies");
	    this.SHOW_CODES = Option.create("--show-codes")
	      .summary("Output the code for each byte");
	    this.SHOW_BINARY = Option.create("--show-binary").associatedWith(ENCODE)
	      .summary("Output a base-two representation of the encoded sequence");

	    this.parser = ArgsParser.create("java HuffmanCodes")
	      .summary("Encodes and decodes files using Huffman's technique")
	      .helpFlags("-h, --help");
	    this.IN = Operand.create(File.class, "IN");
	    this.OUT = Operand.create(File.class, "OUT");
	    parser.requireOneOf("mode required", ENCODE, DECODE)
	      .optional(SHOW_FREQUENCY).optional(SHOW_CODES).optional(SHOW_BINARY)
	      .requiredOperand(IN).requiredOperand(OUT);
	  }

	  public static void main(String[] args) throws FileNotFoundException {
	    HuffmanCodes app = new HuffmanCodes();
	    app.start(args);
	  }
	  
	  
	  private void init(Bindings settings) throws FileNotFoundException {
		  	String fileName = settings.operands.get(IN).get(0);
		    File readIn = new File(fileName);
			main = getText(readIn);
			createMap(main);
			addNode();
			Node root = createTree();
			makeCodeX(root,"");
		}
	  
	  
	  public void start(String[] args) throws FileNotFoundException {
	    settings = parser.parse(args);
	    if(settings.hasOption(ENCODE))
	    {
		    init(settings);
	    	encode();
	    }
	    else if(settings.hasOption(DECODE))
	    {
	    	init(settings);
	    	decode();
	    }
	    if(settings.hasOption(SHOW_FREQUENCY))
	    {
	    	showFrequency();
	    }
	    else if(settings.hasOption(SHOW_CODES))
	    {
	    	showCodes();
	    }
	    else if( settings.hasOption(SHOW_BINARY))
	    {
	    	showBinary();
	    }
	  }

	  private void decode(){
			
		}

		
		private String encode(){
			for(int i = 0 ; i < main.length();i++)
			{
				out += encode.get(main.charAt(i));
				outSize++;
			}
			return out;	
		}
	

	private void showBinary() {
		System.out.println("ENCODED SEQUENCE");
		System.out.println(out);
		System.out.println("input: "+inSize+" bytes "+"[ "+inSize*8+"Bytes ]");
		
		
		
	}

	private void showCodes() {
		// TODO Auto-generated method stub
		System.out.println("CODES:");
		for(int i = 0; i < unique.length();i++)
		{
			System.out.println(encode.get(unique.charAt(i))+" -> "+unique.charAt(i));
		}
		
	}

	private void showFrequency() {
		for(int i = 0 ; i < unique.length(); i++)
		{
			System.out.println(unique.charAt(i)+" :" + frequency.get(unique.charAt(i)));
		}
	}

	

	private void makeCodeX(Node root,String input) {
		if(root != null)
		{
			if(root.left == null && root.right == null)
			{
				encode.put(root.name,input);
				decode.put(input, root.name);
			}
		}
		makeCodeX(root.left,input+"0");
		makeCodeX(root.right,input+"1");
	}

	private Node createTree() {
		for(int i = 0; i < store.size(); i++)
		{
			Node add = new Node();
			add.right = store.poll();
			add.left = store.poll();
			add.frequency = add.right.frequency+add.left.frequency;
			store.add(add);
		}
		return store.poll();
	}

	private void addNode() {
		for(int i = 0; i < unique.length(); i++)
		{
			Node add = new Node(unique.charAt(i),frequency.get(unique.charAt(i)));
			store.add(add);
		}
		
	}

	private void createMap(String main) {
		for(int i = 0; i < main.length(); i++){
			char probe = main.charAt(i);
			if(!frequency.containsKey(probe))
			{
				frequency.put(probe, 1);	
				unique+=probe;
			}
			else
			{
				frequency.put(probe,frequency.get(probe)+1);
			}
			
		}
	}

	private String getText(File readIn) throws FileNotFoundException {
		// TODO Auto-generated method stub
		String main = "";
		Scanner read = new Scanner(readIn);
		while(read.hasNext())
		{
			main += read.next();
			inSize++;
		}
		return main;
	}
	
	class Node {
		private Node left = null;
		private Node right = null;
		private char name;
		private int frequency;
		
		public Node() {
		}
		
		public Node(char name, int frequency){
			this.name = name;
			this.frequency = frequency;
		}
	}
}
	
	