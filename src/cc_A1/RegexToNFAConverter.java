package cc_A1;

import java.util.*;
import java.util.regex.*;

class RegexToNFAConverter {
    private static int stateCounter = 0;

    public static NFA convert(String regex) {
        String[] tokens = regex.split("\\|");
        State start = new State(stateCounter++);
        State end = new State(stateCounter++);
        NFA nfa = new NFA(start);
        nfa.states.add(end);
        nfa.finalStates.add(end);

        for (String token : tokens) {
            State tokenStart = new State(stateCounter++);
            State tokenEnd = new State(stateCounter++);
            nfa.states.add(tokenStart);
            nfa.states.add(tokenEnd);
            start.addTransition('\0', tokenStart);
            tokenEnd.addTransition('\0', end);

            State prev = tokenStart;
            for (char c : token.toCharArray()) {
                State next = new State(stateCounter++);
                prev.addTransition(c, next);
                nfa.states.add(next);
                prev = next;
            }
            prev.addTransition('\0', tokenEnd);
        }
        return nfa;
    }
}

class NFAToDFAConverter {
    public static DFA convert(NFA nfa) {
        Map<Set<State>, State> dfaStates = new HashMap<>();
        Queue<Set<State>> queue = new LinkedList<>();
        Set<State> startSet = epsilonClosure(Set.of(nfa.start));
        State startState = new State(0);
        dfaStates.put(startSet, startState);
        queue.add(startSet);
        DFA dfa = new DFA(startState);

        while (!queue.isEmpty()) {
            Set<State> currentSet = queue.poll();
            State dfaState = dfaStates.get(currentSet);

            for (char symbol = ' '; symbol <= '~'; symbol++) { // Handle all printable ASCII chars
                Set<State> nextSet = move(currentSet, symbol);
                nextSet = epsilonClosure(nextSet);

                if (!nextSet.isEmpty() && !dfaStates.containsKey(nextSet)) {
                    State newState = new State(dfaStates.size());
                    dfaStates.put(nextSet, newState);
                    queue.add(nextSet);
                    dfa.states.add(newState);

                    if (containsFinalState(nextSet, nfa.finalStates)) {
                        newState.isFinal = true;
                    }
                }

                if (!nextSet.isEmpty()) {
                    dfaState.addTransition(symbol, dfaStates.get(nextSet));
                }
            }
        }
        return dfa;
    }

    private static boolean containsFinalState(Set<State> states, Set<State> finalStates) {
        for (State state : states) {
            if (finalStates.contains(state)) return true;
        }
        return false;
    }

    private static Set<State> epsilonClosure(Set<State> states) {
        Set<State> closure = new HashSet<>(states);
        Stack<State> stack = new Stack<>();
        stack.addAll(states);

        while (!stack.isEmpty()) {
            State state = stack.pop();
            if (state.transitions.containsKey('\0')) {
                for (State next : state.transitions.get('\0')) {
                    if (!closure.contains(next)) {
                        closure.add(next);
                        stack.push(next);
                    }
                }
            }
        }
        return closure;
    }

    private static Set<State> move(Set<State> states, char symbol) {
        Set<State> result = new HashSet<>();
        for (State state : states) {
            if (state.transitions.containsKey(symbol)) {
                result.addAll(state.transitions.get(symbol));
            }
        }
        return result;
    }
}