package edu.cmu.ltsar.lts;

import java.util.LinkedList;
import java.util.Iterator;

/* MyHash is a speciallized Hashtable/Stack for the composition in the analyser
* it includes a stack structure through the hash table entries
*  -- assumes no attempt to input duplicate key
*  
*/

class MyHashStackEntry  {
    byte[] key;
    int stateNumber;
    boolean marked;
    MyHashStackEntry next;   //for linking buckets in hash table
    MyHashStackEntry link;   //for queue linked list
    MyHashStackEntry list;   //list of all entries
  
    MyHashStackEntry(byte[] l) {
        key =l; stateNumber=-1; next = null; link = null; list = null; marked=false;
    }

    MyHashStackEntry(byte[] l, int n) {
        key =l; stateNumber =n; next = null; link =null; list = null; marked=false;
    }

 }

public class MyHashStack implements StackCheck {

    private MyHashStackEntry [] table;
    private int count =0;
    private int depth =0;
    private MyHashStackEntry head = null;
    private MyHashStackEntry listhead = null;

    public MyHashStack(int size) {
        table = new MyHashStackEntry[size];
    }

    public void pushPut(byte[] key) {
        MyHashStackEntry entry = new MyHashStackEntry(key);
        //insert in hash table
        int hash = StateCodec.hash(key) % table.length;
        entry.next=table[hash];
        table[hash]=entry;
        ++count;
        //insert in stack
        entry.link = head;
        head = entry;
        ++depth;
        // insert in list
        entry.list = listhead;
        listhead = entry;
    }
    
    public void pop() { //remove from head of queue
    	 if (head==null) return;
      head.marked = false;
    	 head = head.link;
    	 --depth;
    }
    
    public byte[] peek() { //remove from head of queue
       return head.key;
    }
    
    public void mark(int id) {
    	   head.marked = true;
    	   head.stateNumber = id;
    }
    
    public boolean marked() {
    	   return head.marked;
    }

    public boolean empty() {return head==null;}
    
    public boolean containsKey(byte[] key) {
       int hash = StateCodec.hash(key) % table.length;
        MyHashStackEntry entry = table[hash];
        while (entry!=null) {
            if (StateCodec.equals(entry.key,key)) return true;
            entry = entry.next;
        }
        return false;
    }
    
    public boolean onStack(byte[] key) {
       int hash = StateCodec.hash(key) % table.length;
        MyHashStackEntry entry = table[hash];
        while (entry!=null) {
            if (StateCodec.equals(entry.key,key)) return entry.marked;
            entry = entry.next;
        }
        return false;
    }
    
    public int get(byte[] key) {
       int hash = StateCodec.hash(key) % table.length;
        MyHashStackEntry entry = table[hash];
        while (entry!=null) {
            if (StateCodec.equals(entry.key,key)) return entry.stateNumber;
            entry = entry.next;
        }
        return -99999;
    }


    public int size() {return count;}
   
    
    public int getDepth() {
    	  return depth;
    }
    
    public Iterator iterator() {
        return new MyHashStackIterator(listhead);
    }

}


class MyHashStackIterator implements Iterator {
    
    MyHashStackEntry head;
    MyHashStackEntry current;
    
    MyHashStackIterator(MyHashStackEntry hd){
        head = hd;
        current = hd;
    }
    
    public boolean hasNext() {
        return (current!=null);
    }
    
    public byte[] next (){
        byte[] rr = current.key;
        current = current.list;
        return rr;
    }  
    
    public void remove() {}
    
}
         