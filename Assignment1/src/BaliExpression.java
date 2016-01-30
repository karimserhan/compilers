import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.TokenizerException;

public class BaliExpression {

    public static String getExp(SamTokenizer f)
    {
        if(f.peekAtKind() == Tokenizer.TokenType.INTEGER)
        {
            //Literal Case
            return f.getString();
        }
        if(f.check("true"))
        {
            //Literal Case
            return "1";
        }
        else
        {
            f.pushBack();
        }

        if(f.check("false"))
        {
            //Literal case
            return "0";
        }
        else
        {
            f.pushBack();
        }

        if(f.check("("))
        {
            if(f.peekAtKind() == Tokenizer.TokenType.OPERATOR)
            {
                if(f.check("-"))
                {
                    //Generate SAM code
                    String result = "-" + getExp(f);
                    if(!f.check(")"))
                    {
                        //Error
                        return null;
                    }

                    return result;
                }
                else
                {
                    f.pushBack();
                    if(!f.check("!"))
                    {
                        //Error
                        return null;
                    }
                    String result = "!" + getExp(f);
                    if(!f.check(")"))
                    {
                        //Error
                        return null;
                    }
                    return result;
                }



            }
        }

        else
        {
            f.pushBack();
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
