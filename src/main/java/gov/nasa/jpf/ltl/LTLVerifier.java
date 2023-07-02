package gov.nasa.jpf.ltl;

import java.util.*;

import gov.nasa.jpf.*;
import gov.nasa.jpf.search.*;
import gov.nasa.jpf.report.*;
import gov.nasa.jpf.util.*;
import gov.nasa.jpf.jvm.*;
import gov.nasa.jpf.jvm.bytecode.*;
import gov.nasa.ltl.graph.*;
import gov.nasa.ltl.trans.*;

/**
 * A listener for automation of LTL verification for Java Pathfinder.
 *
 * At this point, the listener can verify method invocation and linear
 * constraint over program variables. It supports both sequential and 
 * concurrent programs.
 *
 * LTL atom must be in the form
 *    method:method_call (for method validation), or
 *    assertion:linear_constraint (for assertion validation)
 *
 * usage: LTLVerifier app-class {app-args ..} ltl-formula
 *                                      +jpf.basedir=../<your-jpf-dir>
 *
 * @author Anh Cuong
 */
public class LTLVerifier extends PropertyListenerAdapter {

  private String ltlFormula;
  private Graph ba;          // an automata of the ltl-formula
  private Vector<Node> currentStates, previousStates;
  private boolean propertyViolence;
  
  /* added in to handle concurrency
   * track the last state of each thread, so that jpf can backtrack */
  private Vector<Vector<Node>> stateTracker;
  

  public static void main(String[] args) throws Exception{
    /* reformat arguments 
     * take out the ltl-formula */
    String[] newArgs = new String[args.length - 1];
    String ltlFormula = args[args.length-1];
    for (int index = 0; index < args.length-1; index++) {
      newArgs[index] = args[index];               // add app
    }

    for (String arg: newArgs)
      System.out.println(arg);

    /* run JPF */
    System.out.println("Verifying property - " + ltlFormula);
    System.out.println("---------------------------------");
    Config conf = JPF.createConfig(newArgs);
    JPF jpf = new JPF(conf);
    LTLVerifier ltlv = new LTLVerifier(ltlFormula);
    jpf.addPropertyListener(ltlv);
    System.out.println("LTL-JPF Started");
    jpf.run();
    System.out.println("LTL-JPF Terminated");

    /* code to print out method call error traces */
    System.out.println("================================== trace");
    Reporter reporter = jpf.getReporter();
    Path path = reporter.getPath();
    if (path.size() != 0) {
      for (Transition t : path) {
        String lastLine = " ";
        String lastNoLine = "";
        Step last = null;
        for (Step s : t) {
          if (!s.equals(last)) {
            String line = s.getLineString();
            if (line != null) {
              Instruction insn = s.getInstruction();
              String methodName = insn.getMethodInfo().getBaseName();
              StringTokenizer st = new StringTokenizer(methodName, ".");
              while (st.hasMoreTokens()) {
                methodName = st.nextToken();
              }
              if (lastLine.contains(methodName)) {
                System.out.print(Left.format(lastNoLine, 30));
                System.out.print(" : ");
                System.out.println(lastLine);
                lastLine = "";
              }
              if (isMethodCall(insn)) {
                lastLine = line.trim();
                lastNoLine = s.getLocationString();
              }
            }
          }
        }
        if (!lastLine.equals("")) {
          System.out.print(Left.format(lastNoLine, 30));
          System.out.print(" : ");
          System.out.println(lastLine);
        }
      }
    }

    /* for testing purpose 
     * System.out.println(ltlv.ltlFormula);
     * System.out.println(ltlv.ba); */
  }

  public LTLVerifier(String ltl) throws Exception{
    ltlFormula = ltl;
    ba = LTL2Buchi.translate(ltlFormula);
    previousStates = new Vector<Node>();
    currentStates = new Vector<Node>();
    currentStates.add(ba.getInit());
    propertyViolence = false;
    stateTracker = new Vector<Vector<Node>>();
    stateTracker.add(currentStates);
  }
  
  @Override
  public boolean check(Search search, JVM vm) {
    return !propertyViolence;
  }

