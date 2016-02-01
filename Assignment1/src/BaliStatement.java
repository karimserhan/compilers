import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.TokenizerException;

public class BaliStatement {
    private SamTokenizer tokenizer;
    private int lastLabelIndexUsed = 0;
    private String currentWhileLabel = null;
    private BaliMethod.MethodMetaData methodMeta;

    public BaliStatement(SamTokenizer t, BaliMethod.MethodMetaData meta) {
        this.tokenizer = t;
        this.methodMeta = meta;
    }

    public String getStatement() {
        String samCode;
        if (tokenizer.test("return")) {
            samCode = getReturn();
        } else if (tokenizer.test("if")) {
            samCode = getIf();
        } else if (tokenizer.test("while")) {
            samCode = getWhile();
        } else if (tokenizer.test("break")) {
            samCode = getBreak();
        } else if (tokenizer.test('{')) {
            samCode = getBlock();
        }else if (tokenizer.test(';')) {
            samCode = ""; // do nothing
            tokenizer.check(';');
        } else { // hopefully an ASSIGN
            samCode = getAssign();
        }
        return samCode;
    }

    private String getAssign() {
        String writeVariable;
        try {
            writeVariable = tokenizer.getWord();
        } catch (TokenizerException exp) {
            System.out.println("Invalid variable name at line: " + tokenizer.lineNo());
            return null;
        }
        if (!tokenizer.test('=')) {
            System.out.println("Expecting '=' at line: " + tokenizer.lineNo());
            return null;
        }

        tokenizer.check('=');
        String expSamCode = new BaliExpression(tokenizer, methodMeta).getExp();
        if (expSamCode == null) { // ma akal
            return null;
        }


        if (!tokenizer.check(';')) {
            System.out.println("Expecting ';' at line: " + tokenizer.lineNo());
            return null;
        }

        int offset;
        try {
            //Expression is complete
            //Set variable to initialized
            methodMeta.symbolTable.setVariableInitialized(writeVariable);
            offset = methodMeta.symbolTable.lookupOffsetForVariable(writeVariable);
        } catch (IllegalArgumentException exp) {
            System.out.println("Variable not declared: " + writeVariable + " at line: " + tokenizer.lineNo());
            return null;
        }

        // generate sam code
        String samCode = expSamCode;
        samCode += "STOREOFF " + offset + "\n";
        return samCode;
    }

    private String getBlock() {
        String samCode = "";
        tokenizer.check('{');

        while (!tokenizer.test('}')) {
            String stmtCode = getStatement();
            if (stmtCode == null) {
                return null;
            }
            samCode += stmtCode;
        }
        
        return samCode;
    }

    private String getBreak() {
        tokenizer.check("break");

        if (!tokenizer.check(';')) {
            System.out.println("Expecting ';' at line: " + tokenizer.lineNo());
            return null;
        }

        if (currentWhileLabel == null) {
            System.out.println("Cannot have a break statement outside of a while loop, at line: " + tokenizer.lineNo());
            return null;
        }

        String currentWhileLabelEnd = currentWhileLabel + "End";
        String samCode = "JUMP " + currentWhileLabelEnd;
        return samCode;
    }

    private String getWhile() {
        currentWhileLabel = "whileLbl" + lastLabelIndexUsed;
        lastLabelIndexUsed++;
        String currentWhileLabelEnd = currentWhileLabel + "End";

        tokenizer.check("while");

        if (!tokenizer.check('(')) {
            System.out.println("Expecting '(' at line: " + tokenizer.lineNo());
            return null;
        }
        String expSamCode = new BaliExpression(tokenizer, methodMeta).getExp();
        if (expSamCode == null) {
            return null;
        }

        if (!tokenizer.check(')')) {
            System.out.println("Expecting ')' at line: " + tokenizer.lineNo());
            return null;
        }

        String samCode = currentWhileLabelEnd + ":\n";
        samCode += expSamCode;
        samCode += "ISNIL\n";
        samCode += "JUMPC " + currentWhileLabelEnd + "\n";
        samCode += getStatement();
        samCode += "JUMP " + currentWhileLabel + "\n";
        samCode += currentWhileLabelEnd + ":\n";

        currentWhileLabel = null;
        return samCode;
    }

    private String getIf() {
        tokenizer.check("if");

        String samCode = "";
        if (!tokenizer.check('(')) {
            System.out.println("Expecting '(' at line: " + tokenizer.lineNo());
            return null;
        }
        String exp = new BaliExpression(tokenizer, methodMeta).getExp();
        if (exp == null) {
            return null;
        }

        if (!tokenizer.check(')')) {
            System.out.println("Expecting ')' at line: " + tokenizer.lineNo());
            return null;
        }
        //Get statement for if block
        String ifStatement = getStatement();

        if (!tokenizer.check("else")) {
            System.out.println("Expecting 'else' at line: " + tokenizer.lineNo());
            return null;
        }

        //Get statement for else block
        String elseStatement = getStatement();

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

    public String getReturn() {
        tokenizer.check("return");
        String expSamCode = new BaliExpression(tokenizer, methodMeta).getExp();
        if (expSamCode == null) {
            return null;
        }

        if (!tokenizer.check(';')) {
            System.out.println("Expecting ';' at line: " + tokenizer.lineNo());
            return null;
        }

        String samCode = expSamCode;
        samCode += "STOREOFF -" + (methodMeta.nbrOfFormals + 1) +"\n";
        samCode += "ADDSP -" + (methodMeta.nbrOfLocals) + "\n";
        samCode += "JUMPIND\n";
        return samCode;
    }
}
