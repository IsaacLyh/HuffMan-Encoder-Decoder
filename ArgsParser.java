package pa4;

////////////////////////////////////////////////////////////////////////////////
// ArgsParser.java
////////////////////////////////////////////////////////////////////////////////


import java.io.*;
import java.util.*;

// A general command-line argument parser following the Unix
// single-character option conventions (similar to getopt,
// http://en.wikipedia.org/wiki/Getopt) and also the GNU long-form
// option conventions (cf. getopt_long, ibid.).
//
// The API uses the fluent-interface style, as discussed in:
// http://www.martinfowler.com/bliki/FluentInterface.html.
public class ArgsParser
{
	// Canned messages and formatting strings.
	private static final String
	DEFAULT_VERSION = "(unknown)",
	HELP_MESSAGE = "display this help and exit",
	VERSION_MESSAGE = "output version information and exit",
	GENERIC_OPTIONS = "OPTIONS",
	OPTION_SUMMARY_FMT = "%4s%s %-20s   %s%n";


	private boolean reuqiredOperand = false;

	private int count = 0;
	private int requiredOneOfCount = 0 ;
	private boolean connected = false;
	
	private boolean operandHas = false;
	private int counterRequired = 0;
	private Set<Option> optional = new HashSet<>();
	private Set<Option> required = new HashSet<>();
	private List<Option> requiredOneOf = new ArrayList<>();
	private Set<Operand> requiredOp = new HashSet<>();
	private Set<Operand> optionalOp = new HashSet<>();  
	private Set<Operand> oneOrMoreOp = new HashSet<>();
	private Set<Operand> zeroOrMoreOp = new HashSet<>();

	private ArrayList<Operand> operandList  = new ArrayList<>();
	// Factory to make a new ArgsParser instance, to generate help
	// messages and to process and validate the arguments for a command
	// with the given `commandName`.
	public static ArgsParser create(String commandName) {
		return new ArgsParser(commandName);
	}

	// A queryable container to hold the parsed results.
	//
	// Options are added using on of the `optional`, `require`, and
	// `requireOneOf` methods. The presence of such Options in the
	// actual arguments processed can be queried via the `hasOption`
	// method.
	//
	// Operands can be associated with an Option or can stand
	// alone. Standalone Operands are added using the `requiredOperand`,
	// `optionalOperand`, `oneOrMoreOperands`, and `zeroOrMoreOperands`
	// methods. Operands associated with an Option are added when that
	// Option is added.
	public class Bindings {
		public boolean hasOption(Option optionToQuery) {
			return options.contains(optionToQuery);
		}

		// If an Operand is optional and has a default value, then this method
		// will return the default value when the Operand wasn't specified.
		public <T> T getOperand(Operand<T> operand) {

			if (operands.containsKey(operand)) {
				List<T> result = getOperands(operand);

				if (result.size() == 1) {
					return result.get(0);
				}
			}
			else if (operand.hasDefaultValue()) {
				return operand.getDefaultValue();
			}
			throw new RuntimeException(
					String.format("Expected one binding for operand %s", operand));
		}

		public <T> List<T> getOperands(Operand<T> operand) {
			List<T> result = new ArrayList<>();
			if (operands.containsKey(operand)) {
				List<String> uninterpretedStrings = operands.get(operand);
				for (String stringFormat: uninterpretedStrings) {
					result.add(operand.convertArgument(stringFormat));
				}
			}
			return result;
		}

		private void addOption(Option option) {
			options.add(option);
		}

		private void bindOperand(Operand<?> operand, String lexeme) {
			List<String> bindings;
			if (operands.containsKey(operand)) {
				bindings = operands.get(operand);
			}
			else {
				bindings = new ArrayList<>();
				operands.put(operand, bindings);
			}
			try {
				operand.convertArgument(lexeme);
			}
			catch (Exception e) {
				throw new RuntimeException(
						String.format("(invalid format) %s", e.getMessage()));
			}
			bindings.add(lexeme);
		}

		final Set<Option> options = new HashSet<>();
		final Map<Operand, List<String>> operands = new HashMap<>();

		private Bindings() {
			/* intentionally left blank */
		}

		public Set<Option> getOptions()
		{
			return options;
		}
	}

