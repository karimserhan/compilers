import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.TokenizerException;

public class BaliExpression {
    private SamTokenizer tokenizer;
    private BaliMethod.MethodMetaData methodMeta;

    public BaliExpression(SamTokenizer t, BaliMethod.MethodMetaData meta) {
        tokenizer = t;
        this.methodMeta = meta;
    }

    public String getExp() {
        String samCode = "";
        // Literal case
        if(tokenizer.peekAtKind() == Tokenizer.TokenType.FLOAT) {
            System.out.println("Floating point numbers are not supported; at line: " + tokenizer.lineNo());
            return null;
        }
        if(tokenizer.peekAtKind() == Tokenizer.TokenType.INTEGER) {
            int value = tokenizer.getInt();
            samCode = "PUSHIMM" +  " " + value + "\n";
            return samCode;
        }
        if(tokenizer.test("true")) {
            tokenizer.check("true");
            samCode = "PUSHIMM 1" + "\n";
            return samCode;
        }
        if(tokenizer.test("false")) {
            tokenizer.check("false");
            samCode = "PUSHIMM 0" + "\n";
            return samCode;
        }

        // not a literal case
        if(tokenizer.test('(')) {
            return getParenthesizedExp();
        }

        //Method or Location
        String variableName;
        try {
            variableName = tokenizer.getWord();
        }
        catch (TokenizerException e) {
            System.out.println("Invalid variable name at line: " + tokenizer.lineNo());
            return null;
        }

        if (tokenizer.test('(')) { // method call
            return handleMethodCall(variableName);
        } else { // variable use
            return handleVariableUse(variableName);
        }
    }

    private String getParenthesizedExp() {
        tokenizer.check('('); // will always be true
        String samCode = "";
        if (tokenizer.test('-')) { // unary operator '-'
            tokenizer.check('-');
            //Generate SAM code
            samCode = getExp();
            samCode += "PUSHIMM -1\n";
            samCode += "TIMES\n";
        } else if (tokenizer.test('!')){ // unary operator '!'
            if (!tokenizer.check('!')) {
                System.out.println("Invalid unary operator at line: " + tokenizer.lineNo() + ". - and ! are the only valid unary operators.");
                return null;
            }
            samCode = getExp();
            samCode += "ISNIL\n";
        } else { //binary operators or single parenthesized expression
            samCode = getExp();

            // binrary operators
            if (nextTokenIsBinaryOp()) {
                char op = tokenizer.getOp();
                samCode +=  getExp();
                samCode += handleBinaryOp(op);
            } else {// single parenthesized exression
            }
        }

        // eat up the remaining )
        if (!tokenizer.check(')')) {
            System.out.println("Expecting ')' at line: " + tokenizer.lineNo());
            return null;
        }

        return samCode;
    }

    private String handleBinaryOp(char op) {
        String samCode ="";
        switch (op){
            case '+':
                samCode += "ADD\n";
                break;
            case '-':
                samCode += "SUB\n";
                break;
            case '*':
                samCode += "TIMES\n";
                break;
            case '/':
                samCode += "DIV\n";
                break;
            case '&':
                samCode += "AND\n";
                break;
            case '|':
                samCode += "OR\n";
                break;
            case '<':
                samCode += "LESS\n";
                break;
            case '>':
                samCode += "GREATER\n";
                break;
            case '=':
                samCode += "EQUAL\n";
                break;
        }

        return samCode;
    }

    private String handleVariableUse(String variableName) {
        try {
            int offset = methodMeta.symbolTable.lookupOffsetForVariable(variableName);
            return "PUSHOFF " + offset + "\n";
        } catch (IllegalArgumentException exp) {
            System.out.println("Variable not declared: " + variableName + " at line: " + tokenizer.lineNo());
            return null;
        } catch (IllegalStateException exp) {
            System.out.println("Variable " + variableName + " used before being initialized. At line: " + tokenizer.lineNo());
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
            System.out.println("Method not declared: " + methodName + " at line: " + tokenizer.lineNo());
            return null;
        }

        // Variable to be passed to getActuals to return the number of actual parameters
        // by reference
        int[] nbrOfActualsArr = new int[1];

        // generate sam code
        samCode = "PUSHIMM 0\n"; // create return value slot
        String actualsSamCode = getActuals(nbrOfActualsArr); // push parameters on stack
        if (actualsSamCode == null) { // error, don't proceed
            return null;
        }
        if (nbrOfFormals != nbrOfActualsArr[0]) {
            System.out.println("Argument list for function " + methodName + " does not match formal parameters. At line: " + tokenizer.lineNo());
            return null;
        }

        samCode += actualsSamCode;
        samCode += "LINK\n"; // save FBR
        samCode += "JSR " + label + "\n"; // jump to function

        if (!tokenizer.check(')')) {
            System.out.println("Expecting ')' at line: " + tokenizer.lineNo());
            return null;
        }

        // clean up code after method was called
        samCode += "POPFBR\n";
        samCode += "ADDSP -" + nbrOfActualsArr[0] + "\n";
        return samCode; //fix duh
    }

    private String getActuals(int[] numberOfParameters) {
        numberOfParameters[0] = 0;
        String samCode = "";
        while(!tokenizer.test(')')) {
            numberOfParameters[0]++;
            String expression = getExp();
            if(expression == null) return null;
            if(!tokenizer.test(',')) {
                break;
            } else {
                tokenizer.check(',');
            }
            samCode += expression;
        }
        return samCode;
    }

    private boolean nextTokenIsBinaryOp() {
        return tokenizer.test('+') || tokenizer.test('-') || tokenizer.test('*') || tokenizer.test('/')
                || tokenizer.test('&') || tokenizer.test('|') || tokenizer.test('<') || tokenizer.test('>')
                || tokenizer.test('=');
    }
}
