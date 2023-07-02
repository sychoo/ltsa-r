
package gov.nasa.jpf.ltl.exp;

import gov.nasa.jpf.ltl.LTLVerifier;

/**
 * This program uses the LTLVerifier to verify Passenger class.
 * 
 * @author Anh Cuong
 */
public class PassengerLTLVerifier {
  
  public static void main(String[] args) throws Exception{
    String[] arg = { "res.min.sample.Passenger",
                     "G{{method:Passenger.checkIn}->{X{F{method:Passenger.aboard}}}}",
                     "+jpf.basedir=../trunk", };

    LTLVerifier.main(arg);
  }

}
