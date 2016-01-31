import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.Tokenizer.TokenType;
import edu.cornell.cs.sam.io.TokenizerException;

public class BaliCompiler
{
	public static void main(String[] args) {
		String baliFileName = "ex.bali";//args[0];
		compiler(baliFileName);
	}

	public static String compiler(String fileName)
	{
		//returns SaM code for program in file
		try {
			SamTokenizer f = new SamTokenizer (fileName);
			String pgm = getProgram(f);
			return pgm;
		}
		catch (Exception e) {
			System.out.println("Fatal error: could not compile program");
			return "STOP\n";
		}
	}
	public static String getProgram(SamTokenizer f) {
		try {
			String pgm="";
			while(f.peekAtKind()!=TokenType.EOF) {
				getMethod(f);
			}
			return pgm;
		}
		catch(Exception e) {
			System.out.println("Fatal error: could not compile program");
			return "STOP\n";
		}
	}
	public static String getMethod(SamTokenizer f) {
		if (!f.check("int")) { //must match at begining
			System.out.println("Invalid return type in method declaration at line: " + f.lineNo());
		}
		String methodName;
		try {
			methodName = f.getWord();
		} catch (TokenizerException exp) {
			System.out.println("Invalid method name at line " + f.lineNo());
		}

		if (!f.check('(')) { // must be an opening parenthesis
			System.out.println("Expecting '(' at line: " + f.lineNo());
		}
		String formals = getFormals(f);
		if (formals == null) { // handled error occured
			return null;
		}

		if (!f.check(')')) {  // must be an closing parenthesis
			System.out.println("Expecting ')' at line: " + f.lineNo());
		}

		if (!f.check('{')) { // must have an opening brace
			System.out.println("Expecting '{' at line: " + f.lineNo());
		}
		String body = getBody(f);
		if (body == null) { // handled error occured
			return null;
		}
		if (!f.check('}')) {// must have a closing brace
			System.out.println("Expecting '}' at line: " + f.lineNo());
			return null;
		}

		return "";
	}

	public static String getFormals(SamTokenizer f){
		while(!f.getString().equals(")")) {
			f.pushBack();
			if (!f.check("int")) {
				System.out.println("Expecting type (int) at line: " + f.lineNo());
				return null;
			}
			String argName;
			try {
				argName = f.getWord();
			} catch (TokenizerException e) {
				System.out.println("Invalid variable name at line: " + f.lineNo());
				return null;
			}
			if (!f.getString().equals(",")) {
				f.pushBack(); // push back the (hopefully) closing parenthesis
				break;
			}
		}
		f.pushBack();
		return "";
	}

	public static String getBody(SamTokenizer f) {
		while (f.check("int")) {
			f.pushBack();
			getDeclaration(f);
		}
		f.pushBack();
		while (!f.getString().equals("}")) {
			f.pushBack();
			BaliStatements.getStatement(f);
		}
		f.pushBack();

		return "";
	}

	public static String getDeclaration(SamTokenizer f) {
		f.check("int");

		while(true) {
			String variableName;
			try {
				variableName = f.getWord();
			} catch (TokenizerException e) {
				System.out.println("Invalid variable name at line: " + f.lineNo());
				return null;
			}
			if(f.getString().equals("=")) {
				String expression = BaliExpression.getExp(f);
			} else {
				f.pushBack();
			}

			if(!f.getString().equals(",")) {
				f.pushBack();
				break;
			}
		}

		if(!f.getString().equals(";")) {
			System.out.println("Expecting ';' at line: " + f.lineNo());
			return null;
		}

		//Need to change
		return "";
	}
}

