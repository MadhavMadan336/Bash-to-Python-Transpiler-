import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("📂 Reading input file...");
            String input = new String(Files.readAllBytes(Paths.get("bash.sh")), StandardCharsets.UTF_8);
            System.out.println("✅ File read successfully!\n");

            // ✅ Tokenization Step
            System.out.println("🔍 Tokenizing input...");
            Lexer lexer = new Lexer(input);
            List<Token> tokens = lexer.tokenize();
            System.out.println("✅ Tokenization complete!");

            // ✅ Print tokens for debugging
            System.out.println("\n🔹 **TOKENS FOUND:**");
            for (Token token : tokens) {
                System.out.println(token);
            }

            // ✅ Parsing Step
            System.out.println("\n🔍 Parsing tokens...");
            Parser parser = new Parser(tokens);
            List<String> pythonCode = parser.parse();
            System.out.println("✅ Parsing complete!");

            // ✅ Writing Output
            System.out.println("\n💾 Writing to output.py...");
            Files.write(Paths.get("output.py"), pythonCode, StandardCharsets.UTF_8);
            System.out.println("\n✅ Conversion successful! Check output.py");

        } catch (IOException e) {
            System.err.println("❌ Error reading or writing files: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}