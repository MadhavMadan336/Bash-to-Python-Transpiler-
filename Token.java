public class Token {
    private TokenType type;
    private String value;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public TokenType getType() {  // ✅ Fix: Add this method
        return type;
    }

    public String getValue() {  // ✅ Fix: Add this method
        return value;
    }

    @Override
    public String toString() {
        return "Token(" + type + ", " + value + ")";
    }
}
