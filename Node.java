import java.util.List;

public class Node {
    String type;
    String functionName;
    String file;
    List<String> parameters;  // Used in function definitions
    List<String> arguments;   // Used in function calls
    Node value;               // Used for assignments and echo
    Node content;             // Used in redirection (output to files)
    Node command1;            // Used for pipelines
    Node command2;            // Used for pipelines
    Node body;                // Used in functions and loops
    Node condition;           // Used in if and while loops
    Node iterable;            // Used in for loops
    String variable;          // Used in variable assignments and loops
    Node command;             // Used in subprocess execution
    Node left;                // Used in logical and comparison operators
    Node right;               // Used in logical and comparison operators
    String operator;          // Used in logical and comparison operators
    Node exitCode;            // Used in exit statements

    public Node(String type) {
        this.type = type;
    }
}
