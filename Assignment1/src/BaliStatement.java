import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.TokenizerException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Bali Statement parser
 */
public class BaliStatement {
    private SamTokenizer tokenizer;
    private static int lastLabelIndexUsed = 0; // used to ensure label uniqueness
    private static List<String> whileLoopLabels = new ArrayList<String>(); // stack of while loop labels
    private BaliMethod.MethodMetaData methodMeta; // meta data of containing method
    private boolean doesReturn; // flag to determine whether a return statement has been found
    private HashSet<String> initializedVars; // set of variables initialized, passed by reference and updated throughout the parsing

    public BaliStatement(SamTokenizer t, BaliMethod.MethodMetaData meta, HashSet<String> initializedVars) {
        this.tokenizer = t;
        this.methodMeta = meta;
        this.doesReturn = false;
        this.initializedVars = initializedVars;
    }

    /**
     * Parse the statement from the input file
     * @return the generated sam code
     */
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
        String expSamCode = new BaliExpression(tokenizer, methodMeta, initializedVars).getExp();
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
            initializedVars.add(writeVariable);
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

            // parse sub-statement and update list of initialized vars
            HashSet<String> nextInitializedVars = new HashSet<String>(initializedVars);
            BaliStatement stmt = new BaliStatement(tokenizer, methodMeta, nextInitializedVars);
            String stmtCode = stmt.getStatement();
            initializedVars.addAll(nextInitializedVars);

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
        String currentWhileLabel = null;
        if (!whileLoopLabels.isEmpty()) {
            currentWhileLabel = whileLoopLabels.get(0);
        }

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
        String samCode = "\tJUMP " + currentWhileLabelEnd + "\n";
        return samCode;
    }

    private String getWhile() {
        // generate label for current while loop
        String currentWhileLabel = "whileLbl" + lastLabelIndexUsed;
        String currentWhileLabelEnd = currentWhileLabel + "End";
        lastLabelIndexUsed++;

        // push to stack
        whileLoopLabels.add(0, currentWhileLabel);


        tokenizer.check("while");

        if (!tokenizer.check('(')) {
            System.out.println("ERROR: Expecting '(' at line: " + tokenizer.lineNo());
            return null;
        }
        String expSamCode = new BaliExpression(tokenizer, methodMeta, initializedVars).getExp();
        if (expSamCode == null) {
            return null;
        }

        if (!tokenizer.check(')')) {
            System.out.println("ERROR: Malformed expression at line: " + tokenizer.lineNo());
            return null;
        }

        HashSet<String> nextInitializedVars = new HashSet<String>(initializedVars);
        BaliStatement stmt = new BaliStatement(tokenizer, methodMeta, nextInitializedVars);
        String stmtSamCode = stmt.getStatement();
        if (stmtSamCode == null) {
            return null;
        }
        // note: list of initialized vars doesn't change

        String samCode = currentWhileLabel + ":\n";
        samCode += expSamCode;
        samCode += "\tISNIL\n";
        samCode += "\tJUMPC " + currentWhileLabelEnd + "\n";
        samCode += stmtSamCode;
        samCode += "\tJUMP " + currentWhileLabel + "\n";
        samCode += currentWhileLabelEnd + ":\n";

        if (!whileLoopLabels.isEmpty()) {
            whileLoopLabels.remove(0);
        } else {
            System.out.println("ERROR: Malformed while loop at line: " + tokenizer.lineNo());
            return null;
        }
        return samCode;
    }

    private String getIf() {
        //Create labels for if
        String ifLbl = "ifLbl" + lastLabelIndexUsed;
        String ifEndLbl = "ifEndLbl" + lastLabelIndexUsed;
        lastLabelIndexUsed++;

        tokenizer.check("if");

        String samCode = "";
        if (!tokenizer.check('(')) {
            System.out.println("ERROR: Expecting '(' at line: " + tokenizer.lineNo());
            return null;
        }
        String expSamCode = new BaliExpression(tokenizer, methodMeta, initializedVars).getExp();
        if (expSamCode == null) {
            return null;
        }

        if (!tokenizer.check(')')) {
            System.out.println("ERROR: Malformed expression at line: " + tokenizer.lineNo());
            return null;
        }
        //Get statement for if block
        HashSet<String> nextInitializedIfVars = new HashSet<String>(initializedVars);
        BaliStatement ifStmt = new BaliStatement(tokenizer, methodMeta, nextInitializedIfVars);
        String ifStmtSamCode = ifStmt.getStatement();
        if (ifStmtSamCode == null) {
            return null;
        }

        if (!tokenizer.check("else")) {
            System.out.println("ERROR: Expecting 'else' at line: " + tokenizer.lineNo());
            return null;
        }

        //Get statement for else block
        HashSet<String> nextInitializedElseVars = new HashSet<String>(initializedVars);
        BaliStatement elseStmt = new BaliStatement(tokenizer, methodMeta, nextInitializedElseVars);
        String elseStmtSamCode = elseStmt.getStatement();
        if (elseStmtSamCode == null) {
            return null;
        }

        //Set return flag
        if (ifStmt.doesReturn() && elseStmt.doesReturn()) {
            this.doesReturn = true;
        }
        //Update initliazed vars to vars intialized by both if and else statement
        nextInitializedIfVars.retainAll(nextInitializedElseVars);
        initializedVars.addAll(nextInitializedIfVars);

        samCode += expSamCode;
        samCode += "\tJUMPC " + ifLbl + "\n";
        //Add else block
        samCode += elseStmtSamCode;

        samCode += "\tJUMP " + ifEndLbl + "\n";
        samCode += ifLbl + ":\n";
        samCode += ifStmtSamCode;
        samCode += ifEndLbl + ":\n";

        return samCode;
    }

    public String getReturn() {
        tokenizer.check("return");
        String expSamCode = new BaliExpression(tokenizer, methodMeta, initializedVars).getExp();
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
