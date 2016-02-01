import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer.TokenType;

public class BaliCompiler {
	public static MethodLabelsMap methodLabelsMap = new MethodLabelsMap();

	public static void main(String[] args) {
		String baliFileName = "ex.bali";//args[0];
		System.out.println(compile(baliFileName));
	}

	public static String compile(String fileName) {
		//returns SaM code for program in file
		String pgm = "";
		try {
			SamTokenizer f = new SamTokenizer (fileName);
			while(f.peekAtKind() != TokenType.EOF) {
				String methodSamCode = new BaliMethod(f).getMethod();
				if(methodSamCode != null) pgm += methodSamCode + "\n";
				else return null;
			}
			return pgm;
		}
		catch (Exception e) {
			System.out.println("Fatal error: could not compile program");
			return "STOP\n";
		}
	}
}

