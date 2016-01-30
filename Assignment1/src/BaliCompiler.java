import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.Tokenizer.TokenType;
import edu.cornell.cs.sam.io.TokenizerException;

public class BaliCompiler
{
	public static String compiler(String fileName)
	{
		//returns SaM code for program in file
		try
		{
			SamTokenizer f = new SamTokenizer (fileName);
			String pgm = getProgram(f);
			return pgm;
		}
		catch (Exception e)
		{
			System.out.println("Fatal error: could not compile program");
			return "STOP\n";
		}
	}
	public static String getProgram(SamTokenizer f)
	{
		try
		{
			String pgm="";
			while(f.peekAtKind()!=TokenType.EOF)
			{
				getMethod(f);
			}
			return pgm;
		}
		catch(Exception e)
		{
			System.out.println("Fatal error: could not compile program");
			return "STOP\n";
		}
	}
	public static String getMethod(SamTokenizer f)
	{
		//TODO: add code to convert a method declaration to SaM code.
		//Since the only data type is an int, you can safely check for int 
		//in the tokenizer.
		//TODO: add appropriate exception handlers to generate useful error msgs.
		f.check("int"); //must match at begining
		String methodName = f.getString();
		f.check ("("); // must be an opening parenthesis
		String formals = getFormals(f);
		f.check(")");  // must be an closing parenthesis
		f.check("{");
		getBody(f);
		f.check("}");

		//You would need to read in formals if any
		//And then have calls to getDeclarations and getStatements.
		return null;
	}

	public static String getFormals(SamTokenizer f){

		while(f.peekAtKind() != TokenType.CHARACTER) {
			f.check("int");
			String argName = f.getString();
			if (!f.check(",")) {
				f.pushBack(); // push back the (hopefully) parenthesis
				break;
			}
		}
		return null;
	}
	public static String getBody(SamTokenizer f) {
		f.check("{");
		while (f.check("int")) {
			f.pushBack();
			getDeclaration(f);
		}
		f.pushBack();
		while (!f.check("}")) {
			f.pushBack();
			BaliStatements.getStatement(f);
		}
		f.pushBack();

		return null;
	}

	public static String getDeclaration(SamTokenizer f) {
		try {
			f.check("int");

			while(true) {
				String variableName;
				try {
					variableName = f.getWord();
				} catch (TokenizerException e) {
					//Error
					//Invalid variable name

					return null;
				}
				if(f.check("=")) {
					String expression = BaliExpression.getExp(f);
				} else {
					f.pushBack();
				}

				if(!f.check(",")) {
					f.pushBack();
					break;
				}
			}

			if(!f.check(";")) {
				//Error
				//Statment must end with semicolon
				return null;
			}

			//Need to change
			return null;
		}

		catch (Exception e) {
			//
		}

		return null;


	}
}

