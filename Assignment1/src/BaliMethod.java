import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.TokenizerException;

import java.util.ArrayList;
import java.util.List;

public class BaliMethod {
    public static class MethodMetaData {
        public SymbolTable symbolTable;
        public int nbrOfFormals;
        public int nbrOfLocals;
    }
    
    private SamTokenizer tokenizer;
    private MethodMetaData metaData;

    public BaliMethod(SamTokenizer t) {
        tokenizer = t;
        metaData = new MethodMetaData();
        metaData.nbrOfLocals = 0;
        metaData.nbrOfFormals = 0;
        metaData.symbolTable = new SymbolTable();
    }

    public String getMethod() {
        if (!tokenizer.check("int")) { //must match at begining
            System.out.println("Invalid return type in method declaration at line: " + tokenizer.lineNo());
            return null;
        }

        String methodName;
        try {
            methodName = tokenizer.getWord();
        } catch (TokenizerException exp) {
            System.out.println("Invalid method name at line " + tokenizer.lineNo());
            return null;
        }

        if (!tokenizer.check('(')) { // must be an opening parenthesis
            System.out.println("Expecting '(' at line: " + tokenizer.lineNo());
        }
        metaData.nbrOfFormals = getFormals();
        if (metaData.nbrOfFormals == -1) { // handled error occured
            return null;
        }

        // save label of function in labels map
        String functionLbl = BaliCompiler.methodLabelsMap
                .createNewEntry(methodName, metaData.nbrOfFormals);

        if (!tokenizer.check(')')) {  // must be an closing parenthesis
            System.out.println("Expecting ')' at line: " + tokenizer.lineNo());
        }

        if (!tokenizer.check('{')) { // must have an opening brace
            System.out.println("Expecting '{' at line: " + tokenizer.lineNo());
        }
        String body = getBody();
        if (body == null) { // handled error occured
            return null;
        }
        if (!tokenizer.check('}')) {// must have a closing brace
            System.out.println("Expecting '}' at line: " + tokenizer.lineNo());
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
                System.out.println("Expecting type (int) at line: " + tokenizer.lineNo());
                return -1;
            }
            try {
                String argName = tokenizer.getWord();
                paramsList.add(argName);
            } catch (TokenizerException e) {
                System.out.println("Invalid variable name at line: " + tokenizer.lineNo());
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
                metaData.symbolTable.createNewEntryForVariable(param, -n);
                metaData.symbolTable.setVariableInitialized(param);
            } catch (IllegalStateException exp) {
                System.out.println("Variable already defined at line: " + tokenizer.lineNo());
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
        while (!tokenizer.test('}')) {
            if (tokenizer.peekAtKind() == Tokenizer.TokenType.EOF) {
                System.out.println("Expecting '}' at line: " + (tokenizer.lineNo() + 1));
                return null;
            }
            String stmt = new BaliStatement(tokenizer, metaData).getStatement();
            if (stmt == null) {
                return null;
            }
            samCode += stmt;
        }
        return samCode;
    }

    private String getDeclaration() {
        tokenizer.check("int");
        String samCode = "";

        while(true) {
            //Increment number of locals
            metaData.nbrOfLocals++;
            samCode += "PUSHIMM 0\n";
            String variableName;

            try {
                variableName = tokenizer.getWord();
                //Add variable to the Symbol Table
                metaData.symbolTable.createNewEntryForVariable(variableName, metaData.nbrOfLocals + 1);
            } catch (TokenizerException e) {
                System.out.println("Invalid variable name at line: " + tokenizer.lineNo());
                return null;
            } catch (IllegalStateException exp){
                System.out.println("Variable already defined at line: " + tokenizer.lineNo());
                return null;
            }
            if(tokenizer.test('=')) {
                tokenizer.check('=');
                String expression = new BaliExpression(tokenizer, metaData).getExp();
                metaData.symbolTable.setVariableInitialized(variableName);
                int offset = metaData.symbolTable.lookupOffsetForVariable(variableName);
                samCode += expression;
                samCode += "STOREOFF " + offset +"\n";

                if (expression == null) {
                    return null;
                }
            }

            if(!tokenizer.test(',')) {
                break;
            } else {
                tokenizer.check(',');
            }
        }

        if(!tokenizer.check(';')) {
            System.out.println("Expecting ';' at line: " + tokenizer.lineNo());
            return null;
        }

        //Need to change
        return samCode;
    }
}
