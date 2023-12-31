package edu.cmu.ltsar.lts;
import java.util.*;
import java.io.*;

public class CompactState implements Automata {
    public String name;
    public int maxStates;
    public String[] alphabet;
    public EventState []states; // each state is to a vector of <event, nextstate>
    int endseq = -9999; //number of end of sequence state if any
    
    BitSet unControlled; // set of uncontrolled actions
    BitSet Controlled;   // set of conttrolled actions


    public  CompactState() {} // null constructor


    public CompactState(int size, String name, MyHashStack statemap, MyList transitions, String[] alphabet, int endSequence) {
        this.alphabet = alphabet;
        this.name = name;
        maxStates = size;
        states = new EventState[maxStates];
        while(!transitions.empty()) {
            int fromState = (int)transitions.getFrom();
            int toState   = transitions.getTo()==null?-1:statemap.get(transitions.getTo());
            states[fromState]= EventState.add(states[fromState],new EventState(transitions.getAction(),toState));
            transitions.next();
        }
        endseq = endSequence;
    }
    
     
    public void reachable() {
    	  MyIntHash otn = EventState.reachable(states);
    	  //System.out.println("reachable states "+otn.size()+" total states "+maxStates);
    	  // always do reachable for better layout!!
    	  //if (otn.size() == maxStates) return;
    	  EventState[] oldStates = states;
    	  maxStates = otn.size();
    	  states = new EventState[maxStates];
    	  for (int oldi = 0; oldi<oldStates.length; ++oldi) {
    	  	int newi = otn.get(oldi);
    	  	if (newi>-2) {
    	  		states[newi] = EventState.renumberStates(oldStates[oldi],otn);
    	  	}
    	  }
    	  if (endseq>0) endseq = otn.get(endseq);
    }
    
    // change (a ->(tau->P|tau->Q)) to (a->P | a->Q)
    public void removeNonDetTau() { 
    	  if (!hasTau()) return;
    	  while (true) {
    	  	  boolean canRemove = false;
    	  	  for (int i = 0; i<maxStates; i++)   // remove reflexive tau
            states[i] = EventState.remove(states[i],new EventState(Declaration.TAU,i));
	    	  BitSet tauOnly = new BitSet(maxStates);
	    	  for (int i = 1; i<maxStates; ++i) {
						if (EventState.hasOnlyTauAndAccept(states[i],alphabet)) {
							   tauOnly.set(i);
							   canRemove=true;
						}
	    	  }
				if (!canRemove) return;
				for (int i = 0; i<maxStates; ++i) {
					  if (!tauOnly.get(i))
					     states[i] = EventState.addNonDetTau(states[i],states,tauOnly);
	    	  }
	    	  int oldSize = maxStates;
	    	  reachable();
	    	  if (oldSize == maxStates) return;
    	  }
    }
	
	public void removeDetCycles(String action)  {
		int act = eventNo(action);
		if (act >=alphabet.length) return;
		for (int i =0; i<states.length; ++i)  {
			if (!EventState.hasNonDetEvent(states[i],act))
				states[i] = EventState.remove(states[i],new EventState(act,i));
		}
	}
	
	//check if has only single terminal accept state
	//also if no accept states - treats as safety property so that TRUE generates a null constraint
	public boolean isSafetyOnly()  {
		int terminalAcceptStates =0;
		int acceptStates = 0;
		for (int i = 0; i<maxStates; i++)  {
			if (EventState.isAccepting(states[i],alphabet)) {
			   ++acceptStates;
			   if (EventState.isTerminal(i,states[i]))
			   		++terminalAcceptStates;
			}
		}
		return (terminalAcceptStates==1 && acceptStates ==1) || acceptStates == 0 ;
	}
	
