import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.TokenizerException;

public class BaliStatements {

    public static String getStatement(SamTokenizer f) {
        if (f.test("return")) {
            String returnSamCode = getReturn(f);
            if(returnSamCode == null) return null;
        } else if (f.test("if")) {
            String ifSamCode = getIf(f);
            if(ifSamCode == null) return null;
        } else if (f.test("while")) {
            String whileSamCode = getWhile(f);
            if(whileSamCode == null) return null;
        } else if (f.test("break")) {
            String breakSamCode = getBreak(f);
            if(breakSamCode == null) return null;
        } else if (f.test('{')) {
            String blockSamCode = getBlock(f);
            if(blockSamCode == null) return null;
        }else if (f.test(';')) {
            // do nothing
            f.check(';');
        } else { // hopefully an ASSIGN
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
        if (!f.test("=")) {
            System.out.println("Expecting '=' at line: " + f.lineNo());
            return null;
        }
        f.check('=');
        BaliExpressions.getExp(f);
        return "";
    }

    public static String getBlock(SamTokenizer f) {
        f.check('{');

        while (!f.test('}')) {
            getStatement(f);
        }
        return "";
    }

    public static String getBreak(SamTokenizer f) {
        f.check("break");

        if (!f.test(';')) {
            System.out.println("Expecting ';' at line: " + f.lineNo());
            return null;
        }
        return null;
    }

    public static String getWhile(SamTokenizer f) {
        f.check("while");

        if (!f.test('(')) {
            System.out.println("Expecting '(' at line: " + f.lineNo());
            return null;
        }
        f.check('(');
        BaliExpressions.getExp(f);

        if (!f.test(')')) {
            System.out.println("Expecting ')' at line: " + f.lineNo());
            return null;
        }
        f.check(')');
        getStatement(f);
        return "";
    }

    public static String getIf(SamTokenizer f) {
        f.check("if");

        if (!f.test('(')) {
            System.out.println("Expecting '(' at line: " + f.lineNo());
            return null;
        }
        f.check('(');
        BaliExpressions.getExp(f);

        if (!f.test(')')) {
            System.out.println("Expecting ')' at line: " + f.lineNo());
            return null;
        }
        f.check(')');
        getStatement(f);

        if (!f.test("else")) {
            System.out.println("Expecting 'else' at line: " + f.lineNo());
            return null;
        }
        f.check("else");
        getStatement(f);
        return null;
    }

    public static String getReturn(SamTokenizer f) {
        f.check("return");
        BaliExpressions.getExp(f);
        if (!f.test(';')) {
            System.out.println("Expecting ';' at line: " + f.lineNo());
            return null;
        }
        f.check(';');
        return null;
    }
}
