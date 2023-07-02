package gov.nasa.jpf.ltl.exp;

import java.util.Vector;

/**
 * This simple program contains method invocation,
 * assignment statements and control statements
 * 
 * @author Anh Cuong
 */
public class Simple {

  public static void main(String[] args) {
    Vector v = new Vector();
    f1();
    f2(1,2);

    /*
    int x = Integer.parseInt(args[0]);
    int y = Integer.parseInt(args[1]);

    f2(x,y);

    f3(); */
  }

  public static void f1() {
  }

  public static void f3() {
  }

  public static int f2(int x, int y) {
    if (x > 3){
      if (y < 5){ f1(); return 1;}
      f3(); return 100;
    }

    if (y < 5) { f1(); return 100;}
    f3(); return 100;
  }
}
