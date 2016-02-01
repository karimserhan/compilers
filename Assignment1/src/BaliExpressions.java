import com.sun.javaws.exceptions.InvalidArgumentException;
import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.TokenizerException;

public class BaliExpressions {

    public static String getExp(SamTokenizer f) {

        String samCode = "";
        // Literal case
        if(f.peekAtKind() == Tokenizer.TokenType.FLOAT) {
            System.out.println("Floating point numbers are not supported; at line: " + f.lineNo());
            return null;
        }
        if(f.peekAtKind() == Tokenizer.TokenType.INTEGER) {
            int value = f.getInt();
            samCode = "PUSHIMM" +  " " + value + "\n";
            return samCode;
        }
        if(f.test("true")) {
            f.check("true");
            samCode = "PUSHIMM 1" + "\n";
            return samCode;
        }
        if(f.test("false")) {
            f.check("false");
            samCode = "PUSHIMM 0" + "\n";
            return samCode;
        }

        // not a literal case
        if(f.test('(')) {
            return getParenthesizedExp(f);
        }

        //Method or Location
        String variableName;
        try {
            variableName = f.getWord();
        }
        catch (TokenizerException e) {
            System.out.println("Invalid variable name at line: " + f.lineNo());
            return null;
        }

        if (f.test('(')) { // method call
            f.check('(');
            // get label of function
            try {
                String label = BaliCompiler.functionsLabelsMap.lookupLabelForFunction(variableName);
            } catch (IllegalArgumentException exp) {
                System.out.println("Method not declared: " + variableName + " at line: " + f.lineNo());
                return null;
            }
            // generate sam code
            samCode = "PUSHIMM 0\n"; // create return value slot
            String actualsSamCode = getActuals(f); // push parameters on stack
            if (actualsSamCode == null) { // error, don't proceed
                return null;
            }
            samCode += actualsSamCode;
            samCode += "LINK\n"; // save FBR
            samCode += "JSR " + label + "\n"; // jump to function

            if (!f.check(')')) {
                System.out.println("Expecting ')' at line: " + f.lineNo());
                return null;
            }
            return samCode; //fix duh
        } else { // variable use
            try {
                int offset = BaliCompiler.currentSymbolTable.lookupOffsetForVariable(variableName);
                return "PUSHOFF " + offset + "\n";
            } catch (IllegalArgumentException exp) {
                System.out.println("Variable not declared: " + variableName + " at line: " + f.lineNo());
                return null;
            }
        }
    }

    private static String getParenthesizedExp(SamTokenizer f) {
        f.check('(');
        String samCode="";
        if (f.test('-')) { // unary operator 1
            f.check('-');
            //Generate SAM code
            samCode = getExp(f);
            samCode += "PUSHIMM -1\n";
            samCode += "TIMES\n";

        } else if (f.test('!')){ // unary operator 2
            if (!f.check('!')) {
                System.out.println("Invalid unary operator at line: " + f.lineNo() + ". - and ! are the only valid unary operators.");
                return null;
            }
            samCode = getExp(f);
            samCode += "ISNIL\n";
        } else { //binary operators or single parenthesized expression
            samCode = getExp(f);

            // binrary operators
            if (nextTokenIsBinaryOp(f)) {
                char op = f.getOp();
                samCode +=  getExp(f);
                samCode += handleBinaryOp(op);
            } else {// single parenthesized exression
            }
        }
        // eat up the remaining )
        if (!f.check(')')) {
            System.out.println("Expecting ')' at line: " + f.lineNo());
            return null;
        }

        return samCode;
    }

    private static boolean nextTokenIsBinaryOp(SamTokenizer f) {
        return f.test('+') || f.test('-') || f.test('*') || f.test('/')
                || f.test('&') || f.test('|') || f.test('<') || f.test('>')
                || f.test('=');
    }

    private static String handleBinaryOp(char op) {
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

    private static String getActuals(SamTokenizer f) {
        String samCode = "";
        while(!f.test(')')) {
            String expression = getExp(f);
            if(expression == null) return null;
            if(!f.test(',')) {
                break;
            } else {
                f.check(',');
            }
            samCode += expression;
        }
        return samCode;
    }
}
