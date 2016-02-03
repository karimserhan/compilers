import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.TokenizerException;

/**
 * Bali Expression parser
 */
public class BaliExpression {
    private SamTokenizer tokenizer;
    private BaliMethod.MethodMetaData methodMeta;

    public BaliExpression(SamTokenizer t, BaliMethod.MethodMetaData meta) {
        tokenizer = t;
        this.methodMeta = meta;
    }

    /**
     * Parse the expression from the input file
     * @return the generated sam code
     */
    public String getExp() {
        String samCode = "";
        // Check if EXP is float. Throw error if float.
        if(tokenizer.peekAtKind() == Tokenizer.TokenType.FLOAT) {
            System.out.println("ERROR: Floating point numbers are not supported; at line: " + tokenizer.lineNo());
            return null;
        }
        //Check if EXP is Integer.
        //If Integer, PUSHIMM the value of the integer on the stack.
        if(tokenizer.peekAtKind() == Tokenizer.TokenType.INTEGER) {
            int value = tokenizer.getInt();
            samCode = "\tPUSHIMM" +  " " + value + "\n";
            return samCode;
        }
        //Check if EXP is true.
        //If true, PUSHIMM 1 on the stack.
        if(tokenizer.test("true")) {
            tokenizer.check("true");
            samCode = "\tPUSHIMM 1" + "\n";
            return samCode;
        }
        //Check if EXP is false.
        //If false, PUSHIMM 0 on the stack.
        if(tokenizer.test("false")) {
            tokenizer.check("false");
            samCode = "\tPUSHIMM 0" + "\n";
            return samCode;
        }

        // Check if next token is '('
        // If next token is '(', call getParenthesizedExp().
        if(tokenizer.test('(')) {
            return getParenthesizedExp();
        }

        //Case where EXP is either location or method.
        //Either way, get the variable name.
        String variableName;
        //Try getting the variable name.
        //If name is not a valid variable name, return "Invalid variable name" error.
        try {
            variableName = tokenizer.getWord();
        }
        catch (TokenizerException e) {
            System.out.println("ERROR: Malformed expression at line: " + tokenizer.lineNo());
            return null;
        }

        if (tokenizer.test('(')) {
            //Case where EXP is a method call.
            return handleMethodCall(variableName);
        } else {
            //Case where EXP is a variable.
            return handleVariableUse(variableName);
        }
    }

    private String getParenthesizedExp() {
        tokenizer.check('('); // will always be true
        String samCode = "";
        if (tokenizer.test('-')) { // unary operator '-'
            tokenizer.check('-');
            //Generate SAM code
            String expCode = getExp();
            if (expCode == null) { return null; }
            samCode = expCode;
            samCode += "\tPUSHIMM -1\n";
            samCode += "\tTIMES\n";
        } else if (tokenizer.test('!')){ // unary operator '!'
            if (!tokenizer.check('!')) {
                System.out.println("ERROR: Invalid unary operator at line: " + tokenizer.lineNo() + ". - and ! are the only valid unary operators.");
                return null;
            }
            String expCode = getExp();
            if (expCode == null) { return null; }
            samCode = expCode;
            samCode += "\tISNIL\n";
        } else { //binary operators or single parenthesized expression
            // read first expression
            String expSamCode = getExp();
            if (expSamCode == null) {
                return null;
            }
            samCode = expSamCode;

            if (nextTokenIsBinaryOp()) { // binrary operators
                char op = tokenizer.getOp();
                // read second expression
                expSamCode = getExp();
                if (expSamCode == null) {
                    return null;
                }
                samCode +=  expSamCode;
                samCode += handleBinaryOp(op);
            } else {// single parenthesized exression
            }
        }

        // eat up the remaining )
        if (!tokenizer.check(')')) {
            System.out.println("ERROR: Malformed expression at line: " + tokenizer.lineNo());
            return null;
        }

        return samCode;
    }

    private String handleBinaryOp(char op) {
        String samCode ="";
        switch (op){
            case '+':
                samCode += "\tADD\n";
                break;
            case '-':
                samCode += "\tSUB\n";
                break;
            case '*':
                samCode += "\tTIMES\n";
                break;
            case '/':
                samCode += "\tDIV\n";
                break;
            case '&':
                samCode += "\tAND\n";
                break;
            case '|':
                samCode += "\tOR\n";
                break;
            case '<':
                samCode += "\tLESS\n";
                break;
            case '>':
                samCode += "\tGREATER\n";
                break;
            case '=':
                samCode += "\tEQUAL\n";
                break;
        }

        return samCode;
    }

    private String handleVariableUse(String variableName) {
        try {
            //Get offset from symbol table.
            //PUSH variable value on top of the stack using offset.
            int offset = methodMeta.symbolTable.lookupOffset(variableName);
            return "\tPUSHOFF " + offset + "\n";
        } catch (IllegalArgumentException exp) {
            //Variable not declared.
            //Throw "Variable not declared" error.
            System.out.println("ERROR: Variable not declared: " + variableName + " at line: " + tokenizer.lineNo());
            return null;
        } catch (IllegalStateException exp) {
            //Variable not initialized.
            //Throw "Variable not initialized" error.
            System.out.println("ERROR: Variable " + variableName + " used before being initialized. At line: " + tokenizer.lineNo());
            return null;
        }
    }

    private String handleMethodCall(String methodName) {
        String samCode;
        tokenizer.check('(');
        // get label of function
        String label;
        int nbrOfFormals;
        try {
            label = BaliCompiler.methodLabelsMap.lookupLabel(methodName);
            nbrOfFormals = BaliCompiler.methodLabelsMap.lookupNumberOfParameters(methodName);
        } catch (IllegalArgumentException exp) {
            System.out.println("ERROR: Method not declared: " + methodName + " at line: " + tokenizer.lineNo());
            return null;
        }

        // Variable to be passed to getActuals to return the number of actual parameters
        // by reference
        int[] nbrOfActualsArr = new int[1];

        // generate sam code
        samCode = "\tPUSHIMM 0\n"; // create return value slot
        String actualsSamCode = getActuals(nbrOfActualsArr); // push parameters on stack
        if (actualsSamCode == null) { // error, don't proceed
            return null;
        }
        if (nbrOfFormals != nbrOfActualsArr[0]) {
            System.out.println("ERROR: Argument list for function " + methodName + " does not match formal parameters. At line: " + tokenizer.lineNo());
            return null;
        }

        samCode += actualsSamCode;
        samCode += "\tLINK\n"; // save FBR
        samCode += "\tJSR " + label + "\n"; // jump to function

        if (!tokenizer.check(')')) {
            System.out.println("ERROR: Malformed expression at line: " + tokenizer.lineNo());
            return null;
        }

        // clean up code after method was called
        samCode += "\tPOPFBR\n";
        samCode += "\tADDSP -" + nbrOfActualsArr[0] + "\n";
        return samCode; //fix duh
    }

    private String getActuals(int[] numberOfParameters) {
        numberOfParameters[0] = 0;
        String samCode = "";
        while(!tokenizer.test(')')) {
            numberOfParameters[0]++;
            String expression = getExp();
            if(expression == null) return null;
            samCode += expression;
            if(!tokenizer.test(',')) {
                break;
            } else {
                tokenizer.check(',');
            }
        }
        return samCode;
    }

    private boolean nextTokenIsBinaryOp() {
        return tokenizer.test('+') || tokenizer.test('-') || tokenizer.test('*') || tokenizer.test('/')
                || tokenizer.test('&') || tokenizer.test('|') || tokenizer.test('<') || tokenizer.test('>')
                || tokenizer.test('=');
    }
}
