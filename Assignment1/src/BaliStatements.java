import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.TokenizerException;

public class BaliStatements {

    public static String getStatement(SamTokenizer f) {
        if (f.check("return")) {
            f.pushBack();
            String returnSamCode = getReturn(f);
            if(returnSamCode == null) return null;
        } else if (f.check("if")) {
            f.pushBack();
            String ifSamCode = getIf(f);
            if(ifSamCode == null) return null;
        } else if (f.check("while")) {
            f.pushBack();
            String whileSamCode = getWhile(f);
            if(whileSamCode == null) return null;
        } else if (f.check("break")) {
            f.pushBack();
            String breakSamCode = getBreak(f);
            if(breakSamCode == null) return null;
        } else if (f.check("{")) {
            f.pushBack();
            String blockSamCode = getBlock(f);
            if(blockSamCode == null) return null;
        }else if (f.check(";")) {
            // do nothing
        } else { // hopefully an ASSIGN
            f.pushBack();
            String assignSamCode = getAssign(f);
            if(assignSamCode == null) return null;
        }
        return "";
    }

    public static String getAssign(SamTokenizer f) {
        try {
            String writeVariable = f.getWord();
        } catch (TokenizerException exp) {
            System.out.println("Invalid variable name at line: " + f.lineNo());
            return null;
        }
        if (!f.check("=")) {
            System.out.println("Expecting '=' at line: " + f.lineNo());
            return null;
        }
        BaliExpression.getExp(f);
        return "";
    }

    public static String getBlock(SamTokenizer f) {
        f.check("{");

        while (!f.check("}")) {
            f.pushBack();
            getStatement(f);
        }
        return "";
    }

    public static String getBreak(SamTokenizer f) {
        f.check("break");

        if (!f.check(";")) {
            System.out.println("Expecting ';' at line: " + f.lineNo());
            return null;
        }
        return null;
    }

    public static String getWhile(SamTokenizer f) {
        f.check("while");

        if (!f.check("(")) {
            System.out.println("Expecting '(' at line: " + f.lineNo());
            return null;
        }
        BaliExpression.getExp(f);

        if (!f.check(")")) {
            System.out.println("Expecting ')' at line: " + f.lineNo());
            return null;
        }
        getStatement(f);
        return "";
    }

    public static String getIf(SamTokenizer f) {
        f.check("if");

        if (!f.check("(")) {
            System.out.println("Expecting '(' at line: " + f.lineNo());
            return null;
        }
        BaliExpression.getExp(f);

        if (!f.check(")")) {
            System.out.println("Expecting ')' at line: " + f.lineNo());
            return null;
        }
        getStatement(f);

        if (!f.check("else")) {
            System.out.println("Expecting 'else' at line: " + f.lineNo());
            return null;
        }

        getStatement(f);
        return null;
    }

    public static String getReturn(SamTokenizer f) {
        f.check("return");
        BaliExpression.getExp(f);
        if (!f.check(";")) {
            System.out.println("Expecting ';' at line: " + f.lineNo());
            return null;
        }
        return null;
    }
}
