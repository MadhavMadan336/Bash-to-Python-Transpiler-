import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CodeGenerator {
    public static void generatePythonFile(List<String> pythonCode) {
        try (FileWriter writer = new FileWriter("output.py")) {
            for (String line : pythonCode) {
                writer.write(line + "\n");
            }
            System.out.println("✅ Python file generated: output.py");
        } catch (IOException e) {
            System.out.println("❌ Error writing Python file: " + e.getMessage());
        }
    }

    public static String generatePythonCode(Node ast) {
        switch (ast.type) {
            case "if": return generateIfStatement(ast);
            case "while": return generateWhileLoop(ast);
            case "for": return generateForLoop(ast);
            case "assign": return generateAssignment(ast);
            case "function_def": return generateFunctionDefinition(ast);
            case "function_call": return generateFunctionCall(ast);
            case "echo": return generateEcho(ast);
            case "redirect": return generateRedirectOutput(ast);
            case "pipeline": return generatePipeline(ast);
            case "subshell": return generateSubshell(ast);
            case "logical_op": return generateLogicalOperator(ast);
            case "comparison_op": return generateComparisonOperator(ast);
            case "return": return generateReturnStatement(ast);
            case "exit": return generateExitStatement(ast);
            default: throw new RuntimeException("Unsupported AST Node: " + ast.type);
        }
    }

    public static String generateIfStatement(Node ast) {
        return "if " + generatePythonCode(ast.condition) + ":\n\t      " + generatePythonCode(ast.body);
    }

    public static String generateWhileLoop(Node ast) {
        return "while " + generatePythonCode(ast.condition) + ":\n    " + generatePythonCode(ast.body);
    }

    public static String generateForLoop(Node ast) {
        return "for " + ast.variable + " in " + generatePythonCode(ast.iterable) + ":\n    " + generatePythonCode(ast.body);
    }

    public static String generateAssignment(Node ast) {
        return ast.variable + " = " + (ast.value != null ? generatePythonCode(ast.value) : "None");
    }

    public static String generateFunctionDefinition(Node ast) {
        String params = (ast.parameters != null) ? String.join(", ", ast.parameters) : "";
        return "def " + ast.functionName + "(" + params + "):\n    " + generatePythonCode(ast.body);
    }
    
    public static String generateFunctionCall(Node ast) {
        String args = (ast.arguments != null) ? String.join(", ", ast.arguments) : "";
        return ast.functionName + "(" + args + ")";
    }
    

    public static String generateEcho(Node ast) {
        return "print(" + (ast.value != null ? generatePythonCode(ast.value) : "''") + ")";
    }

    public static String generateRedirectOutput(Node ast) {
        return "with open('" + ast.file + "', 'w') as f:\n    f.write(str(" + 
               (ast.content != null ? generatePythonCode(ast.content) : "''") + "))";
    }
    

    public static String generatePipeline(Node ast) {
        return "import subprocess\n" +
               "p1 = subprocess.Popen(" + (ast.command1 != null ? generatePythonCode(ast.command1) : "''") + 
               ", stdout=subprocess.PIPE, shell=True)\n" +
               "p2 = subprocess.Popen(" + (ast.command2 != null ? generatePythonCode(ast.command2) : "''") + 
               ", stdin=p1.stdout, stdout=subprocess.PIPE, shell=True)\n" +
               "p1.stdout.close()\n" +
               "output, _ = p2.communicate()";
    }
    

    public static String generateSubshell(Node ast) {
        return "import subprocess\n" +
               "output = subprocess.check_output(" + (ast.command != null ? generatePythonCode(ast.command) : "''") + ", shell=True).decode()";
    }

    public static String generateLogicalOperator(Node ast) {
        return generatePythonCode(ast.left) + ("&&".equals(ast.operator) ? " and " : " or ") + generatePythonCode(ast.right);
    }

    public static String generateComparisonOperator(Node ast) {
        return generatePythonCode(ast.left) + " " + ast.operator + " " + generatePythonCode(ast.right);
    }

    public static String generateReturnStatement(Node ast) {
        return "return " + (ast.value != null ? generatePythonCode(ast.value) : "None");
    }

    public static String generateExitStatement(Node ast) {
        return "import sys\nsys.exit(" + generatePythonCode(ast.exitCode) + ")";
    }
}
