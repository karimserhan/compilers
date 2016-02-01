import java.util.HashMap;

public class MethodLabelsMap {

    private HashMap<String, String>  map = new HashMap<String, String>();
    private int counter;

    //Default Constructor
    public MethodLabelsMap() {
        counter = 0;
    }

    //Generate New Label
    private String generateNewLabel() {
        String newLabel = "FunctionLabel" +  Integer.toString(counter);
        counter++;
        return newLabel;
    }

    //Create new Entry in Map
    public String createNewEntry(String methodName, int numberOfParams) {
        String methodLabel = generateNewLabel();
        String mehodValue = methodLabel + "," + numberOfParams;
        if(!map.containsKey(methodName)) {
            map.put(methodName, mehodValue);
        } else{
            throw new IllegalStateException("Duplicate method name");
        }
        return methodLabel;
    }

    //Get label for method
    public String lookupLabel(String methodName) {
        //return map.get(methodName);
        if(map.containsKey(methodName)){
            //return map.get(methodName);
            String result = map.get(methodName);
            String[] contents = result.split(",");
            return contents[0];
        }
        else {
            throw new IllegalArgumentException("Method not decalared");
        }
    }

    public int lookupNumberOfParameters(String methodName) {
        if(map.containsKey(methodName)){
            String result = map.get(methodName);
            String[] contents = result.split(",");
            int numberOfParams = Integer.parseInt(contents[1]);
            return numberOfParams;
        }
        else{
            throw new IllegalArgumentException("Number of params not found");
        }
    }
}
