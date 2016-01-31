import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.TokenizerException;

public class BaliExpressions {

    public static String getExp(SamTokenizer f) {
        // Literal case
        if(f.peekAtKind() == Tokenizer.TokenType.FLOAT) {
            System.out.println("Floating point numbers are not supported; at line: " + f.lineNo());
            return null;
        }
        if(f.peekAtKind() == Tokenizer.TokenType.INTEGER) {
            return f.getInt() + "";
        }
        if(f.test("true")) {
            f.check("true");
            return "1";
        }
        if(f.test("false")) {
            f.check("false");
            return "0";
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
            String actualsSamCode = getActuals(f);
            if(actualsSamCode == null) return null;
            if (!f.check(')')) {
                System.out.println("Expecting ')' at line: " + f.lineNo());
                return null;
            }
            return ""; //fix duh
        } else { // variable use
            return variableName;
        }
    }

    private static String getParenthesizedExp(SamTokenizer f) {
        f.check('(');
        String result;

        if (f.test('-')) { // unary operator 1
            f.check('-');
            //Generate SAM code
            result = "-" + getExp(f);
        } else if (f.test('!')){ // unary operator 2
            if (!f.check('!')) {
                System.out.println("Invalid unary operator at line: " + f.lineNo() + ". - and ! are the only valid unary operators.");
                return null;
            }
            result = "!" + getExp(f);
        } else { //binary operators or single parenthesized expression
            result = getExp(f);

            // binrary operators
            if (f.peekAtKind() == Tokenizer.TokenType.OPERATOR) {
                char op = f.getOp();
                // generate sam code
                result += op + getExp(f);
            } else {// single parenthesized exression

            }
        }
        // eat up the remaining )
        if (!f.check(')')) {
            System.out.println("Expecting ')' at line: " + f.lineNo());
            return null;
        }

        return result;
    }

    private static String getActuals(SamTokenizer f) {

        while(!f.test(')')) {
            String expression = getExp(f);
            if(expression == null) return null;
            if(!f.test(',')) {
                break;
            } else {
                f.check(',');
            }
        }
        return "";
    }
}
