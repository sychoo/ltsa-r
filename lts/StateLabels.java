package lts;

import java.util.*;

public class StateLabels {

    CompactState target;
    MyHashStack  labels;  //list of fluent values for each state in target
    StateCodec   coder;     //decode state values
    String []    fluents; //fluent names
    SortedMap    FtoS;     //fluent to state map
    
    
    
    StateLabels(CompactState t, MyHashStack ms, StateCodec sc, String[] nn) {
        target = t;
        labels = ms;
        coder = sc;
        fluents = nn;
        makeFtoS();
    }
    
    void makeFtoS(){
        FtoS = new TreeMap();
        Iterator ii = labels.iterator();
        while (ii.hasNext()) {
           byte[] ls = (byte[])ii.next();
           int[] state = coder.decode(ls);
           if (target.hasControls(state[0])) {
               int ts = state[0]; state[0] = 0;
               Ints key = new Ints(state); 
               BitSet bb = (BitSet)FtoS.get(key);
               if (bb==null) {
                   bb = new BitSet(target.alphabet.length); 
                   EventState.hasEvents(target.states[ts],bb);
                   bb.and(target.Controlled);
                   FtoS.put(key,bb);
               } else {
                   EventState.hasEvents(target.states[ts],bb);
                   bb.and(target.Controlled);
               }
           }
        }
    }
        
    public void print(LTSOutput output) {
        output.outln("controller:-");
        Set nn = FtoS.keySet();
        Iterator ii = nn.iterator();
        while (ii.hasNext()) {
           Ints key = (Ints) ii.next();
           int [] state = key.value();
           printLabel(state, output);
           BitSet bb = (BitSet)FtoS.get(key);
           output.out(" -> ");
           printStates(bb,output);
           output.outln(" ");
        }
    }

   
   void printLabel(int[] state, LTSOutput out) {
        for (int i =1; i<state.length; ++i) {
            if (state[i]==1) out.out(" "+fluents[i]);
        }
   }
   
   void printStates(BitSet actions, LTSOutput output) {
       Vector names = new Vector();
       for (int j=1;j<target.alphabet.length; ++j){
           if (actions.get(j)) names.add(target.alphabet[j]);
       }
       output.out((new Alphabet(names)).toString());
   }      
    
}

class Ints implements Comparable {
    
    int[] val;
    
    Ints (int[] v) {
        val =v;
    }
    
    int[] value() {
        return val;
    }
    
     public int compareTo (Object o) {
       if (o==null) return -1;
       Ints b = (Ints) o; 
       for (int i =0; i<this.val.length;  ++i) {
            if (this.val[i]<b.val[i]) return 1;
            if (this.val[i]>b.val[i]) return -1; 
  	  }
       return 0;
    }
    
   
}
