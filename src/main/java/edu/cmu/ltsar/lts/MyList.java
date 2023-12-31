package edu.cmu.ltsar.lts;


/* MyList is a speciallized List for the analyser
*  -- its stores transitions*/

class MyListEntry {
    int fromState;
    byte[] toState;
    int actionNo;
    MyListEntry next;
    MyListEntry(int from, byte[] to, int action) {
      fromState = from; toState = to; actionNo = action; this.next = null;
    }
}

public class MyList {
  protected MyListEntry head = null;
  protected MyListEntry tail = null;
  protected int count = 0;
  
  public void add(int from, byte[] to, int action) {
      MyListEntry e = new MyListEntry(from,to,action);
      if (head == null) {
         head = tail = e;
      } else {
         tail.next = e;
         tail = e;
      }
      ++count;
  }
  
  public void next() { if (head!=null) head = head.next;}
  
  public boolean empty() {return head==null;}
  
  public int getFrom() {return head !=null ? head.fromState:-1;}
  public byte[] getTo()   {return head !=null ? head.toState:null;}
  public int  getAction(){return head !=null ? head.actionNo:-1;}
  
  public int size() {return count;}
}

  

  