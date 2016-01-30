import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.TokenizerException;

public class BaliExpression {

    public static String getExp(SamTokenizer f) {
        // Literal case
        if(f.peekAtKind() == Tokenizer.TokenType.INTEGER) {
            return f.getString();
        }
        if(f.check("true")) {
            return "1";
        }
        else {
            f.pushBack();
        }
        if(f.check("false")) {
            return "0";
        }
        else {
            f.pushBack();
        }

        // not a literal case
        if(f.check("(")) {
            f.pushBack();
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

        if (f.check("(")) { // method call
            String actualsSamCode = getActuals(f);
            if(actualsSamCode == null) return null;
            if (!f.check(")")) {
                System.out.println("Expecting ')' at line: " + f.lineNo());
                return null;
            }
            return ""; //fix duh
        } else { // variable use
            f.pushBack();
            return variableName;
        }
    }

    private static String getParenthesizedExp(SamTokenizer f) {
        f.check("(");
        String result;

        // unary operators
        if(f.peekAtKind() == Tokenizer.TokenType.OPERATOR) {
            if (f.check("-")) {
                //Generate SAM code
                result = "-" + getExp(f);
            } else {
                f.pushBack();
                if (!f.check("!")) {
                    System.out.println("Invalid unary operator at line: " + f.lineNo() + "- and ! are the only valid unary operators.")
                    return null;
                }
                result = "!" + getExp(f);
            }
        } else { //binary operators or single parenthesized expression
            result = getExp(f);

            // binrary operators
            if (f.peekAtKind() == Tokenizer.TokenType.OPERATOR) {
                String op = f.getString();
                // generate sam code
                result += op + getExp(f);
            } else {// single parenthesized exression

            }
        }
        // eat up the remaining )
        if (!f.check(")")) {
            System.out.println("Expecting ')' at line: " f.lineNo());
            return null;
        }

        return result;
    }

    private static String getActuals(SamTokenizer f) {

        while(!f.check(")")) {
            f.pushBack();
            String expression = getExp(f);
            if(expression == null) return null;
            if(!f.check(",")) {
                break;
            }
        }
        f.pushBack();
        return "";
    }
}
