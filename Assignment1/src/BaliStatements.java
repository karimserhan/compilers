import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.TokenizerException;

public class BaliStatements {
    private static int lastLabelIndexUsed = 0;
    private static String currentWhileLabel = null;


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
        if (!f.test('=')) {
            System.out.println("Expecting '=' at line: " + f.lineNo());
            return null;
        }
        f.check('=');
        String exp = BaliExpressions.getExp(f);
        if (exp == null) { // ma akal
            return null;
        }

        if (!f.check(';')) {
            System.out.println("Expecting ';' at line: " + f.lineNo());
            return null;
        }
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

        if (!f.check(';')) {
            System.out.println("Expecting ';' at line: " + f.lineNo());
            return null;
        }

        if (currentWhileLabel == null) {
            System.out.println("Cannot have a break statement outside of a while loop, at line: " + f.lineNo());
            return null;
        }

        String currentWhileLabelEnd = currentWhileLabel + "End";
        String samCode = "JUMP " + currentWhileLabelEnd;
        return samCode;
    }

    public static String getWhile(SamTokenizer f) {
        currentWhileLabel = "whileLbl" + lastLabelIndexUsed;
        lastLabelIndexUsed++;
        String currentWhileLabelEnd = currentWhileLabel + "End";

        f.check("while");

        if (!f.check('(')) {
            System.out.println("Expecting '(' at line: " + f.lineNo());
            return null;
        }
        String expSamCode = BaliExpressions.getExp(f);
        if (expSamCode == null) {
            return null;
        }

        if (!f.check(')')) {
            System.out.println("Expecting ')' at line: " + f.lineNo());
            return null;
        }

        String samCode = currentWhileLabelEnd + ":\n";
        samCode += expSamCode;
        samCode += "ISNIL\n";
        samCode += "JUMPC " + currentWhileLabelEnd + "\n";
        samCode += getStatement(f);
        samCode += "JUMP " + currentWhileLabel + "\n";
        samCode += currentWhileLabelEnd;

        currentWhileLabel = null;
        return samCode;
    }

    public static String getIf(SamTokenizer f) {
        f.check("if");

        if (!f.check('(')) {
            System.out.println("Expecting '(' at line: " + f.lineNo());
            return null;
        }
        String exp = BaliExpressions.getExp(f);
        if (exp == null) {
            return null;
        }

        if (!f.check(')')) {
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
        String exp = BaliExpressions.getExp(f);
        if (exp == null) {
            return null;
        }

        if (!f.check(';')) {
            System.out.println("Expecting ';' at line: " + f.lineNo());
            return null;
        }
        return "";
    }
}
