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
        String newLabel = "FunctionLabel" Integer.toString(counter);
        counter++;
        return newLabel;
    }

    //Create new Entry in Map
    public void createNewEntryForFunction(String methodName){
        String methodLabel = generateNewLabel();
        map.put(methodName, methodLabel);
    }

    //Get label for method
    public String lookupLabelForFunction(String methodName){
        return map.get(methodName);
    }
}
