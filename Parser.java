import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Parser {
    private List<Token> tokens;
    private int currentTokenIndex = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<String> parse() {
        List<String> pythonCode = new ArrayList<>();

        while (currentTokenIndex < tokens.size()) {
            Token token = tokens.get(currentTokenIndex);
            if (token.getType() == TokenType.EOF) {
                break;
            }
            pythonCode.add(parseStatement(0));
        }

        return pythonCode;
    }

    private void advance() {
        if (currentTokenIndex < tokens.size() - 1) {
            currentTokenIndex++;
        }
    }

    private String parseStatement(int indentLevel) {
        if (currentTokenIndex >= tokens.size()) {
            return "";
        }
    
        Token token = tokens.get(currentTokenIndex);
    
        // Ignore closing braces
        if (token.getType() == TokenType.CURLY_CLOSE) {
            advance();
            return "";
        }
        String indentation = "    ".repeat(indentLevel); // Ensure correct indentation
    
        // Handle IDENTIFIERS (commands, assignments, functions)
        if (token.getType() == TokenType.IDENTIFIER) {
            if (isFunctionDefinition()) {
                return parseFunctionDefinition(indentLevel);
            } else if (lookAhead(1) != null && lookAhead(1).getType() == TokenType.LPAREN) {
                return parseFunctionCall(indentLevel);
            } else if (isCommand(token.getValue())) {
                String stmt = parseCommand(indentLevel);
    
                // ✅ Fix: Wrap f-strings in print() if needed
                if (stmt.startsWith("f\"") && stmt.contains("{")) {
                    stmt = "print(" + stmt + ")";
                }
    
                return indentation + stmt;
            } else if (lookAhead(1) != null && lookAhead(1).getType() == TokenType.ASSIGNMENT) {
                return indentation + parseAssignment(indentLevel);
            } else {
                return indentation + parseCommand(indentLevel);
            }
        }
    
        // Handle Redirects (>) and Pipelines (|)
        if (token.getType() == TokenType.OPERATOR) {
            if (token.getValue().equals(">") || token.getValue().equals("|")) {
                List<Token> commandTokens = collectCommandTokens(indentLevel);
    
                if (token.getValue().equals(">")) {
                    return parseRedirectOutput(commandTokens, indentLevel);
                } else if (token.getValue().equals("|")) {
                    return parsePipeline(commandTokens, indentLevel);
                }
            }
        }
    
        // Handle KEYWORDS and VARIABLES
        switch (token.getType()) {
            case KEYWORD:
                return indentation + parseKeyword(indentLevel);
            case VARIABLE:
                return indentation + parseAssignment(indentLevel);
            case LEFT_BRACKET: // Handle nested conditions
                return indentation + parseNestedCondition(indentLevel);
            default:
                throw new RuntimeException("Syntax Error: Unexpected token " + token);
        }
    }

    private String parseNestedCondition(int indentLevel) {
        System.out.println("Parsing nested condition at index: " + currentTokenIndex);
    
        // Match the opening bracket '['
        match(TokenType.LEFT_BRACKET);
    
        // Parse the condition inside the brackets
        String condition = parseCondition();
    
        // Match the closing bracket ']'
        match(TokenType.RIGHT_BRACKET);
    
        // Handle optional semicolon ';' before next statement
        if (check(TokenType.OPERATOR, ";")) {
            match(TokenType.OPERATOR, ";");
        }
    
        return condition;
    }

    private List<Token> collectCommandTokens(int indentLevel) {
        List<Token> collected = new ArrayList<>();

        while (currentTokenIndex < tokens.size()) {
            Token token = tokens.get(currentTokenIndex);

            // Stop collecting on statement end
            if (token.getType() == TokenType.SEMICOLON || token.getType() == TokenType.EOF || token.getType() == TokenType.CURLY_CLOSE) {
                break;
            }

            collected.add(token);
            advance();
        }

        // Optionally consume trailing semicolon if exists
        if (currentTokenIndex < tokens.size() && tokens.get(currentTokenIndex).getType() == TokenType.SEMICOLON) {
            advance();
        }

        return collected;
    }

    private String parseKeyword(int indentLevel) {
        Token token = match(TokenType.KEYWORD);
    
        switch (token.getValue()) {
            case "if":
                return parseIfStatement(indentLevel);
            case "while":
                return parseWhileLoop(indentLevel);
            case "for":
                return parseForLoop(indentLevel);
            case "case":
                return parseCaseStatement(collectCaseTokens(), indentLevel);
            case "echo":
                String echoStatement = parseEcho(indentLevel);
                return convertToFString(echoStatement);
            case "else":
                return "    ".repeat(indentLevel) + "else:";
            case "fi":
            case "then":
            case "do":
            case "done":
                return "";
            case "break":
                return "    ".repeat(indentLevel) + "break";  // Ensure this is inside a loop
            default:
                throw new RuntimeException("Syntax Error: Unexpected keyword " + token.getValue());
        }
    }

    private List<Token> collectCaseTokens() {
        List<Token> caseTokens = new ArrayList<>();

        while (currentTokenIndex < tokens.size()) {
            Token token = tokens.get(currentTokenIndex);
            caseTokens.add(token);

            if (token.getType() == TokenType.KEYWORD && token.getValue().equals("esac")) {
                advance(); // Move past "esac"
                break;
            }

            advance();
        }

        return caseTokens;
    }

    private String convertToFString(String content) {
        if (content.contains("$")) {
            // Convert $name → {name}
            content = content.replaceAll("\\$(\\w+)", "\\{$1}");

            // Ensure f-string for print statements
            if (content.startsWith("print(\"")) {
                return content.replace("print(\"", "print(f\"").replace("\")", "\")");
            } else {
                return "f\"" + content.replace("\"", "") + "\"";
            }
        }
        return content; // Return unchanged if no variables
    }

    private String parseIfStatement(int indentLevel) {
        System.out.println("Parsing if statement at index: " + currentTokenIndex);
    
        // Match the opening bracket '['
        match(TokenType.LEFT_BRACKET);
    
        // Parse the condition inside the brackets
        String condition = parseCondition();
    
        // Match the closing bracket ']'
        match(TokenType.RIGHT_BRACKET);
    
        // Handle optional semicolon ';' before 'then'
        if (check(TokenType.OPERATOR, ";")) {
            match(TokenType.OPERATOR, ";");
        }
    
        // Match the 'then' keyword
        match(TokenType.KEYWORD, "then");
    
        // Build the Python if statement
        StringBuilder block = new StringBuilder();
        block.append("    ".repeat(indentLevel)).append("if ").append(convertCondition(condition)).append(":");
    
        // Parse the 'then' block
        boolean thenHasStatements = false;
        while (!check("fi") && !check("else")) {
            String stmt = parseStatement(indentLevel + 1);
            if (!stmt.isBlank()) {
                block.append("\n").append(stmt);
                thenHasStatements = true;
            }
        }
    
        // Add a 'pass' statement if the 'then' block is empty
        if (!thenHasStatements) {
            block.append("\n").append("    ".repeat(indentLevel + 1)).append("pass");
        }
    
        // Handle the optional 'else' block
        if (check("else")) {
            match(TokenType.KEYWORD, "else");
            block.append("\n").append("    ".repeat(indentLevel)).append("else:");
    
            boolean elseHasStatements = false;
            while (!check("fi")) {
                String stmt = parseStatement(indentLevel + 1);
                if (!stmt.isBlank()) {
                    block.append("\n").append(stmt);
                    elseHasStatements = true;
                }
            }
    
            // Add a 'pass' statement if the 'else' block is empty
            if (!elseHasStatements) {
                block.append("\n").append("    ".repeat(indentLevel + 1)).append("pass");
            }
        }
    
        // Match the 'fi' keyword
        match(TokenType.KEYWORD, "fi");
    
        return block.toString();
    }

    private String parseWhileLoop(int indentLevel) {
    // Parse condition
    match(TokenType.LEFT_BRACKET);
    String condition = parseCondition();
    match(TokenType.RIGHT_BRACKET);
    
    if (check(TokenType.OPERATOR, ";")) {
        match(TokenType.OPERATOR, ";");
    }
    
    match(TokenType.KEYWORD, "do");
    
    StringBuilder block = new StringBuilder();
    String indentation = "    ".repeat(indentLevel);
    block.append(indentation).append("while ").append(convertCondition(condition)).append(":");
    
    // Extract likely loop variable from condition
    String loopVar = extractLoopVariable(condition);
    boolean hasIncrement = false;
    boolean hasStatements = false;
    
    // Parse loop body
    while (!check("done")) {
        String stmt = parseStatement(indentLevel + 1);
        if (!stmt.isBlank()) {
            block.append("\n").append(stmt);
            hasStatements = true;
            
            // Check for increment of our loop variable
            if (loopVar != null && stmt.matches(".*\\b" + loopVar + "\\s*[+\\-*/]?=.+")) {
                hasIncrement = true;
            }
        }
    }
    
    // Add automatic increment if needed
    if (!hasIncrement && loopVar != null) {
        block.append("\n")
             .append("    ".repeat(indentLevel + 1))
             .append(loopVar)
             .append(" += 1");
    }
    
    // Handle empty loops
    if (!hasStatements) {
        block.append("\n")
             .append("    ".repeat(indentLevel + 1))
             .append("pass");
    }
    
    match(TokenType.KEYWORD, "done");
    return block.toString();
}

private String extractLoopVariable(String condition) {
    // Patterns to detect loop variables in conditions
    // Examples: "$i -le 10" or "count <= 5"
    String[] patterns = {
        "\\$(\\w+)\\s+[-][a-z]+\\s+",  // Bash style ($i -le 10)
        "(\\w+)\\s*([<>]=?|==|!=)\\s*" // Python style (i <= 10)
    };
    
    for (String pattern : patterns) {
        java.util.regex.Matcher m = Pattern.compile(pattern).matcher(condition);
        if (m.find()) {
            return m.group(1); // Return the variable name
        }
    }
    return null;
}
    
      

    private String parseForLoop(int indentLevel) {
        Token var = match(TokenType.IDENTIFIER);
    
        if (!check(TokenType.KEYWORD, "in")) {
            throw new RuntimeException("Syntax Error: Expected 'in' after for-loop variable.");
        }
        match(TokenType.KEYWORD, "in");
    
        List<String> items = new ArrayList<>();
    
        // Handle range expressions like {1..5}
        if (check(TokenType.CURLY_OPEN)) {
            match(TokenType.CURLY_OPEN); // Consume '{'
    
            // Expect a NUMBER token
            Token startToken = match(TokenType.NUMBER);
            String startValue = startToken.getValue();
    
            // Expect '..' as a separate token
            if (!check(TokenType.RANGE_OPERATOR)) {
                throw new RuntimeException("Syntax Error: Expected '..' inside range expression.");
            }
            match(TokenType.RANGE_OPERATOR);
            
    
            // Expect a NUMBER token
            Token endToken = match(TokenType.NUMBER);
            String endValue = endToken.getValue();
    
            // Ensure closing '}'
            if (!check(TokenType.CURLY_CLOSE)) {
                throw new RuntimeException("Syntax Error: Expected '}' at end of range expression.");
            }
            match(TokenType.CURLY_CLOSE);
    
            // Convert `{1..5}` to Python's `range(1, 6)`
            try {
                int startInt = Integer.parseInt(startValue);
                int endInt = Integer.parseInt(endValue);
                items.add("range(" + startInt + ", " + (endInt + 1) + ")"); // Python range is exclusive
            } catch (NumberFormatException e) {
                throw new RuntimeException("Syntax Error: Range bounds must be numbers.");
            }
        } else {
            // Handle normal lists (not range)
            while (currentTokenIndex < tokens.size()) {
                Token token = tokens.get(currentTokenIndex);
    
                if (token.getType() == TokenType.NUMBER || token.getType() == TokenType.IDENTIFIER || token.getType() == TokenType.STRING) {
                    String value = token.getValue();
    
                    // Ensure correct formatting for strings
                    if (token.getType() == TokenType.STRING) {
                        items.add(value);  // Strings are already quoted in tokenization
                    } else if (!isNumber(value)) {
                        items.add("\"" + value + "\""); // Wrap non-numeric values in quotes
                    } else {
                        items.add(value);
                    }
                    advance();
                } else {
                    break;
                }
            }
        }
    
        // Handle optional ';' before 'do'
        if (check(TokenType.OPERATOR, ";")) {
            match(TokenType.OPERATOR, ";");
        }
    
        // Ensure 'do' exists
        if (!check(TokenType.KEYWORD, "do")) {
            throw new RuntimeException("Syntax Error: Expected 'do' in for-loop, but found: " + tokens.get(currentTokenIndex));
        }
        match(TokenType.KEYWORD, "do");
    
        // Generate Python for-loop syntax
        StringBuilder block = new StringBuilder();
        block.append("    ".repeat(indentLevel))
             .append("for ")
             .append(var.getValue())
             .append(" in ")
             .append(String.join(", ", items))
             .append(":");
    
        // Ensure 'done' exists before parsing statements
        while (currentTokenIndex < tokens.size() && !check(TokenType.KEYWORD, "done")) {
            String stmt = parseStatement(indentLevel + 1);
    
            // Handle print formatting properly
            if (stmt.startsWith("f\"") && stmt.contains("{")) {
                stmt = "print(" + stmt + ")";
            } else if (stmt.startsWith("print(")) {
                String content = stmt.substring(6, stmt.length() - 1).trim();
                if (content.contains("{")) {
                    stmt = "print(f\"" + content + "\")";
                } else {
                    stmt = "print(" + content + ")";
                }
            }
    
            block.append("\n").append("    ".repeat(indentLevel + 1)).append(stmt.trim());

        }
    
        // Ensure 'done' is matched
        if (!check(TokenType.KEYWORD, "done")) {
            throw new RuntimeException("Syntax Error: Expected 'done' to close for-loop.");
        }
        match(TokenType.KEYWORD, "done");
    
        return block.toString();
    }
    
    
    

    private String parseFunctionDefinition(int indentLevel) {
        // Match function name
        Token funcNameToken = match(TokenType.IDENTIFIER);
        String funcName = funcNameToken.getValue();

        // Match parentheses ()
        match(TokenType.LEFT_PAREN);
        match(TokenType.RIGHT_PAREN);

        // Match opening brace {
        match(TokenType.CURLY_OPEN);

        // Capture output inside the block
        StringBuilder sb = new StringBuilder();
        sb.append("    ".repeat(indentLevel)).append("def ").append(funcName).append("():\n");

        // Parse the block body directly here
        parseBlock(indentLevel + 1);

        return sb.toString().stripTrailing();
    }

    private String parseFunctionCall(int indentLevel) {
        Token funcName = match(TokenType.IDENTIFIER);
        match(TokenType.LEFT_PAREN);
        if (!check(TokenType.RIGHT_PAREN)) {
            // Handle arguments if they exist
            List<String> arguments = new ArrayList<>();
            while (!check(TokenType.RIGHT_PAREN)) {
                Token arg = matchAny(TokenType.IDENTIFIER, TokenType.VARIABLE, TokenType.STRING, TokenType.NUMBER);
                arguments.add(arg.getValue());
                if (check(TokenType.COMMA, ",")) {
                    match(TokenType.COMMA, ",");
                }
            }
            match(TokenType.RIGHT_PAREN);
            return "    ".repeat(indentLevel) + funcName.getValue() + "(" + String.join(", ", arguments) + ")";
        }
        match(TokenType.RIGHT_PAREN);

        return "    ".repeat(indentLevel) + funcName.getValue() + "()";
    }

    private String parseCommand(int indentLevel) {
        Token command = match(TokenType.IDENTIFIER);
        List<String> args = new ArrayList<>();

        while (currentTokenIndex < tokens.size()) {
            Token next = tokens.get(currentTokenIndex);

            if (next.getType() == TokenType.KEYWORD || next.getType() == TokenType.EOF || next.getType() == TokenType.SEMICOLON) {
                break;
            }

            String argument = match(next.getType()).getValue();

            // Convert only arguments that contain variables
            if (argument.contains("$")) {
                argument = convertToFString(argument);
            } else {
                argument = "\"" + argument + "\"";  // Ensure normal arguments remain quoted
            }

            args.add(argument);
        }

        // Handle function calls separately
        if (command.getValue().equals("greet") && args.isEmpty()) {
            return "    ".repeat(indentLevel) + "greet()";
        }

        // Standard subprocess call
        String cmdStr = command.getValue() + " " + String.join(", ", args);
        return "    ".repeat(indentLevel) + "subprocess.run(" + cmdStr.trim() + ", shell=True)";
    }

    private String parseEcho(int indentLevel) {
        List<String> arguments = new ArrayList<>();
        boolean containsVariable = false;
    
        while (currentTokenIndex < tokens.size()) {
            Token next = tokens.get(currentTokenIndex);
    
            // Stop parsing if a new statement starts
            if (next.getType() == TokenType.KEYWORD || 
                next.getType() == TokenType.CURLY_CLOSE || 
                next.getType() == TokenType.EOF || 
                (next.getType() == TokenType.OPERATOR && next.getValue().equals(";"))) {
                break;
            }
    
            // Acceptable argument types
            if (next.getType() == TokenType.STRING || next.getType() == TokenType.IDENTIFIER ||
                next.getType() == TokenType.VARIABLE || next.getType() == TokenType.NUMBER) {
                
                String value = match(next.getType()).getValue();
    
                // If it's a variable ($i), convert it properly
                if (next.getType() == TokenType.VARIABLE) {
                    containsVariable = true;
                    value = "{" + value.replace("$", "") + "}";  // Convert $i -> {i}
                }
                
                arguments.add(value);
            } else {
                // Move to the next token if it's not an acceptable argument type
                currentTokenIndex++;
            }
        }
    
        // ✅ Ensure correct f-string formatting
        String output = String.join(" ", arguments);
        if (containsVariable) {
            output = "f\"" + output + "\"";  // Only wrap in f-string if variables exist
        } else {
            output = "\"" + output + "\"";   // Normal string without f-prefix
        }
    
        return "print(" + output + ")";
    }

    

    private String parseAssignment(int indentLevel) {
        Token variable = matchAny(TokenType.IDENTIFIER, TokenType.VARIABLE);
        match(TokenType.ASSIGNMENT, "=");

        StringBuilder expr = new StringBuilder();
        while (currentTokenIndex < tokens.size()) {
            Token token = tokens.get(currentTokenIndex);

            if (token.getType() == TokenType.EOF || token.getType() == TokenType.KEYWORD || 
                token.getType() == TokenType.BRACKET || token.getType() == TokenType.SEMICOLON) {
                break;
            }

            // Handle arithmetic expressions like $((x + 1))
            if (token.getType() == TokenType.DOLLAR && lookAhead(1) != null && 
            lookAhead(1).getType() == TokenType.LEFT_PAREN) {
                expr.append(parseArithmeticExpression());
                advance(); // ✅ Ensure we move forward after parsing
                continue;
            }

            // Handle variable interpolation like $x
            if (token.getType() == TokenType.VARIABLE) {
                expr.append(token.getValue().replace("$", ""));
                advance();
                expr.append(" ");
                continue;
            }

            // Handle normal values (strings, numbers, etc.)
            String value = match(token.getType()).getValue();

            // ✅ FIXED: Wrap non-numeric values in quotes
            if (!isNumber(value) && !value.startsWith("\"") && !value.startsWith("'")) {
                value = "\"" + value + "\"";
            }

            expr.append(value).append(" ");
        }

        return "    ".repeat(indentLevel) + variable.getValue().replace("$", "") + " = " + expr.toString().trim();
    }

    // ✅ Add this function to fix the undefined error
    private boolean isNumber(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);  // Try parsing as a number
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Token currentToken() {
        return tokens.get(currentTokenIndex);
    }

    private String parseArithmeticExpression() {
        StringBuilder arithExpr = new StringBuilder();
        
        // Skip the '$' and first '('
        match(TokenType.DOLLAR, "$");
        match(TokenType.LEFT_PAREN, "(");
        match(TokenType.LEFT_PAREN, "(");

        int parenCount = 2; // because we entered $((...))

        while (parenCount > 0 && currentTokenIndex < tokens.size()) {
            Token t = currentToken();

            if (t.getType() == TokenType.LEFT_PAREN) {
                parenCount++;
                arithExpr.append("(");
            } else if (t.getType() == TokenType.RIGHT_PAREN) {
                parenCount--;
                if (parenCount == 0) {
                    match(TokenType.RIGHT_PAREN, ")");
                    advance(); // exit loop after last ')'
                    break;
                }
                arithExpr.append(")");
            } else if (t.getType() == TokenType.VARIABLE) {
                arithExpr.append(t.getValue().replace("$", ""));
            } else {
                arithExpr.append(t.getValue());
            }

            arithExpr.append(" ");
            advance();
        }
        return arithExpr.toString().trim();
    }

    private String parseCondition() {
        StringBuilder condition = new StringBuilder();
        System.out.println("Parsing condition at index: " + currentTokenIndex);
    
        while (!check(TokenType.RIGHT_BRACKET)) {
            Token token = tokens.get(currentTokenIndex);
            System.out.println("Condition token: " + token);
    
            if (token.getType() == TokenType.VARIABLE) {
                // Remove the '$' from variable names
                condition.append(token.getValue().replace("$", ""));
                advance();
            } else if (token.getType() == TokenType.IDENTIFIER || token.getType() == TokenType.NUMBER) {
                condition.append(match(token.getType()).getValue());
            } else if (token.getType() == TokenType.STRING) {
                String value = match(TokenType.STRING).getValue();
                if (!value.startsWith("\"") && !value.startsWith("'")) {
                    value = "\"" + value + "\""; // Ensure strings are quoted
                }
                condition.append(value);
            } else if (token.getType() == TokenType.OPERATOR) {
                // Convert Bash operators to Python operators
                String bashOperator = match(TokenType.OPERATOR).getValue();
                String pythonOperator = convertOperator(bashOperator);
                condition.append(" ").append(pythonOperator).append(" ");
            } else {
                throw new RuntimeException("Syntax Error: Unexpected token " + token);
            }
        }
    
        return condition.toString().trim();
    }
    // ✅ Converts Bash comparison operators to Python syntax
    private String convertOperator(String bashOp) {
        return switch (bashOp) {
            case "-eq" -> "==";
            case "-ne" -> "!=";
            case "-lt" -> "<";
            case "-le" -> "<=";
            case "-gt" -> ">";
            case "-ge" -> ">=";
            default -> bashOp; // Return as-is for unknown operators
        };
    }
    private String convertCondition(String condition) {
        // Convert Bash style condition to Python style condition
        return condition.replace("-gt", ">")
                        .replace("-lt", "<")
                        .replace("-eq", "==")
                        .replace("-ne", "!=")
                        .replace("-le", "<=")
                        .replace("-ge", ">=");
    }

    private String parseRedirectOutput(List<Token> tokens, int indentLevel) {
        StringBuilder command = new StringBuilder();
        String outputFile = null;

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType() == TokenType.OPERATOR && token.getValue().equals(">")) {
                if (i + 1 < tokens.size()) {
                    outputFile = tokens.get(i + 1).getValue();
                }
                break;
            } else {
                command.append(token.getValue()).append(" ");
            }
        }

        if (outputFile == null || outputFile.isEmpty()) {
            throw new RuntimeException("Syntax Error: Missing filename after redirection operator '>'");
        }

        return "    ".repeat(indentLevel) + "with open('" + outputFile + "', 'w') as f:\n" +
               "        f.write(subprocess.check_output('" + command.toString().trim() + "', shell=True).decode('utf-8'))";
    }    

    private void parseBlock(int indentLevel) {
        while (!check(TokenType.CURLY_CLOSE) && !check(TokenType.EOF)) {
            if (check(TokenType.KEYWORD) && tokens.get(currentTokenIndex).getValue().equals("echo")) {
                match(TokenType.KEYWORD, "echo"); // properly consume 'echo'
                System.out.println(parseEcho(indentLevel));
            } else if (check(TokenType.KEYWORD) && tokens.get(currentTokenIndex).getValue().equals("return")) {
                match(TokenType.KEYWORD, "return"); // properly consume 'return'
                System.out.println(parseReturn(indentLevel));
            } else {
                parseStatement(indentLevel);
            }
            
        }
        match(TokenType.CURLY_CLOSE); // consume the closing }
    }
    private String parseReturn(int indentLevel) {
        String returnValue = "";
        if (check(TokenType.NUMBER) || check(TokenType.IDENTIFIER) || check(TokenType.VARIABLE)) {
            returnValue = match(tokens.get(currentTokenIndex).getType()).getValue();
            if (returnValue.startsWith("$")) {
                returnValue = returnValue.substring(1); // remove $
            }
        }
        return "    ".repeat(indentLevel) + "return " + returnValue;
    }

    private String parsePipeline(List<Token> tokens, int indentLevel) {
        return "    ".repeat(indentLevel) + "subprocess.run(['" + 
        tokens.stream().map(Token::getValue).collect(Collectors.joining("', '")) + "'], shell=True)";
    }

    private String parseSubshell(List<Token> tokens, int indentLevel) {
        return "    ".repeat(indentLevel) + "subprocess.check_output([" + tokens.get(1).getValue() + "])";
    }

    private String parseLogicalOperator(List<Token> tokens, int indentLevel) {
        String logicalOperator = tokens.get(1).getValue().equals("&&") ? "and" : "or";
        return "    ".repeat(indentLevel) + tokens.get(0).getValue() + " " + logicalOperator + " " + tokens.get(2).getValue();
    }

    private String parseComparisonOperator(List<Token> tokens, int indentLevel) {
        String comparisonOperator = convertOperator(tokens.get(1).getValue());
        return "    ".repeat(indentLevel) + tokens.get(0).getValue() + " " + comparisonOperator + " " + tokens.get(2).getValue();
    }

    private String parseBlock(List<Token> tokens, int indentLevel) {
        // Implement handling for blocks of code inside loops or functions
        StringBuilder block = new StringBuilder();
        while (!check("done") && !check("fi") && !check("}")) {
            block.append(parseStatement(indentLevel + 1)).append("\n");
        }
        return block.toString();
    }

    private String handleBashFunctions(List<Token> tokens, int indentLevel) {
        if (tokens.get(1).getValue().equals("(")) {
            // Function Definition
            String functionName = tokens.get(0).getValue();
            return "def " + functionName + "():\n";
        }
        // Function Call
        return "    ".repeat(indentLevel) + tokens.get(0).getValue() + "()";
    }

    private String parseExitCommand(List<Token> tokens, int indentLevel) {
        return "    ".repeat(indentLevel) + "sys.exit(" + tokens.get(1).getValue() + ")";
    }

    private String parseReturnCommand(List<Token> tokens, int indentLevel) {
        return "    ".repeat(indentLevel) + "return " + tokens.get(1).getValue();
    }

    private String parseCaseStatement(List<Token> tokens, int indentLevel) {
        StringBuilder caseStatement = new StringBuilder();

        // ✅ Ensure "case" is followed by a VARIABLE or IDENTIFIER
        if (tokens.size() < 3 || 
           (tokens.get(1).getType() != TokenType.VARIABLE && tokens.get(1).getType() != TokenType.IDENTIFIER)) {
            throw new RuntimeException("Syntax Error: Expected variable after 'case' but found '" 
                                       + (tokens.size() > 1 ? tokens.get(1).getValue() : "EOF") + "'");
        }

        String variable = tokens.get(1).getValue();

        // ✅ Find "in" dynamically
        int inIndex = -1;
        for (int j = 2; j < tokens.size(); j++) {
            if (tokens.get(j).getValue().equals("in")) {
                inIndex = j;
                break;
            }
        }
        if (inIndex == -1) {
            throw new RuntimeException("Syntax Error: Expected 'in' after variable '" + variable + "'");
        }

        caseStatement.append("    ".repeat(indentLevel)).append("match ").append(variable).append(":\n");

        int i = inIndex + 1; // ✅ Start parsing after "in"

        while (i < tokens.size()) {
            Token token = tokens.get(i);

            // ✅ Stop at "esac"
            if (token.getType() == TokenType.KEYWORD && token.getValue().equals("esac")) {
                break;
            }

            // ✅ Accept both STRING and NUMBER as case labels
            if (token.getType() == TokenType.STRING || token.getType() == TokenType.NUMBER) {
                caseStatement.append("    ".repeat(indentLevel + 1)).append("case ").append(token.getValue()).append(":\n");

                // Find where the block starts
                int blockStart = i + 2;
                int blockEnd = blockStart;
                while (blockEnd < tokens.size() && tokens.get(blockEnd).getType() != TokenType.OPERATOR) {
                    blockEnd++;
                }

                // Parse block inside each case
                caseStatement.append("    ".repeat(indentLevel + 2))
                             .append(parseBlock(tokens.subList(blockStart, blockEnd), indentLevel + 2))
                             .append("\n");

                i = blockEnd + 1;  // Move to the next case pattern
            } else {
                i++;  // Move to next token
            }
        }

        return caseStatement.toString();
    }

    private String parseBreakStatement(List<Token> tokens, int indentLevel) {
        return "    ".repeat(indentLevel) + "break";
    }

    private Token match(TokenType expected) {
        if (currentTokenIndex >= tokens.size()) {
            throw new RuntimeException("Syntax Error: Unexpected end of input. Expected " + expected);
        }
    
        Token token = tokens.get(currentTokenIndex);
        System.out.println("DEBUG: Matching expected '" + expected + "', found: '" + token.getValue() + "'");
    
        if (token.getType() == expected) {
            currentTokenIndex++;
            return token;
        }
    
        throw new RuntimeException("Syntax Error: Expected " + expected + " but found '" + token.getValue() + "' at index " + currentTokenIndex);
    }
    
    private Token match(TokenType expected, String expectedValue) {
        if (currentTokenIndex >= tokens.size()) {
            throw new RuntimeException("Syntax Error: Unexpected end of input. Expected '" + expectedValue + "' of type " + expected);
        }
    
        Token token = tokens.get(currentTokenIndex);
        if (token.getType() == expected && token.getValue().equals(expectedValue)) {
            currentTokenIndex++;
            return token;
        }
    
        throw new RuntimeException(
            "Syntax Error: Expected '" + expectedValue + "' of type " + expected + 
            " but found '" + token.getValue() + "' of type " + token.getType() + " at index " + currentTokenIndex
        );
    }

    private Token matchAny(TokenType... types) {
        Token token = tokens.get(currentTokenIndex);
        for (TokenType type : types) {
            if (token.getType() == type) {
                currentTokenIndex++;
                return token;
            }
        }
        throw new RuntimeException("Syntax Error: Unexpected token " + token);
    }

    private Token lookAhead(int offset) {
        if (currentTokenIndex + offset < tokens.size()) {
            return tokens.get(currentTokenIndex + offset);
        }
        return null;
    }

    private boolean check(String keyword) {
        if (currentTokenIndex >= tokens.size()) return false;
        Token t = tokens.get(currentTokenIndex);
        return t.getType() == TokenType.KEYWORD && t.getValue().equals(keyword);
    }

    private boolean check(TokenType type, String value) {
        if (currentTokenIndex >= tokens.size()) return false;
        Token t = tokens.get(currentTokenIndex);
        return t.getType() == type && t.getValue().equals(value);
    }

    private boolean check(TokenType type) {
        if (currentTokenIndex >= tokens.size()) return false;
        Token t = tokens.get(currentTokenIndex);
        return t.getType() == type;
    }

    private boolean isCommand(String value) {
        return value.equals("cat") || value.equals("ls") || value.equals("touch") || value.equals("rm") || value.equals("mkdir");
    }

    private boolean isFunctionDefinition() {
        Token t1 = lookAhead(1);
        Token t2 = lookAhead(2);
        Token t3 = lookAhead(3);
        
        return t1 != null && t1.getType() == TokenType.LEFT_PAREN &&
               t2 != null && t2.getType() == TokenType.RIGHT_PAREN &&
               t3 != null && t3.getType() == TokenType.LEFT_BRACKET;
    }
}