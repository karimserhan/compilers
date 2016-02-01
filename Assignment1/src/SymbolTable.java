import com.sun.javaws.exceptions.InvalidArgumentException;

import java.util.HashMap;

/**
 * Created by egantoun on 1/31/16.
 */
public class SymbolTable {

    private HashMap<String, Integer> map = new HashMap<String, Integer>();

    public int lookupOffsetForVariable(String variableName){
        if(map.containsKey(variableName)){
            return map.get(variableName);
        }
        else{
            throw new IllegalArgumentException("Variable not declared");
        }
    }

    public void createNewEntryForVariable(String variable, int offset){
        if(!map.containsKey(variable)){
            map.put(variable, offset);
        }

        else{
            throw new IllegalStateException("Variable already defined");
        }
    }

}
