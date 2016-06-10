package automata.nsa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import automata.nsa.DNATree.DNATransition;


/*
 *  Class representing an NSA
 */
public class NSA {

	
	private int stateCount;
	private int annotationCount;
	
	
	private Set<Integer> startStates;
	
	private List<Set<Integer>> redSets;
	private List<Set<Integer>> greenSets;
	
	/*
	 *  Describes the transition function of the NSA, such that delta(q,sigma)
	 *  is contained in transitionMap[sigma][q]
	 */
	private Map<String,List<Set<Integer>>> transitionMap;

	/*
	 * A constructor for the NSA, receives a path to a file containing a graphviz format and generates
	 * an NSA object. 
	 */
	public NSA(String path) throws NumberFormatException, IOException {
		stateCount = 0;
		
		this.greenSets = new ArrayList<Set<Integer>>();
		this.redSets = new ArrayList<Set<Integer>>();
			
		startStates = new HashSet<Integer>();
		
		transitionMap = new HashMap<String, List<Set<Integer>>>();
		
		BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(path)));
		
		Pattern pstate_00 = Pattern.compile("(\\s+)q(\\d+) \\[label=\"q(\\d+)\"\\]"); //NOT starting, NOT accepting
		//Pattern pstate_01 = Pattern.compile("(\\s+)q(\\d+) \\[label=\"q(\\d+)\\$\"\\]"); //NOT starting, accepting
		Pattern pstate_10 = Pattern.compile("(\\s+)q(\\d+) \\[label=\"\\*q(\\d+)\"\\]"); //starting, NOT accepting
		Pattern pstate_11 = Pattern.compile("(\\s+)q(\\d+) \\[label=\"\\*q(\\d+)\\$\"\\]"); //starting, accepting
		Pattern ptransition = Pattern.compile("(\\s+)q(\\d+) -> q(\\d+) \\[label=(\\S+)\\]");
		Pattern pSets = Pattern.compile("(\\S)_(\\d+) (.*)");
		Matcher m; // get a matcher object
		
		int fromState;
		int toState;
		String label;
		String line;
		while((line = bufferedReader.readLine()) != null) {
			
			if((m = ptransition.matcher(line)).matches()) { //Transition line
				fromState = Integer.parseInt(m.group(2));
				toState = Integer.parseInt(m.group(3));
				label = m.group(4);
				
				List<Set<Integer>> labelList;
				
				if(!transitionMap.containsKey(label))
				{
					labelList = new ArrayList<Set<Integer>>();
					
					for(int i = 0; i < stateCount;i++)
					{
						labelList.add(i, new HashSet<Integer>());
					}
					
					transitionMap.put(label, labelList);
				}
				
				else
				{
					labelList = transitionMap.get(label);
				}
				
				
				Set<Integer> tmp0 = labelList.get(fromState);
				tmp0.add(toState);
			
				
			}
			
			
			else if((m = pSets.matcher(line)).matches())
			{
				String color = m.group(1);
				int index = Integer.parseInt(m.group(2));
				String stateString = m.group(3);
				String[] states = stateString.split(" ");
				
				Set<Integer> theSet = new HashSet<Integer>();
				
				if(color.equals("R"))
					redSets.add(index, theSet);
				else if (color.equals("G"))
					greenSets.add(index, theSet);
				else
					System.out.println("Error:Bad input format");
				
				
				
				if (!stateString.equals("")) {
					for (String state : states) {
						theSet.add(Integer.parseInt(state));
					}
				}
				
			
				
			}
			
			else { //State line	
				
				if((m = pstate_00.matcher(line)).matches()) { } //NOT starting, NOT accepting
				
					
				else if((m = pstate_10.matcher(line)).matches()) //starting, NOT accepting
					startStates.add(stateCount);
				else if((m = pstate_11.matcher(line)).matches()) { //starting, accepting
					
					startStates.add(stateCount);
				} else {continue;} //Cannot parse
				
				stateCount++;
			}
		}
		bufferedReader.close();
		
		
		this.annotationCount = this.greenSets.size();	
		
	}
	
	/*
	 * Applies the transition function of the NSA on a set. Removing forbidden red sets.
	 */
	public Set<Integer> transitionFunction(Set<Integer> states,Set<Integer> excludedAnnotations ,String sigma)
	{
		List<Set<Integer>> transitions;
		Set<Integer> result = new HashSet<Integer>();
		
		if(transitionMap.containsKey(sigma))
		{
			transitions = transitionMap.get(sigma);
		}
		else
		{
			System.out.println("Critical error: wrong char");
			transitions = null;
		}
		
		for(Integer state: states)
		{
			result.addAll(transitions.get(state));
		}
		
		for(Integer excluded : excludedAnnotations)
		{
			result.removeAll(redSets.get(excluded));
		}
		
		return result;
	}
	
	/*
	 * Removes non accepting states from the supplied set
	 */
	public void retainGreen(Set<Integer> states, int annotation)
	{
		states.retainAll(this.greenSets.get(annotation));
	}
	
	
	public Set<Integer> getStartStates() {
		return new HashSet<Integer>(startStates);
	}
	
	public int getStateCount() {
		return stateCount;
	}
	
	public int getAnnotationCount() {return annotationCount;}
	
	public int getNPrime() {return this.stateCount * (this.annotationCount+1);}
	
	/*
	 * Returns the conversion of the NSA to a string containing the DNA in graphviz format.
	 */
	public String convertToDNA()
	{
		SortedSet<DNATree> states = new TreeSet<DNATree>(new Comparator<DNATree>() {
			@Override
			public int compare(DNATree o1, DNATree o2) {
				String startString = (new DNATree(NSA.this)).toString();
				String o1String = o1.toString();
				String o2String = o2.toString();
				
				if(o1String.equals(startString) && o2String.equals(startString))
					return 0;
				else if(o1String.equals(startString))
					return -1;
				else if(o2String.equals(startString))
					return 1;
				else
					return o1String.toString().compareTo(o2String.toString());
			}
		});
		
		recGenerateDNA(states, new DNATree(this));
		
		String stateString = "";
		String transitionString = "";
		
		int index = 0;
		for(DNATree state : states)
		{
			stateString += String.format("		Q%d [label=\"%s\"]" + System.lineSeparator(), index, state.toString());
			state.setTreeIndex(index);
			index++;
		}
		
		SortedSet<String> outputTransitions = new TreeSet<String>();
		for(DNATree state : states)
		{
			for(String c : transitionMap.keySet())
			{
				DNATransition trans = state.transition(c);
				String label = c + "[" + trans.k + "]";
				
				outputTransitions.add(String.format("				Q%d -> Q%d [label=\"%s\"]"+ System.lineSeparator(),
						trans.originalState.getTreeIndex(),states.headSet(trans.resultState).size(), label)); 
			}	
		}
		
		for(String transition : outputTransitions)
		{
			transitionString += transition;
		}
		
		return stateString+transitionString;
	}
	
	/*
	 * A recursive helper method for generating the DNA
	 */
	private void recGenerateDNA(SortedSet<DNATree> states, DNATree tree)
	{
		if(!states.contains(tree))
		{
			states.add(tree);
			
			for(String c : transitionMap.keySet())
			{
				recGenerateDNA(states, tree.transition(c).resultState);
			}
			
		}
	}
	
	
	
}
