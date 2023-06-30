package lts.ltl;
import lts.*;
import java.util.*;
/* -----------------------------------------------------------------------*/

public class FluentProcess extends CompactState {
    
    BitSet isTrue;
    BitSet isFalse;
    
    int initialState = 0;
    
    static SortedMap fluents;
    
    static void make(Symbol name, boolean init, Vector trueActions, Vector falseActions) {
        if(fluents==null) fluents = new TreeMap();
        if(fluents.put(name.toString(),new FluentProcess(name,init,trueActions,falseActions))!=null) 
        Diagnostics.fatal ("duplicate fluent process definition: "+name, name);       
    }
    
    
    private FluentProcess(Symbol name, boolean init, Vector trueActions, Vector falseActions) {
        initialState = init? 1 : 0;
        makeAlphabet(trueActions, falseActions);
        makeStates(name);
        
    }
    
    public static Vector getFluents() {
        if (fluents==null) return null;
        Vector v = new Vector();
        v.addAll(fluents.values());
        return v;
    }
    
    public static String[] getNames() {
        Set nn = fluents.keySet();
        String[] ns = new String[nn.size()+1];
        Iterator ii = nn.iterator();
        int i = 1;
        while (ii.hasNext()) {
            ns[i]= (String) ii.next();
            ++i;
        }
        return ns;
    }
   
    
    static void init(){
    	  fluents = null;
    }
    
    private void makeStates(Symbol name) {
        this.name = name.toString();
        this.maxStates = 2;
        this.states = new EventState[maxStates];
        BitSet falseTrans = new BitSet();
        for (int i=0; i<alphabet.length; ++i) {
            if (isFalse.get(i)) {
                states[0] = EventState.add(states[0], new EventState(i,0));
                states[1] = EventState.add(states[1], new EventState(i,0));
            } else if (isTrue.get(i)) {
                states[1] = EventState.add(states[1], new EventState(i,1));
                states[0] = EventState.add(states[0], new EventState(i,1));
            }         
	}
    }

    private void makeAlphabet(Vector trueActions, Vector falseActions) {
        int len = trueActions.size()+falseActions.size()+1; //labels + tau + extra
        alphabet = new String[len];
        isTrue = new BitSet(len);
        isFalse = new BitSet(len);
        alphabet[0] = "tau";
        int pos = 1;
        Iterator ii = falseActions.iterator();
        while (ii.hasNext()) {
            alphabet[pos] = (String)ii.next();
            isFalse.set(pos);
            ++pos;
        }
        ii = trueActions.iterator();
        while (ii.hasNext()) {
            alphabet[pos] = (String)ii.next();
            isTrue.set(pos);
            ++pos;
        }
    }


}



