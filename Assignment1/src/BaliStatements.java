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
        String writeVariable;
        try {
            writeVariable = f.getWord();
        } catch (TokenizerException exp) {
            System.out.println("Invalid variable name at line: " + f.lineNo());
            return null;
        }
        if (!f.test('=')) {
            System.out.println("Expecting '=' at line: " + f.lineNo());
            return null;
        }
        f.check('=');
        String expSamCode = BaliExpressions.getExp(f);
        if (expSamCode == null) { // ma akal
            return null;
        }

        if (!f.check(';')) {
            System.out.println("Expecting ';' at line: " + f.lineNo());
            return null;
        }

        int offset;
        try {
            offset = BaliCompiler.currentSymbolTable.lookupOffsetForVariable(writeVariable);
        } catch (IllegalArgumentException exp) {
            System.out.println("Variable not declared: " + writeVariable + " at line: " + f.lineNo());
            return null;
        }

        // generate sam code
        String samCode = expSamCode;
        samCode += "STOREOFF " + offset + "\n";
        return samCode;
    }

    public static String getBlock(SamTokenizer f) {
        String samCode = "";
        f.check('{');

        while (!f.test('}')) {
            String stmtCode = getStatement(f);
            if (stmtCode == null) {
                return null;
            }
            samCode += stmtCode;
        }
        
        return samCode;
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
        samCode += currentWhileLabelEnd + ":\n";

        currentWhileLabel = null;
        return samCode;
    }

    public static String getIf(SamTokenizer f) {
        f.check("if");

        String samCode = "";
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
        //Get statement for if block
        String ifStatement = getStatement(f);

        if (!f.check("else")) {
            System.out.println("Expecting 'else' at line: " + f.lineNo());
            return null;
        }

        //Get statement for else block
        String elseStatement = getStatement(f);

        //Create label for if
        String ifLbl = "ifLbl" + lastLabelIndexUsed;
        samCode += exp;
        samCode += "JUMPC " + ifLbl + "\n";
        //Add else block
        samCode += elseStatement;

        String ifEndLbl = "ifEndLbl" + lastLabelIndexUsed;
        lastLabelIndexUsed++;
        samCode += "JUMP " + ifEndLbl;
        samCode += ifLbl + ":\n";
        samCode += ifStatement;
        samCode += ifEndLbl + ":\n";

        return samCode;
    }

    public static String getReturn(SamTokenizer f) {
        f.check("return");
        String expSamCode = BaliExpressions.getExp(f);
        if (expSamCode == null) {
            return null;
        }

        if (!f.check(';')) {
            System.out.println("Expecting ';' at line: " + f.lineNo());
            return null;
        }

        String samCode = expSamCode;
        samCode += "STOREOFF -" + (BaliCompiler.currentNbrOfFormals + 1) +"\n";
        samCode += "ADDSP -" + (BaliCompiler.currentNbrOfLocals) + "\n";
        samCode += "JUMPIND\n";
        return samCode;
    }
}
