import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.TokenizerException;

public class BaliStatement {
    private SamTokenizer tokenizer;
    private int lastLabelIndexUsed = 0;
    private String currentWhileLabel = null;
    private BaliMethod.MethodMetaData methodMeta;
    private boolean doesReturn;

    public BaliStatement(SamTokenizer t, BaliMethod.MethodMetaData meta) {
        this.tokenizer = t;
        this.methodMeta = meta;
        this.doesReturn = false;
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

    public boolean doesReturn() {
        return doesReturn;
    }

    private String getAssign() {
        String writeVariable;
        try {
            writeVariable = tokenizer.getWord();
        } catch (TokenizerException exp) {
            System.out.println("ERROR: Invalid variable name at line: " + tokenizer.lineNo());
            return null;
        }
        if (!tokenizer.test('=')) {
            System.out.println("ERROR: Expecting '=' at line: " + tokenizer.lineNo());
            return null;
        }

        tokenizer.check('=');
        String expSamCode = new BaliExpression(tokenizer, methodMeta).getExp();
        if (expSamCode == null) { // ma akal
            return null;
        }


        if (!tokenizer.check(';')) {
            System.out.println("ERROR: Expecting ';' at line: " + tokenizer.lineNo());
            return null;
        }

        int offset;
        try {
            //Expression is complete
            //Set variable to initialized
            methodMeta.symbolTable.markVariableInitialized(writeVariable);
            offset = methodMeta.symbolTable.lookupOffset(writeVariable);
        } catch (IllegalArgumentException exp) {
            System.out.println("ERROR: Variable not declared: " + writeVariable + " at line: " + tokenizer.lineNo());
            return null;
        }

        // generate sam code
        String samCode = expSamCode;
        samCode += "\tSTOREOFF " + offset + "\n";
        return samCode;
    }

    private String getBlock() {
        String samCode = "";
        tokenizer.check('{');
        boolean printedWarningMsg = false;

        while (!tokenizer.test('}') && tokenizer.peekAtKind() != Tokenizer.TokenType.EOF) {
            // print warning message
            if (this.doesReturn && !printedWarningMsg) {
                System.out.println("WARNING: unreachable code, line: " + tokenizer.nextLineNo());
                printedWarningMsg = true;
            }

            BaliStatement stmt = new BaliStatement(tokenizer, methodMeta);
            String stmtCode = stmt.getStatement();

            // set return flag of the block statement
            if (stmt.doesReturn()) {
                this.doesReturn = true;
            }

            if (stmtCode == null) {
                return null;
            }
            samCode += stmtCode;
        }

        if (!tokenizer.check('}')) {
            System.out.println("ERROR: Expecting '}' at line: " + tokenizer.lineNo());
            return null;
        }
        return samCode;
    }

    private String getBreak() {
        tokenizer.check("break");

        if (!tokenizer.check(';')) {
            System.out.println("ERROR: Expecting ';' at line: " + tokenizer.lineNo());
            return null;
        }

        if (currentWhileLabel == null) {
            System.out.println("ERROR: Cannot have a break statement outside of a while loop, at line: " + tokenizer.lineNo());
            return null;
        }

        String currentWhileLabelEnd = currentWhileLabel + "End";
        String samCode = "\tJUMP " + currentWhileLabelEnd;
        return samCode;
    }

    private String getWhile() {
        currentWhileLabel = "whileLbl" + lastLabelIndexUsed;
        lastLabelIndexUsed++;
        String currentWhileLabelEnd = currentWhileLabel + "End";

        tokenizer.check("while");

        if (!tokenizer.check('(')) {
            System.out.println("ERROR: Expecting '(' at line: " + tokenizer.lineNo());
            return null;
        }
        String expSamCode = new BaliExpression(tokenizer, methodMeta).getExp();
        if (expSamCode == null) {
            return null;
        }

        if (!tokenizer.check(')')) {
            System.out.println("ERROR: Expecting ')' at line: " + tokenizer.lineNo());
            return null;
        }

        BaliStatement stmt = new BaliStatement(tokenizer, methodMeta);
        String stmtSamCode = stmt.getStatement();
        if (stmtSamCode == null) {
            return null;
        }

        String samCode = currentWhileLabelEnd + ":\n";
        samCode += expSamCode;
        samCode += "\tISNIL\n";
        samCode += "\tJUMPC " + currentWhileLabelEnd + "\n";
        samCode += stmtSamCode;
        samCode += "\tJUMP " + currentWhileLabel + "\n";
        samCode += currentWhileLabelEnd + ":\n";

        currentWhileLabel = null;
        return samCode;
    }

    private String getIf() {
        tokenizer.check("if");

        String samCode = "";
        if (!tokenizer.check('(')) {
            System.out.println("ERROR: Expecting '(' at line: " + tokenizer.lineNo());
            return null;
        }
        String expSamCode = new BaliExpression(tokenizer, methodMeta).getExp();
        if (expSamCode == null) {
            return null;
        }

        if (!tokenizer.check(')')) {
            System.out.println("ERROR: Expecting ')' at line: " + tokenizer.lineNo());
            return null;
        }
        //Get statement for if block
        BaliStatement ifStmt = new BaliStatement(tokenizer, methodMeta);
        String ifStmtSamCode = ifStmt.getStatement();
        if (ifStmtSamCode == null) {
            return null;
        }

        if (!tokenizer.check("else")) {
            System.out.println("ERROR: Expecting 'else' at line: " + tokenizer.lineNo());
            return null;
        }

        //Get statement for else block
        BaliStatement elseStmt = new BaliStatement(tokenizer, methodMeta);
        String elseStmtSamCode = elseStmt.getStatement();
        if (elseStmtSamCode == null) {
            return null;
        }

        //Set return flag
        if (ifStmt.doesReturn() && elseStmt.doesReturn()) {
            this.doesReturn = true;
        }

        //Create label for if
        String ifLbl = "ifLbl" + lastLabelIndexUsed;
        samCode += expSamCode;
        samCode += "\tJUMPC " + ifLbl + "\n";
        //Add else block
        samCode += elseStmtSamCode;

        String ifEndLbl = "ifEndLbl" + lastLabelIndexUsed;
        lastLabelIndexUsed++;
        samCode += "\tJUMP " + ifEndLbl + "\n";
        samCode += ifLbl + ":\n";
        samCode += ifStmtSamCode;
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
            System.out.println("ERROR: Expecting ';' at line: " + tokenizer.lineNo());
            return null;
        }

        // set flag
        doesReturn = true;

        String samCode = expSamCode;
        samCode += "\tSTOREOFF -" + (methodMeta.nbrOfFormals + 1) +"\n";
        samCode += "\tADDSP -" + (methodMeta.nbrOfLocals) + "\n";
        samCode += "\tJUMPIND\n";
        return samCode;
    }
}
