import com.sun.javaws.exceptions.InvalidArgumentException;

import java.util.HashMap;

/**
 * Created by egantoun on 1/31/16.
 */
public class LabelsMap {

    private HashMap<String, String>  map = new HashMap<String, String>();
    private int counter;

    //Default Constructor
    public LabelsMap() {
        counter = 0;
    }

    //Generate New Label
    private String generateNewLabel() {
        String newLabel = "FunctionLabel" +  Integer.toString(counter);
        counter++;
        return newLabel;
    }

    //Create new Entry in Map
    public void createNewEntryForFunction(String methodName, int numberOfParams){
        String methodLabel = generateNewLabel() + "," + numberOfParams;
        if(!map.containsKey(methodLabel)) {
            map.put(methodName, methodLabel);
        }

        else{
            throw new IllegalStateException("Duplicate method name");
        }
    }

    //Get label for method
    public String lookupLabelForFunction(String methodName){
        //return map.get(methodName);
        if(map.containsKey(methodName)){
            //return map.get(methodName);
            String result = map.get(methodName);
            String[] contents = result.split(",");
            return contents[0];
        }
        else{
            throw new IllegalArgumentException("Method not decalared");
        }
    }

    public int lookupNumberOfParameters(String methodName){
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