	// Parses the given command-line argument values according to the
	// specifications set through calls to the `optional`, `require`,
	// `requireOneOf` and `operands` methods.
	//
	// When the given arguments don't match the options specified, an
	// error message is printed and the program exits.
	//
	// Options for displaying the help message and the version message
	// are supported by calls made to `help` and `version`,
	// respectively. A call to 'parse` will cause the program to exit if
	// the help or version options are present in the given `args`. If
	// both are specified, then both will be printed before exit.
	public ArgsParser.Bindings parse(String[] args) {
		Bindings bindings = new Bindings();
		//TODO
		try{
			for(String str : args)
			{

			if(str.length()>=2&&str.startsWith("-")&&!str.substring(1,2).equals("-"))
			{
				startWithone(str,bindings,args);
			}
			else if(str.length()>=2&&str.startsWith("-")&&str.substring(1,2).equals("-"))
			{
				dealWithLongOption(str,bindings,args);
			}
			else if(count-1>0&&!str.startsWith("-")&&(operandHas&&args[count-1].startsWith("-")||connected))
			{
				dealWithOperandWithOption(str,bindings,args);
			}
			else if(!str.startsWith("-")||(str.startsWith("-")&&str.length()<2))
			{
				dealWithOperandWithOutOption(str,bindings,args);
			}

			count++;
		}
		checkOption(bindings);
		
		checkOperand(bindings);
		
		}
		catch(IllegalArgumentException e)
		{
			System.err.println("Usage:");
			System.err.println("Error:");
			try{
			throw new IllegalArgumentException(); 
			}
			catch(IllegalArgumentException d)
			{
				System.exit(1);
			}
		}
		
		
		
		return bindings;
	}













	













	// Uses the given `summaryString` when the help/usage message is printed.
	public ArgsParser summary(String summaryString) {
		this.summaryString = summaryString;
		return this;
	}

	// Enables the command to have an option to display the current
	// version, represented by the given `versionString`. The version
	// option is invoked whenever any of the given `flags` are used,
	// where `flags` is a comma-separated list of valid short- and
	// long-form flags.
	public ArgsParser versionNameAndFlags(String versionString, String flags) {
		this.versionString = versionString;
		this.versionOption = Option.create(flags).summary(VERSION_MESSAGE);
		return optional(versionOption);
	}

	// Enables an automated help message, generated from the options
	// specified.  The help message will be invoked whenever any of the
	// given `flags` are used.
	//
	// The `flags` parameter is a comma-separated list of valid short-
	// and long-form flags, including the leading `-` and `--` marks.
	public ArgsParser helpFlags(String flags) {
		this.helpOption = Option.create(flags).summary(HELP_MESSAGE);
		return optional(helpOption);
	}

	// Adds the given option to the parsing sequence as an optional
	// option. If the option takes an Operand, the value of the
	// associated operand can be accessed using a reference to that
	// specific Operand instance.
	//
	// Throws an IllegalArgumentException if the given option specifies
	// flags that have already been added.
	public ArgsParser optional(Option optionalOption) {

		if(!optional.contains(optionalOption))
		{
			optional.add(optionalOption);
		}
		else
		{
			throw new IllegalArgumentException("IllegalArgumentException");
		}



		return this;
	}

	// Adds the given option to the parsing sequence as a required
	// option. If the option is not present during argument parsing, an
	// error message is generated using the given `errString`. If the
	// option takes an Operand, the value of the associated operand can
	// be accessed using a reference to that specific Operand instance.
	//
	// Throws an IllegalArgumentException if the given option specifies
	// flags that have already been added.
	public ArgsParser require(String errString, Option requiredOption) {

		if(required.contains(requiredOption))
		{
			throw new IllegalArgumentException(errString);
		}
		else
		{
			required.add(requiredOption);
		}
		return this;
	}

	// Adds the given set of mutually-exclusive options to the parsing
	// sequence. An error message is generated using the given
	// `errString` when multiple options that are mutually exclusive
	// appear, and when none appear. An example of such a group of
	// mutually- exclusive options is when the option specifies a
	// particular mode for the command where none of the modes are
	// considered as a default.
	//
	// Throws an IllegalArgumentException if any of the given options
	// specify flags that have already been added.
	public ArgsParser requireOneOf(String errString, Option... exclusiveOptions) {
		// TODO
		if(exclusiveOptions.length == 0)
		{
			throw new IllegalArgumentException(errString);
		}
		for(Option option : exclusiveOptions)
		{
			if(requiredOneOf.contains(option))
			{
				throw new IllegalArgumentException(errString);
			}
			else
			{
				requiredOneOf.add(option);
			}
		}
		return this;
	}