	//precondition - isSafetyOnly()
	//translates acceptState to ERROR state
	/*
	public void makeSafety()  {
		for (int i = 0; i<maxStates; i++)  {
			if (EventState.isAccepting(states[i],alphabet)) {
			   states[i] = new EventState(Declaration.TAU,Declaration.ERROR);
			}
		}
	}*/
	/* This version handles FALSE 13th June 2004 */
	public void makeSafety()  {
		int acceptState = -1;
		for (int i = 0; i<maxStates; i++)  {
			if (EventState.isAccepting(states[i],alphabet)) {
			  acceptState = i;
			  break;
			}
		}
		if (acceptState>=0) states[acceptState] = EventState.removeAccept(states[acceptState]);
		for (int i = 0; i<maxStates; i++)  {
			EventState.replaceWithError(states[i],acceptState);
		}
                Vector atsymbol = new Vector();
                atsymbol.add("@"+name); //remove acceptance label from alphabet
                conceal(atsymbol);
		reachable();
	}				
       	
	  //remove acceptance from states with only outgoing tau
    public void removeAcceptTau(){
      for (int i = 1; i<maxStates; ++i) {
				if (EventState.hasOnlyTauAndAccept(states[i],alphabet)) {
					 states[i] = EventState.removeAccept(states[i]);
				}
      }
    }
	
	public boolean hasERROR() {
		for (int i=0; i<maxStates; i++ )
            if (EventState.hasState(states[i],Declaration.ERROR))
				return true;
		return false;	
	}	

    
    public void prefixLabels(String prefix) {
        name = prefix+":"+name;
        for (int i=1; i<alphabet.length; i++) { // don't prefix tau
            String old = alphabet[i];
            alphabet[i]= prefix+"."+old;
        }
    }

    private boolean hasduplicates = false;

    public boolean relabelDuplicates() {return hasduplicates;}

    public void relabel(Relation oldtonew) {
        hasduplicates = false;
        if (oldtonew.isRelation())
            relational_relabel(oldtonew);
        else
            functional_relabel(oldtonew);
    }

    private void relational_relabel(Relation oldtonew) {
        Vector na = new Vector();
        Relation otoni = new Relation();  // index map old to additional
        na.setSize(alphabet.length);
        int new_index = alphabet.length;
        na.setElementAt(alphabet[0], 0);
        for (int i=1; i<alphabet.length; i++) {
            int prefix_end = -1;
            Object o = oldtonew.get(alphabet[i]);
            if (o!=null) {
                if (o instanceof String) {
                    na.setElementAt(o,i);
                } else { //one - to - many
                    Vector v = (Vector)o;
                    na.setElementAt(v.firstElement(),i);
                    for (int j=1;j<v.size();++j) {
                        na.addElement(v.elementAt(j));
                        otoni.put(new Integer(i),new Integer(new_index));
                        ++new_index;
                    }
                }
            } else if ((prefix_end=maximalPrefix(alphabet[i],oldtonew))>=0) { //is it prefix?
                String old_prefix = alphabet[i].substring(0,prefix_end);
                o = oldtonew.get(old_prefix);
                if (o!=null) {
                    if (o instanceof String) {
                        na.setElementAt(((String)o) + alphabet[i].substring(prefix_end),i);
                    } else { //one - to - many
                        Vector v = (Vector)o;
                        na.setElementAt(((String)v.firstElement()) + alphabet[i].substring(prefix_end),i);
                        for (int j=1;j<v.size();++j) {
                            na.addElement(((String)v.elementAt(j)) + alphabet[i].substring(prefix_end));
                            otoni.put(new Integer(i),new Integer(new_index));
                            ++new_index;
                        }
                    }
                } else {
                    na.setElementAt(alphabet[i],i); //not relabelled
                }
            } else {
                na.setElementAt(alphabet[i],i); //not relabelled
            }
        }
        //install new alphabet
        String aa[] = new String[na.size()];
        na.copyInto(aa);
        alphabet = aa;
        // add transitions
        addtransitions(otoni);
        checkDuplicates();
    }

    private void functional_relabel(Hashtable oldtonew) {
       for (int i=1; i<alphabet.length; i++) {  //don't relabel tau
            String newlabel = (String)oldtonew.get(alphabet[i]);
            if (newlabel!=null)
                 alphabet[i] = newlabel;
            else
                 alphabet[i] = prefixLabelReplace(i,oldtonew);
        }
        checkDuplicates();
    }

