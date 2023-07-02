
package gov.nasa.jpf.ltl.exp;

import gov.nasa.jpf.ltl.LTLVerifier;

/**
 * This program uses the LTLVerifier to verify Test class.
 * 
 * @author Anh Cuong
 */
public class SimpleLTLVerifier {
  
  public static void main(String[] args) throws Exception{
    String[] arg = { "+classpath=build/classes",
                      "gov.nasa.jpf.ltl.exp.Simple", "5",
                     "<>(method:gov.nasa.jpf.ltl.exp.Simple.f2I)"
                     };
    LTLVerifier.main(arg);
  } 

}
