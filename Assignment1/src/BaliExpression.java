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
            //Error: Wrong name
            return null;
        }

        if (f.check("(")) { // method call
            f.pushBack();
            getActuals(f);
            if (!f.check(")")) {
                // error
                return null;
            }
            return ""; //fix duh
        } else { // variable use
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
                    //Error
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
            //Error
            return null;
        }

        return result;
    }

    private static String getActuals(SamTokenizer f) {
        return null;
    }
}