	// Adds the given operand to the parsing sequence as a required
	// operand to appear exactly once. The matched argument's value is
	// retrievable from the `ArgsParser.Bindings` store by passing the
	// same `requiredOperand` instance to the `getOperand` method.
	public ArgsParser requiredOperand(Operand requiredOperand) {
		// TODO
		if(requiredOp.contains(requiredOperand))
		{
			throw new IllegalArgumentException();
		}
		else
		{
			requiredOp.add(requiredOperand);		  
		}


		return this;
	}

	// Adds the given operand to the parsing sequence as an optional
	// operand. The matched argument's value is retrievable from the
	// `ArgsParser.Bindings` store by passing the same `optionalOperand`
	// instance to the `getOperands` method, which will return either a
	// the empty list or a list with a single element.
	public ArgsParser optionalOperand(Operand optionalOperand) {
		// TODO

		if(optionalOp.contains(optionalOperand))
		{
			throw new IllegalArgumentException();
		}
		else
		{
			optionalOp.add(optionalOperand);
		}

		return this;
	}

	// Adds the given operand to the parsing sequence as a required
	// operand that must be specifed at least once and can be used
	// multiple times (the canonical example would be a list of one or
	// more input files).
	//
	// The values of the arguments matched is retrievable from the
	// `ArgsParser.Bindings` store by passing the same `operand`
	// instance to the `getOperands` method, which will return a list
	// with at least one element (should the arguments pass the
	// validation process).
	public ArgsParser oneOrMoreOperands(Operand operand) {
		// TODO

		if(oneOrMoreOp.contains(operand))
		{
			throw new IllegalArgumentException();
		}
		else
		{
			oneOrMoreOp.add(operand);
		}


		return this;
	}

	// Adds the given operand to the parsing sequence as an optional
	// operand that can be used zero or more times (the canonical
	// example would be a list of input files, where if none are given
	// then stardard input is assumed).
	//
	// The values of the arguments matched is retrievable from the
	// `ArgsParser.Bindings` store by passing the same `operand`
	// instance to the `getOperands` method, which will return a list of
	// all matches, potentially the empty list.
	public ArgsParser zeroOrMoreOperands(Operand operand) {
		// TODO

		if(zeroOrMoreOp.contains(operand))
		{
			throw new IllegalArgumentException();
		}
		else
		{
			zeroOrMoreOp.add(operand);
		}
		return this;
	}

	private final String commandName;

	private String summaryString = null;
	private String versionString = DEFAULT_VERSION;
	private Option helpOption = null;
	private Option versionOption = null;

	private ArgsParser(String commandName) {
		this.commandName = commandName;
	}

	// TODO: Add more code here if you think it'll be helpful!


