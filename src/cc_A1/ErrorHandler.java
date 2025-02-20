package cc_A1;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ErrorHandler {

    private static final Set<String> VALID_KEYWORDS = new HashSet<>();
    private static final Set<String> declaredVariables = new HashSet<>();

    static {
        // Add all defined keywords from your lexer
        VALID_KEYWORDS.add("int");
        VALID_KEYWORDS.add("return");
        VALID_KEYWORDS.add("void");
        VALID_KEYWORDS.add("show");
        VALID_KEYWORDS.add("global");
        VALID_KEYWORDS.add("local");
        VALID_KEYWORDS.add("onoff");
        VALID_KEYWORDS.add("tinytxt");
        VALID_KEYWORDS.add("aslongas");
        VALID_KEYWORDS.add("traverse");
        VALID_KEYWORDS.add("when");
        VALID_KEYWORDS.add("otherwise");
        VALID_KEYWORDS.add("yep");
        VALID_KEYWORDS.add("nope");
    }

    public static void checkForErrors(List<Token> tokens, String input) {
        // Remove multi-line comments <<< ... >>>
    	
    	System.out.println();
    	System.out.println();
    	System.out.println("\033[1mERROR TABLE:\033[0m");

        String codeWithoutComments = removeComments(input);
        
        String[] lines = codeWithoutComments.split("\n");
        int lineNumber = 1;

        for (String line : lines) {
            boolean hasAssignmentOrFunctionCall = containsAssignmentOrFunctionCall(line);

            for (Token token : tokens) {
                if (line.contains(token.getValue())) {

                    // ERROR 1: Unknown token
                    if (token.getType() == TokenType.UNKNOWN) {
                        System.out.println("⚠ Error: Unknown token '" + token.getValue() + "' at line " + lineNumber);
                    }
//
                    // ERROR 2: Standalone string literals
                    if (token.getType() == TokenType.QUOTE && !hasAssignmentOrFunctionCall) {
                        System.out.println("⚠ Error: Standalone string '" + token.getValue() + "' at line " + lineNumber);
                    }
//
                    // ERROR 4: Potentially undefined keyword (misspelled)
                    if (token.getType() == TokenType.IDENTIFIER) {
                        if (isPotentiallyMisspelledKeyword(token.getValue())) {
                            System.out.println("⚠ Error: Unknown keyword or misspelled identifier '" + token.getValue() + "' at line " + lineNumber);
                        }
                    }
                }
            }

            // Track declared variables (both local and global)
            if (line.matches("\\s*(global|local|int)\\s+\\w+(\\s*=\\s*\\d+(\\.\\d+)?|\\s*;|\\s*,\\s*\\w+\\s*=\\s*\\d+(\\.\\d+)?)?")) {
                // Extract variable names
                String[] words = line.split("\\s+");
                for (int i = 1; i < words.length; i++) {
                    String word = words[i].replaceAll("[,;=]", "").trim();  // Remove special characters
                    if (!VALID_KEYWORDS.contains(word)) {  // Ensure it's not a keyword
                        declaredVariables.add(word);
                    }
                }
            }

            // ERROR 6: Detect missing '=' in int variable declarations but allow function definitions
            if (line.matches("\\s*int\\s+\\w+\\s+\\d+(\\.\\d+)?\\s*;?")) { 
                if (!line.matches("\\s*int\\s+\\w+\\s*\\(.\\)\\s\\{.*")) { // Allow int launch() {}
                    System.out.println("⚠ Error: Missing '=' in variable declaration at line " + lineNumber);
                }
            }

            // ERROR 7: Incorrect variable declarations (missing datatype)
            Pattern incorrectVarPattern = Pattern.compile("^(?!\\b(int|double|float|char|String|boolean|long|short|byte)\\b)\\s*\\b[a-zA-Z_]\\w*\\s*;");
            Matcher incorrectVarMatcher = incorrectVarPattern.matcher(line);
            if (incorrectVarMatcher.find()) {
                System.out.println("⚠ Error: Incorrect variable declaration '" + incorrectVarMatcher.group().trim() + "' on line " + lineNumber + ". Missing datatype.");
            }
            
            if (line.contains("\"")) {
                // Count the number of quotes in the line
                long quoteCount = line.chars().filter(ch -> ch == '"').count();
                // If the number of quotes is odd, the string is unterminated
                if (quoteCount % 2 != 0) {
                    System.out.println("⚠ Error: Unterminated string literal at line " + lineNumber + ". Missing closing quote.");
                }
            }    

            // ERROR 8: Unexpected tokens (identifiers starting with a number)
            Pattern unexpectedTokenPattern = Pattern.compile("\\b\\d+[a-zA-Z_]\\w*\\b");
            Matcher unexpectedTokenMatcher = unexpectedTokenPattern.matcher(line);
            while (unexpectedTokenMatcher.find()) {
                System.out.println("⚠ Error: Unexpected token '" + unexpectedTokenMatcher.group() + "' at line " + lineNumber + ". Identifiers cannot start with a number.");
            }

            // ERROR 9: Redeclaration of variables with invalid symbols (e.g., $c)
            Pattern invalidRedeclarationPattern = Pattern.compile("\\$\\w+");
            Matcher invalidRedeclarationMatcher = invalidRedeclarationPattern.matcher(line);
            while (invalidRedeclarationMatcher.find()) {
                String variable = invalidRedeclarationMatcher.group().substring(1); // Remove the '$'
                if (declaredVariables.contains(variable)) {
                    System.out.println("⚠ Error: Invalid redeclaration of variable '" + variable + "' with '$' at line " + lineNumber);
                }
            }
            Pattern illegalAfterTerminatorPattern = Pattern.compile("[;}]\\s*([^\\w\\s();{}\"=+*/<>-])");
            Matcher illegalAfterTerminatorMatcher = illegalAfterTerminatorPattern.matcher(line);
            while (illegalAfterTerminatorMatcher.find()) {
                System.out.println("⚠ Error: Illegal character '" + illegalAfterTerminatorMatcher.group(1) + "' found after statement terminator at line " + lineNumber);
            }


            // ERROR 11: Detect missing start of multi-line comment
            if (line.contains(">>>") && !input.contains("<<<")) {
                System.out.println("⚠ Error: Multi-line comment ending found (>>>) without a starting delimiter (<<<) at line " + lineNumber);
            }
            
         // Remove all string literals from the line to avoid false positives
            Pattern stringPattern = Pattern.compile("\"([^\"])\"|'([^'])'");
            Matcher stringMatcher = stringPattern.matcher(line);
            String lineWithoutStrings = line;

            // Replace quoted content with placeholders (preserving string length)
            while (stringMatcher.find()) {
                String match = stringMatcher.group();
                String placeholder = match.replaceAll(".", " "); // Replace all characters with spaces
                lineWithoutStrings = lineWithoutStrings.replace(match, placeholder);
            }

            // ERROR: Identifiers should not contain capital letters (ignoring strings)
            Pattern capitalLetterIdentifierPattern = Pattern.compile("\\b[A-Z_]+\\w*\\b"); 
            Matcher capitalLetterIdentifierMatcher = capitalLetterIdentifierPattern.matcher(lineWithoutStrings);

            // Only print the error if a match is found
            while (capitalLetterIdentifierMatcher.find()) {  
                System.out.println("⚠ Error: Identifier '" + capitalLetterIdentifierMatcher.group() + "' contains capital letters at line " + lineNumber);
            }



           

            lineNumber++;
        }
        
    }

    // Removes multi-line comments <<< ... >>>
    private static String removeComments(String code) {
        return code.replaceAll("<<<[\\s\\S]*?>>>", "");  // Regex to remove multi-line comments
    }

    // Checks if a line contains assignment (=) or a function call with ()
    private static boolean containsAssignmentOrFunctionCall(String line) {
        return Pattern.compile("=.|\\w+\\s\\(").matcher(line).find();
    }

    // Checks if an identifier closely resembles a keyword but is incorrect
    private static boolean isPotentiallyMisspelledKeyword(String word) {
        for (String keyword : VALID_KEYWORDS) {
            if (isSimilar(keyword, word)) {
                return true;
            }
        }
        return false;
    }

    // Checks if two words are similar (allowing minor typos)
    private static boolean isSimilar(String keyword, String word) {
        if (Math.abs(keyword.length() - word.length()) > 1) {
            return false;
        }

        int differences = 0;
        for (int i = 0, j = 0; i < keyword.length() && j < word.length(); i++, j++) {
            if (keyword.charAt(i) != word.charAt(j)) {
                differences++;
                if (differences > 1) {
                    return false;
                }
                if (keyword.length() > word.length()) {
                    j--; // Skip a char in keyword
                } else if (keyword.length() < word.length()) {
                    i--; // Skip a char in word
                }
            }
        }
        return true;
    }
}