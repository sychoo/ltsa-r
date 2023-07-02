package gov.nasa.jpf.ltl.exp;


import gov.nasa.jpf.jvm.Verify;

/**
 * This simple program implements McCarthy91 function
 *
 * @author Anh Cuong
 */
public class McCarthy91 {
    /**
     * @param n     the input for mccarthy91 function
     * @return      the mccarthy91 value
     */
    public static int mcCarthy91(int n){
        n = (n > 100) ? n - 10 : mcCarthy91(mcCarthy91(n+11));

        return n;
    }


    public static void main(String[] args){
      int rand = Verify.random(101);
      mcCarthy91(rand);
    }

}