    private void checkDuplicates(){
        Hashtable duplicates=new Hashtable();
        for (int i=1; i<alphabet.length; i++) {
            if(duplicates.put(alphabet[i],alphabet[i])!=null) {
                hasduplicates = true;
                crunchDuplicates();
            }
        }
    }

    private void crunchDuplicates() {
        Hashtable newAlpha = new Hashtable();
        Hashtable oldtonew   = new Hashtable();
        int index =0;
        for(int i = 0; i<alphabet.length; i++) {
            if (newAlpha.containsKey(alphabet[i])) {
                oldtonew.put(new Integer(i), newAlpha.get(alphabet[i]));
            } else {
                newAlpha.put(alphabet[i],new Integer(index));
                oldtonew.put(new Integer(i), new Integer(index));
                index++;
            }
        }
        alphabet = new String[newAlpha.size()];
        Enumeration e = newAlpha.keys();
        while(e.hasMoreElements()) {
            String s = (String)e.nextElement();
            int i = ((Integer)newAlpha.get(s)).intValue();
            alphabet[i] = s;
        }
         // renumber transitions
        for (int i=0; i<states.length; i++)
            states[i] = EventState.renumberEvents(states[i],oldtonew);
     }
     
     //now used only for incremental minimization
     public Vector hide(Vector toShow) {
       Vector toHide = new Vector();
        for(int i = 1; i<alphabet.length; i++) {
            if (!contains(alphabet[i],toShow))
                toHide.addElement(alphabet[i]);
        }
        return toHide;
    }


    // hides every event but the ones in toShow
    public void expose(Vector toShow) {
        BitSet visible = new BitSet(alphabet.length);
        for(int i=1; i<alphabet.length ; ++i) {
           if (contains(alphabet[i],toShow)) visible.set(i);
        }
        visible.set(0);
        dohiding(visible);
    }

    public void conceal(Vector toHide) {
        BitSet visible = new BitSet(alphabet.length);
        for(int i=1; i<alphabet.length ; ++i) {
           if (!contains(alphabet[i],toHide)) visible.set(i);
        }
        visible.set(0);
        dohiding(visible);
    }
  
    private void dohiding(BitSet visible) {
        Integer tau = new Integer(Declaration.TAU);
        Hashtable oldtonew = new Hashtable();
        Vector newAlphabetVec = new Vector();
        int index =0;
        for(int i = 0; i<alphabet.length; i++) {
            if (!visible.get(i)) {
                oldtonew.put(new Integer(i), tau);
            } else {
                newAlphabetVec.addElement(alphabet[i]);
                oldtonew.put(new Integer(i), new Integer(index));
                index++;
            }
        }
        alphabet = new String[newAlphabetVec.size()];
        newAlphabetVec.copyInto(alphabet);
        // renumber transitions
        for (int i=0; i<states.length; i++)
            states[i] = EventState.renumberEvents(states[i],oldtonew);
     }

    static boolean contains(String action, Vector v) {
        Enumeration e = v.elements();
        while(e.hasMoreElements()) {
            String s = (String)e.nextElement();
            if(s.equals(action) || isPrefix(s,action)) return true;
        }
        return false;
    }

    // make every state have transitions to ERROR state
    // for actions not already declared from that state
	// properties can terminate in any state,however, we set no end state
	
	private boolean prop = false;
	
	public boolean isProperty() {
		return prop;
	}	
	
    public void makeProperty() {
		endseq = -9999;
		prop = true;
        for (int i=0; i<maxStates; i++ )
            states[i] = EventState.addTransToError(states[i],alphabet.length);
    }
	
    public void unMakeProperty() {
    	endseq = -9999;
    	prop = false;
         for (int i=0; i<maxStates; i++ )
             states[i] = EventState.removeTransToState(states[i],Declaration.ERROR);
     }
	

    public boolean isNonDeterministic() {
        for (int i=0; i<maxStates; i++ )
            if (EventState.hasNonDet(states[i])) return true;
        return false;
    }
    
    // make controller with respect to control actions cc
    