  /**
   * Indicate whether an instruction should 
   * be tracked to build the program model
   * 
   * @param insn  instruction to be checked
   * @return      true if insn should be tracked; false otherwise
   */
  public boolean selectedInstruction(Instruction insn) {
    return !isLibrary(insn);
  }

  /**
   * Indicate whether the an instruction (a program
   * model transition) satisfies a set of method_call predicates
   * 
   * @param insn          instruction to be checked
   * @param ti            thread to be checked
   * @param predicateSet  a set of predicates
   * @return              true if insn satisfies all predicates in predicateSet; false otherwise
   */
  private boolean validate1(Instruction insn, ThreadInfo ti, Vector<String> guards) {
    for (String guard: guards) {
      boolean isMethodCall = isMethodCall(insn);

      /* get method name of the current instruction */
      String methodName = "";
      if (isMethodCall) {
        methodName = ((InvokeInstruction) insn).getInvokedMethod().getCompleteName();
      }

      /* predicate is an atom
       * if it is a method atom
       * return false when methodName does not equal with method atom name */
      if (!guard.startsWith("!")) {
        String[] token = guard.toString().split(":");
        if (token[0].equals("method") &&
                 (!methodName.equals(token[1]) || !isMethodCall))
          return false;
      }
      /* predicate is a not an atom
       * if it is a not method atom
       * return false when methodName equals with method atom name */
      else {
        guard = guard.substring(1);
        String[] token = guard.split(":");
        if (token[0].equals("method") &&
                isMethodCall && methodName.equals(token[1])) {
          return false;
        }
      }
    }
    
    return true;
  }

  /**
   * Indicate whether the an instruction (a program
   * model transition) satisfies a set of assertion predicates
   * 
   * @param insn          instruction to be checked
   * @param ti            thread to be checked
   * @param predicateSet  a set of predicates
   * @return              true if insn satisfies all predicates in predicateSet; false otherwise
   */
  private boolean validate2(Instruction insn, ThreadInfo ti, Vector<String> guards) {
    // System.out.println(guards);
    for (String guard: guards) {
      String[] variableList = insn.getMethodInfo().getLocalVariableNames();
      String methodName = insn.getMethodInfo().getBaseName();
      String token[];

      String operator = "";

      if (!guard.startsWith("!"))
        token = guard.split(":");
      else
        token = guard.substring(1).split(":");

      if (token[0].equals("assertion")) {
        String assertion = token[1];
        if (assertion.contains(">=")) 
          operator = ">=";
        else if (assertion.contains("<=")) 
          operator = "<=";
        else if (assertion.contains(">"))
          operator = ">";
        else if (assertion.contains("<"))
          operator = "<";
        else if (assertion.contains("=="))
          operator = "==";
        else if (assertion.contains("!="))
          operator = "!=";
        
        String variableName = assertion.split(operator)[0];
        int value = Integer.parseInt(assertion.split(operator)[1]);

        boolean checkAssertion = false;
        for (String varName: variableList){
          /* check for a match in variable name */
          String fullVarName = methodName + "." + varName;
          if (fullVarName.equals(variableName)){
            checkAssertion = true;
            variableName = varName;
            break;
          }
        }
        /* operator should not be empty */
        checkAssertion = checkAssertion && !operator.equals("");
        
        if (checkAssertion) {
          int actualValue = ti.getLocalVariable(variableName);
          boolean compareValue = true;
          
          if (operator.equals(">=")) 
            compareValue = (actualValue >= value);
          else if (operator.equals("<=")) 
            compareValue = (actualValue <= value);
          else if (operator.equals(">")) 
            compareValue = (actualValue > value);
          else if (operator.equals("<")) 
            compareValue = (actualValue < value);
          else if (operator.equals("==")) 
            compareValue = (actualValue == value);
          else if (operator.equals("!="))
            compareValue = (actualValue != value);

          if (!guard.startsWith("!") && !compareValue){
            // System.out.println(variableName+ "=" + actualValue);
            return false;
          }
          else if (guard.startsWith("!") && compareValue) 
            return false;
        } else
          return false;
      }
    }
    
    return true;
  }
  /**
   * Return all possible next states for the
   * current instruction (a program model transition)
   * 
   * @param insn          instruction to be checked
   * @param ti            thread to be checked
   * @return              set of possible next states
   */
  private Vector<Node> validate(Instruction insn, ThreadInfo ti) {
    Vector<Node> nextStates = new Vector<Node>();

    for (int inx = 0; inx < currentStates.size(); inx++) {
      Vector<Edge> transitions = new Vector<Edge>();
      for (Node node: currentStates){
        transitions.addAll(node.getOutgoingEdges());
      }
      
      for (Edge transition: transitions) {
        /* got the predicate set over the transition */
        String guard = transition.getGuard();

        /* got a tail state for this transition*/
        Node nextState = transition.getNext();

        /* if insn passes all validation test
          * add this node to next state set */
        boolean getThisTail =  validate1(insn, ti, getGuards(guard))
                            && validate2(insn, ti, getGuards(guard));
        if (getThisTail && !nextStates.contains(nextState))
          nextStates.add(nextState);
      }
    }
    
    return nextStates;
  }

