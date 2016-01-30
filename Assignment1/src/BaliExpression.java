import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;

public class BaliExpression {

    public static String getExp(SamTokenizer f) {
        switch (f.peekAtKind()) {
            case INTEGER: //E -> integer
                return "PUSHIMM " + f.getInt() + "\n";
            case
            case OPERATOR:
            {
            }
            default:   return "ERROR\n";
        }
    }
}