    public void makeController(Vector controls, LTSOutput output) {
        unControlled = new BitSet(alphabet.length);
        Controlled = new BitSet(alphabet.length);
        checkNotDetControls(unControlled);
        if (controls!=null){
            for(int i=1; i<alphabet.length ; ++i) {
               if (!contains(alphabet[i],controls)) unControlled.set(i); else Controlled.set(i);
            }
        } else {
            for(int i=1; i<alphabet.length ; ++i) unControlled.set(i);
        }
        unControlled.set(0);
        for (;;) {
            while (propagating(unControlled)) reachable();
            removeTransToError();
            if (!markDeadlockasError()) break;
            reachable();
        }    
        // removeTransToSelf();
        output.outln("Controller states: "+maxStates+" Stable states: " + stableStates());
    }
    
    public boolean hasControls(int sn) {
        if (Controlled == null) return false;
        if (sn>=maxStates) return false;
        return (EventState.hasEventInSet(states[sn], Controlled)||(states[sn]==null));
    }

    
    public boolean isStable(int sn) {
        if (unControlled == null) return false;
        if (sn>=maxStates) return false;
        return (!EventState.hasEventInSet(states[sn], unControlled));
    }
    
    public int stableStates() {
        int count = 0;
        for (int i=0; i<maxStates; i++ ) {
           if (isStable(i)) count++;
        }
        return count;
    }
    
    private void removeTransToError() {
        for (int i=0; i<maxStates; i++ ) {
           states[i] = EventState.removeTransToState(states[i],Declaration.ERROR);
        }
    }
    
    private void removeTransToSelf() {
        for (int i=0; i<maxStates; i++ ) {
           states[i] = EventState.removeTransToState(states[i],i);
        }
    }

    
    private void checkNotDetControls(BitSet unControlled) {
        for (int i=0; i<maxStates; i++ ) {
           EventState.checkNotDetControls(states[i], unControlled, alphabet);
        }
    }
    
    private boolean markDeadlockasError(){
        if (maxStates==1 && states[0]==null){
            Diagnostics.fatal("NO SAFE CONTROLLER");
            return false;       
        }
        BitSet isDeadlock = new BitSet(states.length);
        boolean done = false;
        for (int i=0; i<maxStates; i++) {
            if (states[i]==null) {
                isDeadlock.set(i); done = true;
            }
        }
        for (int i=0; i<maxStates; i++ ) {
           EventState.setStateError(states[i], isDeadlock);
        }
        return done;
    }
    
    private boolean propagating(BitSet unControlled) {
        BitSet isErr = new BitSet(states.length);
        boolean doing = false;
        for (int i=0; i<maxStates; i++ ) {
             if (EventState.hasUnControlledTransToError(states[i], unControlled))
             {isErr.set(i); doing = true;}
        }
        for (int i=0; i<maxStates; i++ ) {
           EventState.setStateError(states[i], isErr);
        }
        if (maxStates==1){
            BitSet bb = new BitSet(alphabet.length);
            EventState.hasEvents(states[0], bb);
            for (int j=1; j<alphabet.length; j++) {
                if (bb.get(j) && unControlled.get(j)) {
                    Diagnostics.fatal("NO SAFE CONTROLLER");
                    return false;
                }
            }
            return false;
        } else
            return doing;
    }

    //output LTS in aldebaran format
    public void printAUT(PrintStream out) {    
      out.print("des(0,"+ntransitions()+","+maxStates+")\n");
      for (int i=0; i<states.length; i++)
          EventState.printAUT(states[i],i,alphabet,out);
    }

    public CompactState myclone() {
        CompactState m = new CompactState();
        m.name = name;
        m.endseq = endseq;
		m.prop = prop;
        m.alphabet = new String[alphabet.length];
        for (int i=0; i<alphabet.length; i++) m.alphabet[i]=alphabet[i];
        m.maxStates = maxStates;
        m.states = new EventState[maxStates];
        for (int i=0;i<maxStates; i++)
            m.states[i] = EventState.union(m.states[i],states[i]);
        return m;
    }

