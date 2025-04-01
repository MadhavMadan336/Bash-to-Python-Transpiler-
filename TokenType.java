public enum TokenType {
    IDENTIFIER,
    ASSIGNMENT,
    STRING,
    NUMBER,
    KEYWORD,
    VARIABLE,
    OPERATOR,
    BRACKET,
    SEPARATOR,
    LPAREN,      // Left parenthesis (
    RPAREN,      // Right parenthesis )
    LBRACE,      // Left brace {
    RBRACE,      // Right brace }
    LBRACKET,    // Left bracket [
    RBRACKET,    // Right bracket ]
    ASTERISK,    // Asterisk *
    DOT,         // Dot .
    COMMA,       // Comma ,
    SEMICOLON,   // Semicolon ;
    COLON,       // Colon :
    REDIRECT,
    EOF,
    ECHO,
    NEWLINE,
    COMMAND,
    PAREN,
    CURLY_OPEN,
    CURLY_CLOSE,
    STAR,
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACKET,
    RIGHT_BRACKET,
    DOLLAR,
    RANGE_OPERATOR,
    OTHER;       // For any other special characters not explicitly listed

    private TokenType() {
    }
}