	private void startWithone(String str, Bindings bindings, String[] args) {
		// TODO Auto-generated method stub
		String match=null;
		String match2=null;
		int counter = 0 ;
		if(str.length()==2)
		{
			match = str.substring(1);
		}
		else
		{
			match = str.substring(1, 2);
			match2 = str.substring(2,str.length());
			connected = true;
		}

		for(Option option : required)
		{
			ArrayList<String> shortFlag = new ArrayList<String>();
			ArrayList<String> longFlag = new ArrayList<String>();
			option.getFlags(longFlag, shortFlag);

			

			if(shortFlag.contains(match))
			{

				if(!option.hasOperand()&&match2!=null)
				{
					throw new IllegalArgumentException();
				}
				
				if(option.hasOperand())
				{
					operandHas = true;
				}
				else{
					operandHas = false;
				}
				
				
				counter++;

				bindings.addOption(option);
				if(option.hasOperand()&&str.length()<3)
				{
					
					try{
						bindings.bindOperand(option.getOperand(), args[count+1]);
						
					}
					catch(RuntimeException e)
					{
						throw new IllegalArgumentException();
					}
				}
				// find fucking connected option with operand
				else if(option.hasOperand()&&str.length()>2&&!shortFlag.contains(str.substring(2, str.length()-1)))
				{
					
					try{
						bindings.bindOperand(option.getOperand(), str.substring(2, str.length()));
					}
					catch(RuntimeException e)
					{
						System.out.println("found 1 exception");
					}
				}
				else if(count<args.length-1&&!args[count+1].startsWith("-"))
				{
					for(Operand operand : zeroOrMoreOp)
					{
						try{
							bindings.bindOperand(operand, str);
						}
						catch(RuntimeException e)
						{
							System.out.println("found runtime Exception");
						}
					}

				}
			}
		}
		for(Option option : requiredOneOf)
		{
			ArrayList<String> shortFlag = new ArrayList<String>();
			ArrayList<String> longFlag = new ArrayList<String>();
			option.getFlags(longFlag, shortFlag);
			if(shortFlag.contains(match))
			{
				
				if(option.hasOperand())
				{
					operandHas = true;
				}
				else{
					operandHas = false;
				}
				if(!option.hasOperand()&&match2!=null)
				{
					throw new IllegalArgumentException();
				}
				counter++;
				bindings.addOption(option);

				if(option.hasOperand()&&str.length()>2&&!shortFlag.contains(str.substring(2, str.length()-1)))
				{

					try{
						bindings.bindOperand(option.getOperand(), str.substring(2, str.length()));
					}
					catch(RuntimeException e)
					{
						System.out.println("found 1 exception");
					}
				}
				else if(option.hasOperand())
				{
					
					try{
						bindings.bindOperand(option.getOperand(), args[count+1]);
					}
					catch(RuntimeException e)
					{
						System.out.println("found 1 exception");
					}
				}
				else if(!args[count+1].startsWith("-")){
					for(Operand operand : zeroOrMoreOp)
					{
						
						try{
							bindings.bindOperand(operand, str);
						}
						catch(RuntimeException e)
						{
							System.out.println("found runtime Exception");
						}
					}

				}
			}

		}
		for(Option option : optional)
		{
			ArrayList<String> shortFlag = new ArrayList<String>();
			ArrayList<String> longFlag = new ArrayList<String>();
			option.getFlags(longFlag, shortFlag);
			
			if(shortFlag.contains(match))
			{

				if(option.hasOperand())
				{
					operandHas = true;
				}
				else{
					operandHas = false;
				}
				
				if(!option.hasOperand()&&match2!=null)
				{
					throw new IllegalArgumentException();
				}
				counter++;
				bindings.addOption(option);
				if(count+1<args.length-1&&option.hasOperand())
				{
					try{
						bindings.bindOperand(option.getOperand(), args[count+1]);
					}
					catch(RuntimeException e)
					{
						System.out.println("found 1 exception");
					}
				}
				else if(count+1<args.length-1&&!args[count+1].startsWith("-")&&option.hasOperand())
				{
					for(Operand operand : zeroOrMoreOp)
					{
						try{
							bindings.bindOperand(operand, args[count+1]);
						}
						catch(RuntimeException e)
						{
							System.out.println("found runtime Exception");
						}
					}

				}
			}

		}
		
		if(counter == 0)
		{
			throw  new IllegalArgumentException();
		}
		
	}

	private void dealWithOperandWithOption(String str, Bindings bindings,
			String[] args) {
		// TODO Auto-generated method stub
		for(Operand operand : zeroOrMoreOp)
		{
			try{
				bindings.bindOperand(operand, str);
			}
			catch(RuntimeException e)
			{
				System.out.println("found runtime Exception");
			}
		}
		for(Operand operand : oneOrMoreOp)
		{
			try{
				bindings.bindOperand(operand, str);
			}
			catch(RuntimeException e)
			{
				System.out.println("found runtime Exception");
			}
		}
		for(Operand operand : optionalOp)
		{
			try{
				bindings.bindOperand(operand, str);
			}
			catch(RuntimeException e)
			{
				System.out.println("found runtime Exception");
			}
		}
		for(Operand operand : requiredOp)
		{
			try{
				bindings.bindOperand(operand, str);
			}
			catch(RuntimeException e)
			{
				System.out.println("found runtime Exception");
			}
		}

	}