    public int ntransitions() {
        int count = 0;
        for (int i=0; i<states.length; i++)
            count += EventState.count(states[i]);
        return count;
    }

    public boolean hasTau() {
        for (int i = 0; i<states.length; ++i) {
            if (EventState.hasTau(states[i])) return true;
        }
        return false;
    }


    /* ------------------------------------------------------------*/
    private String prefixLabelReplace(int i, Hashtable oldtonew) {
        int prefix_end = maximalPrefix(alphabet[i],oldtonew);
        if (prefix_end<0) return alphabet[i];
        String old_prefix = alphabet[i].substring(0,prefix_end);
        String new_prefix = (String)oldtonew.get(old_prefix);
        if (new_prefix==null) return alphabet[i];
        return new_prefix + alphabet[i].substring(prefix_end);
    }

    private int maximalPrefix(String s, Hashtable oldtonew) {
        int prefix_end = s.lastIndexOf('.');
        if (prefix_end<0) return prefix_end;
        if (oldtonew.containsKey(s.substring(0,prefix_end)))
            return prefix_end;
        else
            return maximalPrefix(s.substring(0,prefix_end),oldtonew);
    }

    static private boolean isPrefix(String prefix, String s) {
        int prefix_end = s.lastIndexOf('.');
        if (prefix_end<0) return false;
        if (prefix.equals(s.substring(0,prefix_end)))
            return true;
        else
            return isPrefix(prefix,s.substring(0,prefix_end));
    }

    /* ------------------------------------------------------------*/

    public boolean isErrorTrace(Vector trace) {
        boolean hasError = false;
        for (int i=0; i<maxStates && !hasError; i++ )
            if (EventState.hasState(states[i],Declaration.ERROR))
                hasError=true;
        if (!hasError) return false;
        return isTrace(trace,0,0);
    }

    private boolean isTrace(Vector v,int index, int start) {
        if (index<v.size()) {
            String ename = (String) v.elementAt(index);
            int eno = eventNo(ename);
            if (eno<alphabet.length) {   // this event is in the alphabet
                if (EventState.hasEvent(states[start],eno)) {
                    int n[] = EventState.nextState(states[start],eno);
                    for (int i=0; i<n.length; ++i) // try each nondet path
                        if (isTrace(v,index+1,n[i])) return true;
                    return false;
                } else if (eno!=Declaration.TAU)  // ignore taus
                    return false;
            }
            return isTrace(v,index+1,start);
        } else
            return (start == Declaration.ERROR);
    }

    private int eventNo(String ename) {
        int i = 0;
        while (i<alphabet.length && !ename.equals(alphabet[i])) i++;
        return i;
    }

    /* ---------------------------------------------------------------*/

    /* addAcess extends the alphabet by creating a new copy of the alphabet
       for each prefix string in pset. Each transition is replicated acording to
       the number of prefixes and renumbered with the new action number.
    */

    public void addAccess(Vector pset) {
        int n = pset.size();
        if (n==0) return;
        String s = "{";
        CompactState machs[] = new CompactState[n];
        Enumeration e =  pset.elements();
        int i =0;
        while (e.hasMoreElements()) {
            String prefix = (String)e.nextElement();
            s = s + prefix;
            machs[i] = myclone();
            machs[i].prefixLabels(prefix);
            i++;
            if (i<n) s = s+",";
        }
        //new name
        name = s+"}::"+name;
        //new alphabet
        int alphaN = alphabet.length - 1;
        alphabet = new String[(alphaN*n) +1];
        alphabet[0] = "tau";
        for (int j = 0; j<n ; j++) {
            for (int k = 1; k<machs[j].alphabet.length; k++) {
                alphabet[alphaN*j+k] = machs[j].alphabet[k];
            }
        }
        //additional transitions
        for(int j = 1; j<n; j++) {
            for(int k = 0; k<maxStates; k++) {
                EventState.offsetEvents(machs[j].states[k],alphaN*j);
                states[k] = EventState.union(states[k],machs[j].states[k]);
            }
        }
    }

  /* ---------------------------------------------------------------*/

