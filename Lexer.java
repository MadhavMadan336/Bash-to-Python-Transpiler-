import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Lexer {
    private String input;
    private int position;
    private List<Token> tokens;
    private int currentTokenIndex = 0;

    private static final List<String> KEYWORDS = Arrays.asList(
            "if", "then", "else", "fi", "for", "while", "do", "done", "echo",
            "case", "esac", "in", "function", "break", "continue"
    );

    private static final List<String> OPERATORS = Arrays.asList(
            "&&", "||", "|", "!", "=", "==", "!=", ">", ">>", ">>>", "<", "<<", "<<<", "&", ";"
    );

    private static final List<String> COMPARISON_OPERATORS = Arrays.asList(
            "-eq", "-ne", "-lt", "-gt", "-le", "-ge"
    );

    public Lexer(String input) {
        this.input = input;
        this.position = 0;
        this.tokens = new ArrayList<>();
    }

    public List<Token> tokenize() {
        System.out.println("🔹 Starting tokenization...");

        while (position < input.length()) {
            skipWhitespace();
            if (position >= input.length()) break;

            char currentChar = input.charAt(position);
            System.out.println("➡️ Processing: '" + currentChar + "' at position " + position);

            if (currentChar == '#') {
                skipComment();
                continue;
            }

            if (peekString(2).equals("..")) {
                tokens.add(new Token(TokenType.RANGE_OPERATOR, ".."));
                position += 2;
                continue;
            }


            if (currentChar == '=') {
                if (peek() == '=') {
                    tokens.add(new Token(TokenType.OPERATOR, "=="));
                    position += 2;
                } else {
                    tokens.add(new Token(TokenType.ASSIGNMENT, "="));
                    position++;
                }
                continue;
            }
            if (currentChar == '{') {
                tokens.add(new Token(TokenType.CURLY_OPEN, "{"));
                position++;
                continue;
            }
            
            if (currentChar == '}') {
                tokens.add(new Token(TokenType.CURLY_CLOSE, "}"));
                position++;
                continue;
            }
            
            if (currentChar == '*') {
                tokens.add(new Token(TokenType.STAR, "*"));
                position++;
                continue;
            }
            
            if (currentChar == ',') {
                tokens.add(new Token(TokenType.COMMA, ","));
                position++;
                continue;
            }
            

            if (currentChar == '[') {
                tokens.add(new Token(TokenType.LEFT_BRACKET, "["));
                position++;
                continue;
            }
            if (currentChar == ']') {
                tokens.add(new Token(TokenType.RIGHT_BRACKET, "]"));
                position++;
                continue;
            }
            

            if (Character.isDigit(currentChar)) {
                tokens.add(parseNumber());
                continue;
            }

            if (isComparisonOperator(peekString(3))) {
                tokens.add(new Token(TokenType.OPERATOR, peekString(3)));
                position += 3;
                continue;
            }

            if (isOperator(peekString(3))) {
                tokens.add(new Token(TokenType.OPERATOR, peekString(3)));
                position += 3;
                continue;
            } else if (isOperator(peekString(2))) {
                tokens.add(new Token(TokenType.OPERATOR, peekString(2)));
                position += 2;
                continue;
            } else if (isOperator(String.valueOf(currentChar))) {
                tokens.add(new Token(TokenType.OPERATOR, String.valueOf(currentChar)));
                position++;
                continue;
            }

            if (currentChar == '$') {
                tokens.add(parseVariable());
                continue;
            }

            if (Character.isLetter(currentChar)) {
                tokens.add(parseIdentifierOrKeyword());
                continue;
            }

            if (currentChar == '"' || currentChar == '\'') {
                tokens.add(parseString());
                continue;
            }

            if (currentChar == ';') {
                tokens.add(new Token(TokenType.SEPARATOR, ";"));
                position++;
                continue;
            }

            if (currentChar == '(') {
                tokens.add(new Token(TokenType.LEFT_PAREN, "("));
                position++;
                continue;
            }
            if (currentChar == ')') {
                tokens.add(new Token(TokenType.RIGHT_PAREN, ")"));
                position++;
                continue;
            }
            

            System.out.println("⚠️ Skipping unknown character: " + currentChar);
            position++;
        }

        tokens.add(new Token(TokenType.EOF, ""));
        System.out.println("✅ Tokenization complete!");
        return tokens;
    }

    public Token nextToken() {
        return currentTokenIndex < tokens.size() ? tokens.get(currentTokenIndex++) : new Token(TokenType.EOF, "");
    }

    public Token peekToken() {
        return currentTokenIndex < tokens.size() ? tokens.get(currentTokenIndex) : new Token(TokenType.EOF, "");
    }

    public TokenType getTokenType(String token) {
        if (isKeyword(token)) return TokenType.KEYWORD;
        if (isOperator(token)) return TokenType.OPERATOR;
        if (isComparisonOperator(token)) return TokenType.OPERATOR;
        if (isVariable(token)) return TokenType.VARIABLE;
        if (isNumber(token)) return TokenType.NUMBER;
        if (isString(token)) return TokenType.STRING;
        return TokenType.IDENTIFIER;
    }

    public Token handleStringLiterals(String input) {
        return parseString();
    }

    public void handleSpecialChars(String input) {
        for (char c : input.toCharArray()) {
            if (c == '$') {
                tokens.add(parseVariable());
            } else if (c == '#') {
                skipComment();
            } else if (c == '&') {
                tokens.add(new Token(TokenType.OPERATOR, "&"));
            }
        }
    }

    public String extractVariableName(String input) {
        return isVariable(input) ? input.substring(1) : "";
    }

    public String extractCommand(String input) {
        return input.split("\\s+")[0];
    }

    private void skipWhitespace() {
        while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
            position++;
        }
    }

    private void skipComment() {
        while (position < input.length() && input.charAt(position) != '\n') {
            position++;
        }
        position++;
    }

    private Token parseNumber() {
        int start = position;
        
        // Read digits
        while (position < input.length() && Character.isDigit(input.charAt(position))) {
            position++;
        }
    
        // Check for `..` range operator
        if (position + 1 < input.length() && input.charAt(position) == '.' && input.charAt(position + 1) == '.') {
            // Create NUMBER token for the first part
            String num = input.substring(start, position);
            tokens.add(new Token(TokenType.NUMBER, num));
    
            // Add RANGE_OPERATOR token
            tokens.add(new Token(TokenType.RANGE_OPERATOR, ".."));
            position += 2;  // Skip both dots
    
            // Parse second number
            return parseNumber();
        }
    
        return new Token(TokenType.NUMBER, input.substring(start, position));
    }
    
    private Token parseVariable() {
        int start = position++;
        while (position < input.length() && (Character.isLetterOrDigit(input.charAt(position)) || input.charAt(position) == '_')) {
            position++;
        }
        return new Token(TokenType.VARIABLE, input.substring(start, position));
    }

    private Token parseIdentifierOrKeyword() {
        int start = position;
        while (position < input.length() &&
                (Character.isLetterOrDigit(input.charAt(position))
                || input.charAt(position) == '_' 
                || input.charAt(position) == '.' 
                || input.charAt(position) == '-')) {
            position++;
        }
        String word = input.substring(start, position);
        return isKeyword(word) ? new Token(TokenType.KEYWORD, word) : new Token(TokenType.IDENTIFIER, word);
    }

    private Token parseString() {
        char quoteType = input.charAt(position);
        int start = ++position;
        while (position < input.length() && input.charAt(position) != quoteType) {
            if (input.charAt(position) == '\\' && position + 1 < input.length()) {
                position += 2;
            } else {
                position++;
            }
        }
        String strValue = input.substring(start, position);
        position++; // Skip closing quote
        return new Token(TokenType.STRING, strValue);
    }

    private char peek() {
        return (position + 1 < input.length()) ? input.charAt(position + 1) : '\0';
    }

    private String peekString(int len) {
        return input.substring(position, Math.min(position + len, input.length()));
    }

    private boolean isKeyword(String word) {
        return KEYWORDS.contains(word);
    }

    private boolean isOperator(String symbol) {
        return OPERATORS.contains(symbol);
    }

    private boolean isComparisonOperator(String symbol) {
        return COMPARISON_OPERATORS.contains(symbol);
    }

    private boolean isVariable(String word) {
        return word.startsWith("$") && word.length() > 1;
    }

    private boolean isNumber(String input) {
        return input.matches("-?\\d+(\\.\\d+)?");
    }

    private boolean isString(String input) {
        return input.startsWith("\"") && input.endsWith("\"");
    }
    
    private boolean isComment(String line) {
        return line.trim().startsWith("#");
    }
}