import java.util.HashMap;

public class SymbolTable {

    private HashMap<String, Integer> map = new HashMap<String, Integer>();
    private HashMap<String, Boolean> initialized = new HashMap<String, Boolean>();

    public int lookupOffset(String variableName){
        if(map.containsKey(variableName)){

            if(initialized.get(variableName) == true) {
                return map.get(variableName);
            }
            else {
                throw new IllegalStateException("Variable not initialized");
            }
        }
        else {
            throw new IllegalArgumentException("Variable not declared");
        }
    }

    public void createNewEntry(String variable, int offset){
        if(!map.containsKey(variable)){
            map.put(variable, offset);
            initialized.put(variable, false);

        }

        else{
            throw new IllegalStateException("Variable already defined");
        }
    }

    public void markVariableInitialized(String variableName) {
        if(!map.containsKey(variableName)){
            throw new IllegalArgumentException("Variable not declared");
        }
        initialized.put(variableName, true);
    }
}