    private void addtransitions(Relation oni) {
        for (int i=0; i<states.length; i++) {
            EventState ns = EventState.newTransitions(states[i],oni);
            if (ns!=null)
                states[i] = EventState.union(states[i],ns);
        }
    }

  /* ---------------------------------------------------------------*/

    public boolean hasLabel(String label) {
        for (int i = 0; i<alphabet.length ; ++i)
            if (label.equals(alphabet[i])) return true;
        return false;
    }
    
    public boolean usesLabel(String label) {
        if (!hasLabel(label)) return false;
        int en = eventNo(label);
        for (int i = 0; i<states.length; ++i) {
            if (EventState.hasEvent(states[i],en)) return true;
        }
        return false;
    }
    
  /* ---------------------------------------------------------------*/

    public boolean isSequential() {
        return endseq >=0;
    }
    
    public boolean isEnd() {
        return maxStates == 1 && endseq == 0;
    }
    
  /*----------------------------------------------------------------*/
  
   public static CompactState sequentialCompose(Vector seqs) {
   		if (seqs==null) return null;
   		if (seqs.size()==0) return null;
   		if (seqs.size()==1) return (CompactState)seqs.elementAt(0);
   		CompactState machines[] = new CompactState[seqs.size()];
   		machines = (CompactState[])seqs.toArray(machines);
   		CompactState newMachine =  new CompactState();
   		newMachine.alphabet = sharedAlphabet(machines);
   		newMachine.maxStates = seqSize(machines);
   		newMachine.states = new EventState[newMachine.maxStates];
   		int offset = 0;
   		for (int i=0; i<machines.length; i++ ) {
   			boolean last = (i==(machines.length-1));
   			copyOffset(offset,newMachine.states,machines[i],last);
   			if (last) 	newMachine.endseq = machines[i].endseq+offset;	
   			offset +=machines[i].states.length;
   		}			 				
   	  return newMachine;
   }
   
   /*----------------------------------------------------------------*/
  
   public void expandSequential(Hashtable inserts) {
   	  int ninserts = inserts.size();
   	  CompactState machines[] = new CompactState[ninserts+1];
   	  int insertAt[] = new int[ninserts+1];
   	  machines[0] = this;
   	  int index = 1;
   	  Enumeration e = inserts.keys();
   	  while(e.hasMoreElements()) {
   	  	  Integer ii = (Integer)e.nextElement();
   	  	  CompactState m = (CompactState) inserts.get(ii);
   	  	  machines[index] = m;
   	  	  insertAt[index] = ii.intValue();
   	  	  ++index;
   	  }
/*
   	  System.out.println("Offsets ");
   	  for (int i=0; i<machines.length; i++) {
   	  	  machines[i].printAUT(System.out);
   	  	  System.out.println("endseq "+machines[i].endseq);
   	  }
*/
   		//newalphabet
   		alphabet = sharedAlphabet(machines);
   		//copy inserted machines
   		for (int i=1; i<machines.length; ++i) {
        int offset = insertAt[i];
   			for (int j = 0; j<machines[i].states.length; ++j) {
   				states[offset+j] = machines[i].states[j];
   			}
   		}
   }


  /*
  *   compute size of sequential composite
  */
  private static int seqSize(CompactState[] sm) {
  	 int length = 0;
  	 for (int i=0; i<sm.length; i++ ) 
  	 	    length+=sm[i].states.length;
  	 return length;
  }
  
  private static void copyOffset(int offset, EventState[] dest, CompactState m, boolean last ) {
  	 for(int i = 0; i<m.states.length; i++) {
  	 	  if (!last)
  	 	    dest[i+offset] = EventState.offsetSeq(offset,m.endseq,m.maxStates+offset,m.states[i]);
  	 	  else
  	 	  	dest[i+offset] = EventState.offsetSeq(offset,m.endseq,m.endseq+offset,m.states[i]);
  	 }
  }
  	 	    
  public void offsetSeq(int offset, int finish) {
     for (int i=0; i<states.length; i++) {
         EventState.offsetSeq(offset,endseq,finish,states[i]);
     }
  }

