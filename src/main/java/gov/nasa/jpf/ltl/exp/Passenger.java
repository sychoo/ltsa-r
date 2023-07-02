package gov.nasa.jpf.ltl.exp;


/**
 *
 * @author Anh Cuong
 */
public class Passenger {

  public static int passengerLeft;

  static public void checkIn() {
    passengerLeft--;
  }

  static public void aboard() {
  }

  static public void waitAndAboard() {
    while (passengerLeft != 0) { /* wait */ }
    aboard();
  }

  public static void main(String[] args) {
    passengerLeft = 1;

    new Thread(new Runnable() {
      public void run() {
        checkIn();
      }
    }).start();

    new Thread(new Runnable() {
      public void run() {
        waitAndAboard();
      }
    }).start();
  }
}