	private void dealWithOperandWithOutOption(String str, Bindings bindings,
			String[] args) {
		for(Operand operand : requiredOp)
		{
			try{
				bindings.bindOperand(operand, str);
				requiredOp.remove(operand);
				return;
			}
			catch(RuntimeException e)
			{
				throw new IllegalArgumentException();
			}
		}
		for(Operand operand2 : zeroOrMoreOp)
		{
			try{
				bindings.bindOperand(operand2, str);
				requiredOp.remove(operand2);
				return;
			}
			catch(RuntimeException d)
			{
				throw new IllegalArgumentException();
			}
		}
		
		for(Operand operand : oneOrMoreOp)
		{
			try{
				bindings.bindOperand(operand, str);
				requiredOp.remove(operand);
				
				return;
			}
			catch(RuntimeException e)
			{
				throw new IllegalArgumentException();
			}
		}
		for(Operand operand : optionalOp)
		{
			try{
				bindings.bindOperand(operand, str);
				requiredOp.remove(operand);
				return;
			}
			catch(RuntimeException e)
			{
				throw new IllegalArgumentException();
			}
		}

	}



	private void dealWithLongOption(String str, Bindings bindings, String[] args) {
		
		for(int i = 0; i < str.length();i++)
		{
			
			if(str.substring(i,i+1).equals("="))
			{
				
				
				String operand = str.substring(i+1, str.length());
				String option = str.substring(2, i);
				for(Option option2 : required)
				{
					ArrayList<String> shortFlag = new ArrayList<String>();
					ArrayList<String> longFlag = new ArrayList<String>();
					option2.getFlags(longFlag, shortFlag);
					if(longFlag.contains(option))
					{
						if(option2.hasOperand())
						{
							operandHas = true;
						}
						else{
							operandHas = false;
						}
						bindings.addOption(option2);
						try{
							bindings.bindOperand(option2.getOperand(), operand);
						}
						catch(RuntimeException e)
						{
							System.out.printf("Usage:");
							System.out.printf("Error:");
						}
					}
				}
				for(Option option2 : optional)
				{
					ArrayList<String> shortFlag = new ArrayList<String>();
					ArrayList<String> longFlag = new ArrayList<String>();
					option2.getFlags(longFlag, shortFlag);
					if(longFlag.contains(option))
					{
						if(option2.hasOperand())
						{
							operandHas = true;
						}
						else{
							operandHas = false;
						}
						bindings.addOption(option2);
						try{
							bindings.bindOperand(option2.getOperand(), operand);
						}
						catch(RuntimeException e)
						{
							System.out.printf("Usage:");
							System.out.printf("Error:");
						}
					}
				}
				for(Option option2 : requiredOneOf)
				{
					ArrayList<String> shortFlag = new ArrayList<String>();
					ArrayList<String> longFlag = new ArrayList<String>();
					option2.getFlags(longFlag, shortFlag);
					if(longFlag.contains(option))
					{
						if(option2.hasOperand())
						{
							operandHas = true;
						}
						else{
							operandHas = false;
						}
						bindings.addOption(option2);
						try{
							bindings.bindOperand(option2.getOperand(), operand);
						}
						catch(RuntimeException e)
						{
							System.out.printf("Usage:");
							System.out.printf("Error:");
						}
					}
				}
			}
		}
			if(count+1<args.length&&!args[count+1].startsWith("-"))
			{

				String option = str.substring(2, str.length());			
				for(Option option3 : required)
				{
					if(option3.hasOperand())
					{
						operandHas = true;
					}
					else{
						operandHas = false;
					}
					ArrayList<String> shortFlag = new ArrayList<String>();
					ArrayList<String> longFlag = new ArrayList<String>();
					option3.getFlags(longFlag, shortFlag);
					if(longFlag.contains(option)&&option3.hasOperand())
					{
						bindings.addOption(option3);
						try{
							bindings.bindOperand(option3.getOperand(), args[count+1]);
						}
						catch(RuntimeException e)
						{
							System.out.printf("Usage:");
							System.out.printf("Error:");
						}
					}	
				}
				for(Option option3 : requiredOneOf)
				{
					ArrayList<String> shortFlag = new ArrayList<String>();
					ArrayList<String> longFlag = new ArrayList<String>();
					option3.getFlags(longFlag, shortFlag);
					if(longFlag.contains(option)&&option3.hasOperand())
					{
						if(option3.hasOperand())
						{
							operandHas = true;
						}
						else{
							operandHas = false;
						}
						bindings.addOption(option3);
						try{
							bindings.bindOperand(option3.getOperand(), args[count+1]);
						}
						catch(RuntimeException e)
						{
							System.out.printf("Usage:");
							System.out.printf("Error:");
						}
					}
					
				}
				for(Option option3 : optional)
				{
					ArrayList<String> shortFlag = new ArrayList<String>();
					ArrayList<String> longFlag = new ArrayList<String>();
					option3.getFlags(longFlag, shortFlag);
					if(longFlag.contains(option)&&option3.hasOperand())
					{
						if(option3.hasOperand())
						{
							operandHas = true;
						}
						else{
							operandHas = false;
						}
						bindings.addOption(option3);
						try{
							bindings.bindOperand(option3.getOperand(), args[count+1]);
						}
						catch(RuntimeException e)
						{
							System.out.printf("Usage:");
							System.out.printf("Error:");
						}
					}
					
				}
			}
		}

	
	private void checkOperand(Bindings bindings)
	{
		
		//check for option missing operand
		for(Option option:bindings.options)
		{
			if(option.hasOperand())
			{
				if(!bindings.operands.containsKey(option.getOperand()))
				{
					
					throw new IllegalArgumentException();
				}
			}
		}
		//check for requiredOperand not exist
		for(Operand operand : requiredOp)
		{
			if(bindings.operands.containsKey(operand))
			{
				counterRequired++;
			}	
		}
		if(counterRequired == 0&&!requiredOp.isEmpty())
		{
			
			throw new IllegalArgumentException();
		}
		//check for requiredOneOf
		counterRequired = 0;
		for(Operand operand : oneOrMoreOp)
		{
			if(bindings.operands.containsKey(operand))
			{
				
				counterRequired++;
			}
		}
		if(counterRequired == 0&&!oneOrMoreOp.isEmpty())
		{
			
			throw new IllegalArgumentException();
		}
		//check for zeroOrMore
		counterRequired = 0;
		
	}
	
	
	private void checkExist(String strc, Bindings bindings, String[] args,boolean flagMode) {
		// TODO Auto-generated method stub
		
		String str = strc.substring(1);
		boolean exist = false;
		if(!flagMode);
		{
			for(Option option : required)
			{
				ArrayList<String> shortFlag = new ArrayList<String>();
				ArrayList<String> longFlag = new ArrayList<String>();
				option.getFlags(longFlag, shortFlag);
				if(longFlag.contains(str))
				{
					exist = true;
				}
			}
			for(Option option : optional)
			{
				ArrayList<String> shortFlag = new ArrayList<String>();
				ArrayList<String> longFlag = new ArrayList<String>();
				option.getFlags(longFlag, shortFlag);
				if(longFlag.contains(str))
				{
					exist = true;
				}
			}
			for(Option option : requiredOneOf)
			{
				ArrayList<String> shortFlag = new ArrayList<String>();
				ArrayList<String> longFlag = new ArrayList<String>();
				option.getFlags(longFlag, shortFlag);
				if(longFlag.contains(str))
				{
					exist = true;
				}
			}
			
		}
		if(flagMode);
		{
			for(Option option : required)
			{
				ArrayList<String> shortFlag = new ArrayList<String>();
				ArrayList<String> longFlag = new ArrayList<String>();
				option.getFlags(longFlag, shortFlag);
				if(shortFlag.contains(str))
				{
					
					exist = true;
				}
			}
			for(Option option : optional)
			{
				ArrayList<String> shortFlag = new ArrayList<String>();
				ArrayList<String> longFlag = new ArrayList<String>();
				option.getFlags(longFlag, shortFlag);
				if(shortFlag.contains(str))
				{
					
					exist = true;
				}
			}
			for(Option option : requiredOneOf)
			{
				ArrayList<String> shortFlag = new ArrayList<String>();
				ArrayList<String> longFlag = new ArrayList<String>();
				option.getFlags(longFlag, shortFlag);
				if(shortFlag.contains(str))
				{
					
					exist = true;
				}
			}
			
		}
		if(!exist)
		{
			throw new IllegalArgumentException();
		}
		
		
	}
	
	
	private void checkOption(Bindings bindings) {
		// TODO Auto-generated method stub
		int countOption = 0;
		int countOptionZero = 0;
		if(!required.isEmpty()&&bindings.options.isEmpty())
		{
			
			throw new IllegalArgumentException();
		}
		
		for(Option option : bindings.options)
		{
			if(requiredOneOf.contains(option))
			{

				countOption++;
			}
		}
		if(countOption!=1&&!requiredOneOf.isEmpty())
		{

			throw new IllegalArgumentException();
		}
		
		
	}
	
	}








