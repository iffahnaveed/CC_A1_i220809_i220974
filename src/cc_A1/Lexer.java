package cc_A1;

import java.io.*;
import java.util.*;
import java.util.regex.*;
class State {
    int id;
    Map<Character, List<State>> transitions = new HashMap<>();
    boolean isFinal;
    
    public State(int id) {
        this.id = id;
    }
    
    public void addTransition(char symbol, State state) {
        transitions.computeIfAbsent(symbol, k -> new ArrayList<>()).add(state);
    }
}

class NFA {
    State start;
    Set<State> states = new HashSet<>();
    Set<State> finalStates = new HashSet<>();
    
    public NFA(State start) {
        this.start = start;
        states.add(start);
    }
}

class DFA {
    State start;
    Set<State> states = new HashSet<>();
    
    public DFA(State start) {
        this.start = start;
        states.add(start);
    }
}

class SymbolTableEntry {
    String name, type, scope;
    int memoryAddress;

    public SymbolTableEntry(String name, String type, String scope, int memoryAddress) {
        this.name = name;
        this.type = type;
        this.scope = scope;
        this.memoryAddress = memoryAddress;
    }
}

class SymbolTable {
    private Map<String, SymbolTableEntry> table = new LinkedHashMap<>();
    private int addressCounter = 1000;  // Simulating memory locations

    public void insert(String name, String type, String scope) {
        if (!table.containsKey(name)) {
            table.put(name, new SymbolTableEntry(name, type, scope, addressCounter++));
        }
    }

    public void printTable() {
    	System.out.println();
    	System.out.println();
    	System.out.println("\033[1mSYMBOL TABLE:\033[0m");
        System.out.println("+----------------------+----------------------+------------+----------------+");
        System.out.printf("| %-20s | %-20s | %-10s | %-14s |\n", "Symbol", "Type", "Scope", "Mem. Address");
        System.out.println("+----------------------+----------------------+------------+----------------+");

        for (SymbolTableEntry entry : table.values()) {
            System.out.printf("| %-20s | %-20s | %-10s | 0x%-12X |\n",
                    entry.name, entry.type, entry.scope, entry.memoryAddress);
        }

        System.out.println("+----------------------+----------------------+------------+----------------+");
    }

}

public class Lexer {
    private static final SymbolTable symbolTable = new SymbolTable();

    private static final String RESTRICTED = "\\b(int|return|void|show|global|local|onoff|tinytxt)\\b";
    private static final String AGAINPLS = "\\b(aslongas|traverse)\\b";
    private static final String DECIDER = "\\b(when|otherwise)\\b";
    private static final String IDENTIFIER = "\\b(?!global|local)[a-z_][a-z0-9_]*\\b";
    private static final String DECI = "\\b\\d+\\.\\d{1,5}\\b";
    private static final String DISCRETE = "\\b\\d+\\b";
    private static final String TINYTXT = "'[^']'";
    private static final String ONOFF = "\\b(yep|nope)\\b";

    private static final String QUOTE = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String HINT = "===.*|<<<[\\s\\S]*?>>?";
    private static final String OPERATOR = "[=+\\-*/%]";
    private static final String COMMASHOMMA = "[(){};,]";
    private static final String BOOSTER = "\\b\\d+(\\.\\d{1,5})?\\s*\\^\\s*\\d+(\\.\\d{1,5})?\\b";
    private static final String WHITESPACE = "\\s+";
    public static List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERNS.matcher(input);
        String scope = "global"; // Start with global scope
        int blockDepth = 0; // To track block level depth (inside nested blocks)

