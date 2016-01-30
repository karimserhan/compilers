import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.TokenizerException;

public class BaliExpression {

    public static String getExp(SamTokenizer f)
    {
        if(f.peekAtKind() == Tokenizer.TokenType.INTEGER)
        {
            //Literal Case
        }
        else if(f.check("true"))
        {
            f.pushBack();
            //Literal Case
        }

        else
        {
            //Method or Location
            try
            {
                String variableName = f.getWord();
            }
            catch (TokenizerException e)
            {
                //Error
                //Wrong name
                return null;
            }
            //Check if next token is open paran
            if(f.check("("))
            {
                //case method
            }


        }




        switch (f.peekAtKind()) {
            case INTEGER: //E -> integer
                return "PUSHIMM " + f.getInt() + "\n";

            case OPERATOR:
            {
            }
            default:   return "ERROR\n";
        }
    }
}
