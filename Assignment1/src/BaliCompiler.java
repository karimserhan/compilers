import edu.cornell.cs.sam.core.Sys;
import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer.TokenType;
import edu.cornell.cs.sam.io.TokenizerException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BaliCompiler
{
	public static LabelsMap functionsLabelsMap = new LabelsMap();
	public static SymbolTable currentSymbolTable = new SymbolTable();
	public static int currentNbrOfFormals;
	public static int currentNbrOfLocals;

	public static void main(String[] args) {
		String baliFileName = "ex.bali";//args[0];
		System.out.println(compiler(baliFileName));
	}

	public static String compiler(String fileName) {
		//returns SaM code for program in file
		try {
			SamTokenizer f = new SamTokenizer (fileName);
			String pgm = getProgram(f);
			return pgm;
		}
		catch (IOException e) {
			System.out.println("Fatal error: could not compile program");
			return "STOP\n";
		}
	}

	public static String getProgram(SamTokenizer f) {
		try {
			String pgm="";
			while(f.peekAtKind()!=TokenType.EOF) {
				String methodSamCode = getMethod(f);
				if(methodSamCode != null) pgm += methodSamCode;
				else return null;
			}
			return pgm;
		}
		catch(Exception e) {
			System.out.println("Fatal error: could not compile program");
			return "STOP\n";
		}
	}

	public static String getMethod(SamTokenizer f) {
		// reset function state
		BaliCompiler.currentNbrOfLocals = 0;
		BaliCompiler.currentNbrOfFormals = 0;
		BaliCompiler.currentSymbolTable = new SymbolTable();

		if (!f.check("int")) { //must match at begining
			System.out.println("Invalid return type in method declaration at line: " + f.lineNo());
			return null;
		}

		String methodName;
		try {
			methodName = f.getWord();
		} catch (TokenizerException exp) {
			System.out.println("Invalid method name at line " + f.lineNo());
			return null;
		}

		if (!f.check('(')) { // must be an opening parenthesis
			System.out.println("Expecting '(' at line: " + f.lineNo());
		}
		int nbrOfFormals = getFormals(f);
		if (nbrOfFormals == -1) { // handled error occured
			return null;
		}

		// save label of function in labels map
		String functionLbl = BaliCompiler.functionsLabelsMap
				.createNewEntryForFunction(methodName, nbrOfFormals);

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

		String samCode = functionLbl + ":\n";
		samCode += body;
		return samCode;
	}

	public static int getFormals(SamTokenizer f){
		List<String> paramsList = new ArrayList<String>();

		// parse the parameters
		while(!f.test(')')) {
			if (!f.check("int")) {
				System.out.println("Expecting type (int) at line: " + f.lineNo());
				return -1;
			}
			try {
				String argName = f.getWord();
				paramsList.add(argName);
			} catch (TokenizerException e) {
				System.out.println("Invalid variable name at line: " + f.lineNo());
				return -1;
			}
			if (!f.test(',')) {
				break;
			} else {
				f.check(',');
			}
		}

		// store parameters in symbol table
		int n = paramsList.size();
		for (String param : paramsList) {
			BaliCompiler.currentSymbolTable.createNewEntryForVariable(param, -n);
			n--;
		}

		return paramsList.size();
	}

	public static String getBody(SamTokenizer f) {
		String samCode = "";
		while (f.test("int")) {
			String decl = getDeclaration(f);
			if (decl == null) {
				return null;
			}
			samCode += decl;
		}
		while (!f.test('}')) {
			if (f.peekAtKind() == TokenType.EOF) {
				System.out.println("Expecting '}' at line: " + (f.lineNo() + 1));
				return null;
			}
			String stmt = BaliStatements.getStatement(f);
			if (stmt == null) {
				return null;
			}
			samCode += stmt;
		}
		return samCode;
	}

	public static String getDeclaration(SamTokenizer f) {
		f.check("int");
		String samCode = "";

		while(true) {
			//Increment number of locals
			currentNbrOfLocals++;
			samCode += "PUSHIMM 0";
			String variableName;

			try {
				variableName = f.getWord();
				//Add variable to the Symbol Table
				currentSymbolTable.createNewEntryForVariable(variableName,currentNbrOfLocals +1);
			} catch (TokenizerException e) {
				System.out.println("Invalid variable name at line: " + f.lineNo());
				return null;
			}
			if(f.test('=')) {
				f.check('=');
				String expression = BaliExpressions.getExp(f);
				int offset = currentSymbolTable.lookupOffsetForVariable(variableName);
				samCode += "STOREOFF " + offset;
				if (expression == null) {
					return null;
				}
			}

			if(!f.test(',')) {
				break;
			} else {
				f.check(',');
			}
		}

		if(!f.check(';')) {
			System.out.println("Expecting ';' at line: " + f.lineNo());
			return null;
		}

		//Need to change
		return "";
	}
}