        while (matcher.find()) {
            // Update scope based on curly braces
            if (matcher.group("COMMASHOMMA") != null) {
                String match = matcher.group("COMMASHOMMA");
                if (match.equals("{")) {
                    blockDepth++;
                    scope = "local"; // Entering a block means local scope
                } else if (match.equals("}")) {
                    blockDepth--;
                    if (blockDepth == 0) {
                        scope = "global"; // Exiting the block returns to global scope
                    }
                }
            }

            // Process each token based on the matching groups
            if (matcher.group("RESTRICTED") != null) {
                tokens.add(new Token(TokenType.RESTRICTED, matcher.group()));
                if (matcher.group().equals("local")) {
                    scope = "local"; // explicitly set scope if local is encountered
                }
                symbolTable.insert(matcher.group(), "keyword", scope);
            } else if (matcher.group("AGAINPLS") != null)
                tokens.add(new Token(TokenType.AGAINPLS, matcher.group()));
            else if (matcher.group("DECIDER") != null)
                tokens.add(new Token(TokenType.DECIDER, matcher.group()));
            else if (matcher.group("ONOFF") != null)
                tokens.add(new Token(TokenType.ONOFF, matcher.group()));

            else if (matcher.group("IDENTIFIER") != null) {
                tokens.add(new Token(TokenType.IDENTIFIER, matcher.group()));
                symbolTable.insert(matcher.group(), "identifier", scope);
            } else if (matcher.group("DECI") != null) {
                tokens.add(new Token(TokenType.DECI, matcher.group()));
            } else if (matcher.group("DISCRETE") != null) {
                tokens.add(new Token(TokenType.DISCRETE, matcher.group()));
                symbolTable.insert(matcher.group(), "discrete", scope);
            } else if (matcher.group("TINYTXT") != null) {
                tokens.add(new Token(TokenType.TINYTXT, matcher.group()));
            } else if (matcher.group("QUOTE") != null) {
                tokens.add(new Token(TokenType.QUOTE, matcher.group()));
                symbolTable.insert(matcher.group(), "quote", scope);
            } else if (matcher.group("HINT") != null) {
                tokens.add(new Token(TokenType.HINT, matcher.group()));
                symbolTable.insert(matcher.group(), "comment", scope);
            } else if (matcher.group("BOOSTER") != null) {
                tokens.add(new Token(TokenType.BOOSTER, matcher.group()));
            } else if (matcher.group("OPERATOR") != null) {
                tokens.add(new Token(TokenType.OPERATOR, matcher.group()));
                symbolTable.insert(matcher.group(), "operator", scope);
            } else if (matcher.group("COMMASHOMMA") != null) {
                tokens.add(new Token(TokenType.COMMASHOMMA, matcher.group()));
            }
        }
        return tokens;
    }
   
    private static final Pattern TOKEN_PATTERNS = Pattern.compile(
        "(?<BOOSTER>" + BOOSTER + ")|"
        + "(?<HINT>" + HINT + ")|"
        + "(?<RESTRICTED>" + RESTRICTED + ")|"
        + "(?<AGAINPLS>" + AGAINPLS + ")|"
        + "(?<DECIDER>" + DECIDER + ")|"
          + "(?<ONOFF>" + ONOFF + ")|"

        + "(?<IDENTIFIER>" + IDENTIFIER + ")|"
        + "(?<DECI>" + DECI + ")|"
        + "(?<DISCRETE>" + DISCRETE + ")|"
        + "(?<TINYTXT>" + TINYTXT + ")|"
        + "(?<QUOTE>" + QUOTE + ")|"
        + "(?<OPERATOR>" + OPERATOR + ")|"
        + "(?<COMMASHOMMA>" + COMMASHOMMA + ")|"
        + "(?<WHITESPACE>" + WHITESPACE + ")"
    );
 
    private static void printTransitionTable(DFA dfa) {
        System.out.println("+--------+------------+------------+");
        System.out.println("| Symbol | From State | To State   |");
        System.out.println("+--------+------------+------------+");

        for (State state : dfa.states) {
            for (Map.Entry<Character, List<State>> entry : state.transitions.entrySet()) {
                for (State nextState : entry.getValue()) {
                    System.out.printf("| %-6s | %-10d | %-10d |\n",
                            entry.getKey(), state.id, nextState.id);
                }
            }
        }

        System.out.println("+--------+------------+------------+");
    }

    private static final String REGEX_PATTERN = "int|return|void|show|global|local|onoff|tinytxt";
    private static final String REGEX_PATTERN1 = "aslongas|traverse";
    private static final String REGEX_PATTERN2 = "[=+\\-*/%]";
    private static final String REGEX_PATTERN3 = " when|otherwise";
    private static final String REGEX_PATTERN4 = "?!global|local)[a-z_][a-z0-9_]*";
    private static final String REGEX_PATTERN5 = "\\d+\\.\\d{1,5}";
    private static final String REGEX_PATTERN6 = "\\b\\d+\\b";
    private static final String REGEX_PATTERN7 = "'[^']'";
    private static final String REGEX_PATTERN8 = "([^\"\\\\]|\\\\.)*";
    private static final String REGEX_PATTERN9 = "===.|<<<[\\s\\S]?>>?";
    private static final String REGEX_PATTERN10 = "[=+\\-*/%]";
    private static final String REGEX_PATTERN11 = "[(){};,]";
    private static final String REGEX_PATTERN12 = "\\b\\d+(\\.\\d{1,5})?\\s*\\^\\s*\\d+(\\.\\d{1,5})?\\b";


   
    public static void main(String[] args) {
        InputStream inputStream = Lexer.class.getResourceAsStream("input.fi");
        if (inputStream == null) {
            System.out.println("Error reading file: input.txt");
            return;
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }
        List<Token> tokens = tokenize(content.toString());
        System.out.println("Tokens:");
        for (Token token : tokens) {
            System.out.println(token);
        }

        System.out.println("\nTotal number of tokens: " + tokens.size());
        symbolTable.printTable();
        ErrorHandler.checkForErrors(tokens, content.toString());
        System.out.println();
        System.out.println();
        Scanner scanner = new Scanner(System.in);
        System.out.print("Do you want to print DFA transition tables? (yes/no): ");
        String response = scanner.nextLine().trim().toLowerCase();

        if (response.equals("yes")) {
            NFA[] nfas = {
                RegexToNFAConverter.convert(REGEX_PATTERN),
                RegexToNFAConverter.convert(REGEX_PATTERN1),
                RegexToNFAConverter.convert(REGEX_PATTERN2),
                RegexToNFAConverter.convert(REGEX_PATTERN3),
                RegexToNFAConverter.convert(REGEX_PATTERN4),
                RegexToNFAConverter.convert(REGEX_PATTERN5),
                RegexToNFAConverter.convert(REGEX_PATTERN6),
                RegexToNFAConverter.convert(REGEX_PATTERN7),
                RegexToNFAConverter.convert(REGEX_PATTERN8),
                RegexToNFAConverter.convert(REGEX_PATTERN9),
                RegexToNFAConverter.convert(REGEX_PATTERN10),
                RegexToNFAConverter.convert(REGEX_PATTERN11),
                RegexToNFAConverter.convert(REGEX_PATTERN12)
            };

            for (int i = 0; i < nfas.length; i++) {
                DFA dfa = NFAToDFAConverter.convert(nfas[i]);
                System.out.println("\nDFA Transition Table for RE " + (i + 1) + ":");
                printTransitionTable(dfa);
            }
        }

        scanner.close();
    }
}