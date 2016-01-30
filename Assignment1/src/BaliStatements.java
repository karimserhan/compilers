import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.TokenizerException;

public class BaliStatements {

    public static String getStatement(SamTokenizer f) {
        if (f.check("return")) {
            f.pushBack();
            getReturn(f);
        } else if (f.check("if")) {
            f.pushBack();
            getIf(f);
        } else if (f.check("while")) {
            f.pushBack();
            getWhile(f);
        } else if (f.check("break")) {
            f.pushBack();
            getBreak(f);
        } else if (f.check("{")) {
            f.pushBack();
            getBlock(f);
        } else { // hopefully an ASSIGN
            f.pushBack();
            getAssign(f);
        }
        return "";
    }

    public static String getAssign(SamTokenizer f) {
        try {
            String writeVariable = f.getWord();
        } catch (TokenizerException exp) {
            // error
            return null;
        }
        if (!f.check("=")) {
            // error
            return null;
        }
        BaliExpression.getExp(f);
        return null;
    }

    public static String getBlock(SamTokenizer f) {
        return null;
    }

    public static String getBreak(SamTokenizer f) {
        return null;
    }

    public static String getWhile(SamTokenizer f) {
        return null;
    }

    public static String getIf(SamTokenizer f) {
        return null;
    }

    public static String getReturn(SamTokenizer f) {
        return null;
    }
}
