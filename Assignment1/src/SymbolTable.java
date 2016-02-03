import java.util.HashMap;

public class SymbolTable {

    private HashMap<String, Integer> map = new HashMap<String, Integer>();

    public int lookupOffset(String variableName){
        if(map.containsKey(variableName)){
            return map.get(variableName);
        }
        else {
            throw new IllegalArgumentException("Variable not declared");
        }
    }

    public void createNewEntry(String variable, int offset){
        if(!map.containsKey(variable)){
            map.put(variable, offset);
        }
        else{
            throw new IllegalStateException("Variable already defined");
        }
    }
}
