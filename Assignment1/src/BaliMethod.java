import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.TokenizerException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Bali Method parser
 */
public class BaliMethod {
    /**
     * Structure that holds method meta data to be used by the compiler
     */
    public static class MethodMetaData {
        public SymbolTable symbolTable;
        public int nbrOfFormals;
        public int nbrOfLocals;
    }
    
    private SamTokenizer tokenizer;
    private MethodMetaData metaData;

    // set containing the list of variables known to be initialized at a particular point in the file
    private HashSet<String> initializedVars;

    public BaliMethod(SamTokenizer t) {
        tokenizer = t;
        metaData = new MethodMetaData();
        metaData.nbrOfLocals = 0;
        metaData.nbrOfFormals = 0;
        metaData.symbolTable = new SymbolTable();
        initializedVars = new HashSet<String>();
    }

    /**
     * Parse the method from the input file
     * @return the generated sam code
     */
    public String getMethod() {
        if (!tokenizer.check("int")) { //must match at begining
            System.out.println("ERROR: Invalid return type in method declaration at line: " + tokenizer.nextLineNo());
            return null;
        }

        String methodName;
        try {
            methodName = tokenizer.getWord();
        } catch (TokenizerException exp) {
            System.out.println("ERROR: Invalid method name at line " + tokenizer.lineNo());
            return null;
        }

        if (!tokenizer.check('(')) { // must be an opening parenthesis
            System.out.println("ERROR: Expecting '(' at line: " + tokenizer.lineNo());
            return null;
        }
        metaData.nbrOfFormals = getFormals();
        if (metaData.nbrOfFormals == -1) { // handled error occured
            return null;
        }

        // save label of function in labels map
        String functionLbl = BaliCompiler.methodLabelsMap
                .createNewEntry(methodName, metaData.nbrOfFormals);

        if (!tokenizer.check(')')) {  // must be an closing parenthesis
            System.out.println("ERROR: Expecting ')' at line: " + tokenizer.lineNo());
            return  null;
        }

        if (!tokenizer.check('{')) { // must have an opening brace
            System.out.println("ERROR: Expecting '{' at line: " + tokenizer.lineNo());
            return null;
        }
        String body = getBody();
        if (body == null) { // handled error occured
            return null;
        }
        if (!tokenizer.check('}')) {// must have a closing brace
            System.out.println("ERROR: Expecting '}' at line: " + tokenizer.lineNo());
            return null;
        }

        String samCode = functionLbl + ":\n";
        samCode += body;
        return samCode;
    }

    private int getFormals(){
        List<String> paramsList = new ArrayList<String>();

        // parse the parameters
        while(!tokenizer.test(')')) {
            if (!tokenizer.check("int")) {
                System.out.println("ERROR: Expecting type (int) at line: " + tokenizer.lineNo());
                return -1;
            }
            try {
                String argName = tokenizer.getWord();
                paramsList.add(argName);
            } catch (TokenizerException e) {
                System.out.println("ERROR: Invalid variable name at line: " + tokenizer.lineNo());
                return -1;
            }
            if (!tokenizer.test(',')) {
                break;
            } else {
                tokenizer.check(',');
            }
        }

        // store parameters in symbol table
        int n = paramsList.size();
        for (String param : paramsList) {
            try {
                metaData.symbolTable.createNewEntry(param, -n);
                initializedVars.add(param); // mark parameter as an initialized variable
            } catch (IllegalStateException exp) {
                System.out.println("ERROR: Variable already defined at line: " + tokenizer.lineNo());
                return -1;
            }
            n--;
        }

        return paramsList.size();
    }

    private String getBody() {
        String samCode = "";
        while (tokenizer.test("int")) {
            String decl = getDeclaration();
            if (decl == null) {
                return null;
            }
            samCode += decl;
        }

        boolean doesReturn = false;
        boolean printedWarningMsg = false;

        while (!tokenizer.test('}')) {
            if (tokenizer.peekAtKind() == Tokenizer.TokenType.EOF) {
                System.out.println("ERROR: Expecting '}' at line: " + (tokenizer.lineNo() + 1));
                return null;
            }
            // print warning message
            if (doesReturn && !printedWarningMsg) {
                System.out.println("WARNING: unreachable statement, line: " + tokenizer.nextLineNo());
                printedWarningMsg = true;
            }
            // parse statement
            BaliStatement stmt = new BaliStatement(tokenizer, metaData, initializedVars);
            String stmtSamCode = stmt.getStatement();
            if (stmtSamCode == null) {
                return null;
            }
            // set return flag
            if (stmt.doesReturn()) {
                doesReturn = true;
            }
            samCode += stmtSamCode;
        }

        if (!doesReturn) {
            System.out.println("ERROR: Not all control paths return a value. At line: " + tokenizer.lineNo());
            return null;
        }
        return samCode;
    }

    private String getDeclaration() {
        tokenizer.check("int");
        String samCode = "";

        while(true) {
            //Increment number of locals
            metaData.nbrOfLocals++;
            boolean varAssigned = false;

            samCode += "\tPUSHIMM 0\n";
            String variableName;

            try {
                variableName = tokenizer.getWord();
                if (!isValidVarName(variableName)) {
                    System.out.println("ERROR: Cannot used reserved keyword as a variable name, at line: " + tokenizer.lineNo());
                    return null;
                }

                //Add variable to the Symbol Table
                metaData.symbolTable.createNewEntry(variableName, metaData.nbrOfLocals + 1);
            } catch (TokenizerException e) {
                System.out.println("ERROR: Invalid variable name at line: " + tokenizer.lineNo());
                return null;
            } catch (IllegalStateException exp){
                System.out.println("ERROR: Variable already defined at line: " + tokenizer.lineNo());
                return null;
            }
            if(tokenizer.test('=')) {
                varAssigned = true;
                tokenizer.check('=');
                String expression = new BaliExpression(tokenizer, metaData, initializedVars).getExp();
                if (expression == null) { return null; }
                initializedVars.add(variableName);
                int offset = metaData.symbolTable.lookupOffset(variableName);
                samCode += expression;
                samCode += "\tSTOREOFF " + offset +"\n";

                if (expression == null) {
                    return null;
                }
            }

            if (!tokenizer.test(',') && !tokenizer.test(';')) {
                if (!varAssigned) {
                    System.out.println("ERROR: Expecting ';' at line: " + tokenizer.lineNo());
                } else {
                    System.out.println("ERROR: Malformed expression at line: " + tokenizer.lineNo() + ". Did you miss a ';'?");
                }
                return null;
            }

            if(!tokenizer.test(',')) {
                break;
            } else {
                tokenizer.check(',');
            }
        }
        tokenizer.check(';');

        //Need to change
        return samCode;
    }

    public boolean isValidVarName(String name) {
        return !name.equals("int") && !name.equals("return") && !name.equals("if")
                && !name.equals("else") && !name.equals("while") && !name.equals("break")
                && !name.equals("true") && !name.equals("false");
    }
}