  /**
   * A guard can be of the form (p1|!p1)&(p2|!p2)&...&(pn|!pn)
   * @param guard
   * @return a vector of conjunction elements {p1,!p2,...,!pn}
   */
  private Vector<String> getGuards(String conjunctionGuard){
    Vector<String> guards = new Vector<String>();
    String[] conjunctionElement =  conjunctionGuard.split("&");
    for (String guard: conjunctionElement)
      guards.add(guard);

    return guards;
  }

  private Vector<Node> getAcceptedNodes(Graph graph){
    Vector<Node> acceptedNodes = new Vector<Node>();
    for (Node node: graph.getNodes()){
      if (node.getBooleanAttribute("accepting"))
        acceptedNodes.add(node);
    }

    return acceptedNodes;
  }

  @Override
  public void executeInstruction(JVM vm) {
    // System.out.println(currentStates);
    Instruction insn = vm.getLastInstruction();

    ThreadInfo ti = vm.getLastThreadInfo();

    /* track only selected instruction */
    if (selectedInstruction(insn)) {
      Vector<Node> nextStates = validate(insn, ti);

      /* for testing purpose
       * System.out.println(insn);
       * System.out.println(currentStates);
       */

      if (nextStates.size() > 0) {
        previousStates = currentStates;
        currentStates = nextStates;
      } else {
        propertyViolence = true;
        // we're done, report as quickly as possible
        ti.breakTransition();
      }
      
    }
    
  }

  @Override
  public String getErrorMessage() {
    return "The property " + ltlFormula + " is violated.\n";
  }

  @Override
  public void stateAdvanced(Search search) {
    stateTracker.add(currentStates);
    if (!search.hasNextState()) {
      propertyViolence = true;
      Vector<Node> finalStates = getAcceptedNodes(ba);
      for (Node node: previousStates) {
        if (finalStates.contains(node))
          propertyViolence = false;
      }
    }
  }

  @Override
  public void stateBacktracked(Search search) {
    stateTracker.remove(stateTracker.size() - 1);
    currentStates = stateTracker.get(stateTracker.size() - 1);
  }

  /**
   * Indiate whether an instruction is a method invocation
   * 
   * @param insn  instruction to be checked
   * @return      true if insn is a method call; false otherwise
   */
  static public boolean isMethodCall(Instruction insn) {
    return insn instanceof InvokeInstruction;
  }

  /**
   * Indiate whether an instruction is a library instruction
   *
   * @param insn  instruction to be checked
   * @return      true if insn is library instruction; false otherwise
   */
  public boolean isLibrary(Instruction insn) {
    MethodInfo mi = insn.getMethodInfo();
    return (mi.getCompleteName().startsWith("java.")
            || mi.getCompleteName().startsWith("javax.")
            || mi.getCompleteName().startsWith("sun.")
            || mi.getCompleteName().startsWith("com.sun.")
            );
  }
  
}
