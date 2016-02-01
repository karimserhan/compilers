import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer.TokenType;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class BaliCompiler {
	public static MethodLabelsMap methodLabelsMap = new MethodLabelsMap();

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Invalid parameters. Need to pass <baliFileName> and <samFileName>");
			return;
		}

		String baliFileName = args[0];
		String samCode = compile(baliFileName);

		// write to SaM file
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(args[1], "UTF-8");
			writer.println(samCode == null ? "" : samCode);
			writer.close();
		} catch (FileNotFoundException e) {
			System.out.println("Fatal error: could not write to output file");
		} catch (UnsupportedEncodingException e) {
			System.out.println("Fatal error: could not write to output file");
		}

		if (samCode != null) {
			System.out.println("Compilation succeeded");
		}
	}

	public static String compile(String fileName) {
		//returns SaM code for program in file
		try {
			SamTokenizer f = new SamTokenizer (fileName);
			String pgm = osSetupCode() + "\n";
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

	private static String osSetupCode() {
		String pgm = "start:\n";
		pgm += "\tPUSHIMM 0\n"; //rv slot for main
		pgm += "\tLINK\n"; //save FBR
		pgm += "\tJSR main\n"; //call main
		pgm += "\tPOPFBR\n";
		pgm += "\tSTOP\n";
		return pgm;
	}
}