	/* 
	* create shared alphabet for machines & renumber acording to that alphabet
	*/
	private static String [] sharedAlphabet(CompactState[] sm) {
		  // set up shared alphabet structure
      Counter newLabel    = new Counter(0);
      Hashtable actionMap = new Hashtable();
      for (int i=0; i<sm.length; i++ ) {
          for (int j = 0; j < sm[i].alphabet.length; j++) {
              if (!actionMap.containsKey(sm[i].alphabet[j])) {
                  actionMap.put(sm[i].alphabet[j],newLabel.label());
              } 
          }
      }
      // copy into alphabet array
      String [] actionName = new String[actionMap.size()];
      Enumeration e = actionMap.keys();
      while (e.hasMoreElements()) {
          String s = (String)e.nextElement();
          int index =((Integer)actionMap.get(s)).intValue();
          actionName[index] =s;
      }
      // renumber all transitions with new action numbers
      for (int i=0; i<sm.length; i++ ) {
          for(int j=0; j<sm[i].maxStates;j++) {
              EventState p = sm[i].states[j];
              while(p!=null) {
                  EventState tr = p;
                  tr.event = ((Integer)actionMap.get(sm[i].alphabet[tr.event])).intValue();
                  while (tr.nondet!=null) {
                      tr.nondet.event = tr.event;
                      tr = tr.nondet;
                  }
                  p=p.list;
              }
          }
      }
      return actionName;
          
	}
	
	/** implementation of Automata interface **/
	
	private byte[] encode(int state) {
		 byte[] code = new byte[4];
		 for(int i=0; i<4; ++i) {
		  	   code[i] |= (byte)state;
		  	   state = state >>>8;
		  }
		  return code;
	}
				
  private int decode( byte[] code){
  	  	 int x =0;
		 for(int i=3; i>=0; --i) {
		  	   x |= (int)(code[i])& 0xFF;
		  	   if (i>0) x = x << 8;
		  }
		  return x;

  }
  
	public String[] getAlphabet() {return alphabet;}
	
	public Vector getAlphabetV() {
		  Vector v = new Vector(alphabet.length-1);
		  for (int i=1; i<alphabet.length; ++i)
		  		v.add(alphabet[i]);
		  return v;
	}
	
	public MyList getTransitions(byte[] fromState) {
		MyList tr = new MyList();
		int state;
		if (fromState == null)
			state = Declaration.ERROR;
	  else
	     state = decode(fromState);
		if (state<0 ||state>=maxStates) return tr;
		if (states[(int)state]!=null)
		for(Enumeration e = states[state].elements(); e.hasMoreElements();) {
                EventState t = (EventState)e.nextElement();
                tr.add(state,encode(t.next),t.event);
		}
		return tr;
	}
	
	public String getViolatedProperty() {return null;}

	//returns shortest trace to  state (vector of Strings)
	public Vector getTraceToState(byte[] from, byte[] to){
		EventState trace = new EventState(0,0);
    int result = EventState.search(trace,states,decode(from),decode(to),-123456);
    return EventState.getPath(trace.path,alphabet);
	}

//return the number of the END state
	public boolean END(byte[] state) {
		 return decode(state) == endseq;
	}
	
	//return whether or not state is accepting
    public boolean isAccepting(byte[] state)  {
		return isAccepting(decode(state));
    }
	
	//return the number of the START state
	public byte[] START() {
		 return encode(0);
	}

  //set the Stack Checker for partial order reduction
	public void setStackChecker(StackCheck s){} // null operation

  //returns true if partial order reduction
	public boolean isPartialOrder(){return false;}
	
	//diable partial order
	public void disablePartialOrder() {}
	
	//enable partial order
	public void enablePartialOrder() {}

	
	/*-------------------------------------------------------------*/
	// is state accepting
	public boolean isAccepting(int n) {
		  if (n<0 || n>=maxStates) return false;
		  return EventState.isAccepting(states[n],alphabet);
	}
	
	public BitSet accepting() {
		  BitSet b = new BitSet();
		  for (int i = 0; i<maxStates; ++i) 
		  	   if (isAccepting(i)) b.set(i);
		  	return b;
	}

}