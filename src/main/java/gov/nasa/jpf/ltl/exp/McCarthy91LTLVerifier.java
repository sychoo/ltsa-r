
package gov.nasa.jpf.ltl.exp;

import gov.nasa.jpf.ltl.LTLVerifier;

/**
 * This program uses the LTLVerifier to verify McCarthy91 class.
 * 
 * @author Anh Cuong
 */
public class McCarthy91LTLVerifier {

  public static void main(String[] args) throws Exception{
    String[] arg = { "+classpath=build/classes",
                     "gov.nasa.jpf.ltl.exp.McCarthy91",
                    "<>(assertion:gov.nasa.jpf.ltl.exp.McCarthy91.mcCarthy91.n==90)"
                     };
    
    LTLVerifier.main(arg);
  }
  
}
