package automata.nsa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Represents a tree(state) in a DNA. 
 * For convenience of the construction, we switch between two different representations of the tree.
 * One using a linked tree, and the other using the standard two array representation.
 */
public class DNATree {
	
    /*
     * The underlying NSA associated with the DNA
	 */
	private NSA nsa;
	
	/*
	 * The root of the linked tree representation
	 */
	private DNATreeNode root;
	
	/*
	 * Array representing the tree. Undefined values are Integer.MAX_VALUE.
	 */
	private int[] tree; //active only from index 1
	
	/*
	 * Array representing the states. Undefined values are Integer.MAX_VALUE.
	 */
	private int[] statesMap;
	
	/*
	 * Node Annotation. Templars denoted by -1, and phi by -2. Undefined values are Integer.MAX_VALUE.
	 */
	private int[] annotations;
	
	/*
	 * The index of the tree in the DNA. Used by NSA.convertToDNA
	 */
	private int treeIndex = -1;
	
	public int getTreeIndex() {
		return treeIndex; 
	}

	public void setTreeIndex(int treeIndex) {
		this.treeIndex = treeIndex;
	}

	
	public String toString()
	{
		String treeOutput, statesOutput, annotationOutput;
		
		statesOutput = "[";
		for(int i = 0; i < statesMap.length;i++)
		{
			statesOutput += statesMap[i] < tree.length ? Integer.toString(statesMap[i]) : "$";
			if(i < statesMap.length - 1)
			{
				statesOutput += " ";
			}
		}
		statesOutput += "]";
		
		treeOutput = "[0";
		for(int i = 2; i < nsa.getNPrime();i++) 
		{
				treeOutput += " ";
				
				if(tree[i] < tree.length)
					treeOutput += Integer.toString(tree[i]);	
				else
					treeOutput += '0';
		}
		treeOutput += "]";
		
		
		annotationOutput = "[";
		for(int i = 0; i < annotations.length && annotations[i] < tree.length;i++)
		{	
				if(i > 0)
				{
					 annotationOutput += " ";
				}
				
				if(annotations[i] >= 0)
					annotationOutput += Integer.toString(annotations[i]);
				else if(annotations[i] == -1)
					annotationOutput += '+';
				else if(annotations[i] == -2)
					annotationOutput += 'f';
					
		}
		annotationOutput += "]";
		
		return treeOutput + ',' + statesOutput + ',' + annotationOutput;
	}
	
	/*
	 * Generates Q0 of the DNA related to the given NSA. With non-fully initialized array representation.
	 */
	public DNATree(NSA nsa)
	{
		this.nsa = nsa;
		
		root = new DNATreeNode(nsa.getStartStates(), 0, null, 0,null);
		
		tree = new int[3*nsa.getNPrime()];
		annotations = new int[3*nsa.getNPrime()];
		statesMap = new int[nsa.getStateCount()];
		
		Arrays.fill(tree, Integer.MAX_VALUE);
		Arrays.fill(statesMap, Integer.MAX_VALUE);
		Arrays.fill(annotations, Integer.MAX_VALUE);
		
		for(Integer state : root.states)
		{
			statesMap[state] = 0;
		}
		
		annotations[0] = 0;
	}
	
	/*
	 * Generates the transition(tree and number k) from this tree with input character sigma
	 * (sigma can be a string)
	 */
	public DNATransition transition(String sigma)
	{
		DNATree ret = new DNATree(this.nsa); //Generate a new tree
		ret.tree = this.tree.clone();      
		ret.statesMap = this.statesMap.clone(); //Clone the array representation
		ret.annotations = this.annotations.clone();
	    int k = ret.applyTransition(sigma);     //Use the private helper function applyTransition
		
		return new DNATransition(this, ret, k);
	}
	
	private int applyTransition(String sigma)
	{
		int[] availableAnnotations = new int[this.annotations.length];
		Arrays.fill(availableAnnotations,Integer.MAX_VALUE);
		
		this.updateTree();  
		root.recSpawn(sigma);  // Perform spawn stage
		this.fixSeniority(availableAnnotations);   
		
		return this.fixUnique(availableAnnotations);  //Perform uniqueness and packing stage and return the number k.
	}
	
	/*
	 * Updates the linked tree representation given an updated array representation
	 */
	private void updateTree()
	{
		DNATreeNode[] treeArray = new DNATreeNode[tree.length];
		
		for(int i = 0; i < tree.length;i++)
		{
			if(tree[i] < tree.length|| i == 0)
			{
				treeArray[i] = new DNATreeNode(null,i,null,annotations[i],null);	
			}
			if(tree[i] < tree.length)
			{
				treeArray[tree[i]].children.add(treeArray[i]);
				
				treeArray[i].parent = treeArray[tree[i]];
				
				treeArray[i].excluded.addAll(treeArray[tree[i]].excluded);
				
				if(annotations[tree[i]] == -1)                                    //Setting excluded
				{
					treeArray[i].excluded.add(treeArray[tree[tree[i]]].annotation);	
				}
			}
		}
		
		for(int i = 0; i < statesMap.length;i++)
		{
			if(statesMap[i] < tree.length)
			{
				int node = statesMap[i];
				while(node < tree.length)
				{
					treeArray[node].states.add(i);
					node = tree[node];
				}
			}
		}
		
		
		this.root = treeArray[0];
	}
	
	/*
	 * Perform seniority fix stage
	 */
	private void fixSeniority(int[] availableAnnotations)
	{
		Arrays.fill(tree, Integer.MAX_VALUE);
		Arrays.fill(statesMap, Integer.MAX_VALUE);
		Arrays.fill(this.annotations, Integer.MAX_VALUE);
		Arrays.fill(availableAnnotations, Integer.MAX_VALUE);
		
		this.root.recBuildTreeArray(this.tree, this.annotations, availableAnnotations);
		
		for(int state = 0; state < statesMap.length; state++)
		{
			DNATreeNode node = this.root;
			
			while(node != null)
			{
				DNATreeNode child = null;
				node.children.sort(new Comparator<DNATreeNode>() {
					@Override
					public int compare(DNATreeNode o1, DNATreeNode o2) {	
					if(o1.annotation == -1)      {return 1;}
					
					else if(o2.annotation == -1) { return -1;}
					else                         {return o1.index - o2.index;}	
					
					}
				});;
										
				for(DNATreeNode dtn : node.children)
				{
					if(dtn.states.contains(state))
					{
						child = dtn;
						break;
					}
				}
				
				if(child == null)
				{
					if(node.states.contains(state)) 
						statesMap[state] = node.index;	
					node = null;
				}
				else
				{
					node = child;
				}
				
			}
		}
	}

	/*
	 * Performs the uniqueness and packing fixing stages
	 */
	private int fixUnique(int[] availableAnnotations)
	{
		boolean[] hasUnique = new boolean[tree.length];
		boolean[] isDead = new boolean[tree.length];
		
		boolean[] isEmpty = new boolean[tree.length];
		boolean[] hasTemplarChild = new boolean[tree.length];
		
		Arrays.fill(hasUnique, false);
		Arrays.fill(isDead, false);
		
		Arrays.fill(isEmpty, true);
		Arrays.fill(hasTemplarChild, false);
		
		int k = Integer.MAX_VALUE;
		
		for(int i = 0; i < statesMap.length;i++)
		{
			if(statesMap[i] < tree.length)
			{
				hasUnique[statesMap[i]] = true;                         //Find all nodes with no uniqueness
			}
		}
		
		for(int i = isEmpty.length-1; i >= 0; i--)                    //Find all non empty nodes
		{
			if(hasUnique[i])
				isEmpty[i] = false;
			
			if(tree[i] < tree.length && !isEmpty[i])
			{	
				isEmpty[tree[i]] = false;
			}
		}
		
		for(int i = 0 ;i < tree.length; i++)                     //Find all nodes with a non empty templar child
		{
			if(tree[i] < tree.length)
			{
				if(annotations[i] == -1 && !isEmpty[i])
					hasTemplarChild[tree[i]] = true;
			}
		}
		
		
		for(int i = 0; i < annotations.length; i ++)             //Update annotation if becoming green
		{
			if(!hasUnique[i] && !hasTemplarChild[i] && annotations[i] >= 0) 
				this.annotations[i] = availableAnnotations[i];
		}
		
		for(int i = 1; i < tree.length; i++)
		{
			if(tree[i] < tree.length)
			{
				if(isDead[tree[i]])                      //Mark all nodes whose father has no unique as dead and adjust pointers
				{
					isDead[i] = true; 
					tree[i] = tree[tree[i]];
				}
				
				else if(!hasUnique[tree[i]] && !hasTemplarChild[tree[i]] && annotations[tree[i]] >= 0)
				{
					isDead[i] = true; //Parent(tree[i]) loses this child, since he's gone green
				}	
			}
		}
		
		for(int i = 0; i < statesMap.length;i++)
		{
			if(statesMap[i] < tree.length && isDead[statesMap[i]])                 //Fix the state map
			{
				statesMap[i] = tree[statesMap[i]];
			}
			
		}
		
		Arrays.fill(isEmpty, true);
		
		for(int i = 0; i < statesMap.length;i++)
		{
			if(statesMap[i] < tree.length)
			{
				isEmpty[statesMap[i]] = false;                         //Find all nodes with no uniqueness
			}
		}
		
		for(int i = isEmpty.length-1; i >= 0; i--)                    //Find all non empty nodes
		{	
			if(tree[i] < tree.length && !isEmpty[i])
			{	
				isEmpty[tree[i]] = false;
			}
		}
		
		for(int i = 1; i < tree.length;i++)                                //Remove empty nodes
		{
			if(isEmpty[i])
			{
				tree[i] = Integer.MAX_VALUE;
			}
			
		}
		
		
		for(int i = 0; i < isEmpty.length && k == Integer.MAX_VALUE;i++)     //Find k from g and b
		{
			if(isEmpty[i])
			{
				k = 2*i;
			}
			
			else if((!hasUnique[i] && !hasTemplarChild[i] && annotations[i] >= 0) || annotations[i] == -2)
			{	
				k = 2*i + 1;
			}
		}
		
		
		
		//--------------------------------------------------End of uniqueness, beginning partition fix---------------------------------------------
		
		int[] newTreeIndices = new int[tree.length];
		int count = 1;
		
		for(int i = 1;i < tree.length;i++)
		{
			if(tree[i] < tree.length)
			{
				newTreeIndices[i] = count;
				tree[count] = newTreeIndices[tree[i]];
				annotations[count] = annotations[i];
				count++;
			}
		}
		
		for(int i = 0;i < statesMap.length;i++)
		{
			if(statesMap[i] < tree.length)
			{
				statesMap[i] = newTreeIndices[statesMap[i]];
			}
		}
		
		
		Arrays.fill(tree,count,tree.length,Integer.MAX_VALUE);
		Arrays.fill(annotations,count,tree.length,Integer.MAX_VALUE);		
		
		
		return k;
	}
	
	
	/*
	 * A nested class for a node of the linked tree representation
	 */
	private class DNATreeNode
	{
		private List<DNATreeNode> children;
		private DNATreeNode parent; 
		
		private Set<Integer> states;
		private int index;  //Index in the array representation
		
		private Set<Integer> excluded; //Annotations forbidden for the node
		private int annotation;
		
		
		private void applyTransition(String sigma)
		{
			states = nsa.transitionFunction(states,this.excluded ,sigma);
		}
		
		private DNATreeNode(Set<Integer> states, int index, Set<Integer> excluded, int annotation, DNATreeNode parent)
		{
			if(states == null)
			{
				this.states = new HashSet<Integer>();
			}
			else
			{
				this.states = new HashSet<Integer>(states);
			}
			
			if(excluded == null)
			{
				this.excluded = new HashSet<Integer>();
			}
			else
			{
				this.excluded = new HashSet<Integer>(excluded);
			}
			
			this.index = index;
			this.children = new ArrayList<DNATreeNode>();
			
			this.annotation = annotation;
			
			this.parent = parent;
		}
	
		/*
		 * Recursively performs the spawn stage on the tree
		 */
		private void recSpawn(String sigma)
		{
			
			if(!children.isEmpty())
			{
				for(DNATreeNode node : children)
				{
					node.recSpawn(sigma);
				}
			}
			
			this.applyTransition(sigma);
				
			
			if(this.annotation == -1)            //Special's child
			{
				Set<Integer> available = this.availableAnnotations();
				available.remove(this.parent.annotation);
				int newAnnotation;
				
				if(available.isEmpty())
					newAnnotation = -2; //-2 means phi
				else
					newAnnotation = Collections.min(available);
				
				DNATreeNode spawned = new DNATreeNode(
						this.states, this.index + nsa.getNPrime(),this.excluded,newAnnotation,this );
				spawned.excluded.add(this.parent.annotation);
				this.children.add(spawned);
				
			}
			
			else if(this.annotation >= 0)
			{
				if(this.children.isEmpty()) //Spawn: Special
				{
					DNATreeNode specialSpawned = new DNATreeNode(this.states, this.index + 2*nsa.getNPrime(),this.excluded,-1 ,this);
					this.children.add(specialSpawned);
				}
				
				DNATreeNode spawned = new DNATreeNode(
						this.states, this.index + nsa.getNPrime(),this.excluded, this.nextAvailableAnnotation(),this);
				nsa.retainGreen(spawned.states, this.annotation);
				this.children.add(spawned);
				
			}
				
		
		}
		
		/*
		 * Recursively generates the corresponding array for the tree. (Does not update stateMap array)
		 */
		private void recBuildTreeArray(int[] tree, int[] annotations, int[] availableAnnotations) 
		{
			annotations[this.index] = this.annotation;
			availableAnnotations[this.index] = this.nextAvailableAnnotation();
			
			for(DNATreeNode child : this.children)
			{
				tree[child.index] = this.index;
				child.recBuildTreeArray(tree,annotations, availableAnnotations);
			}
		}	
		
		/*
		 * Annotations not forbidden for the node
		 */
		private Set<Integer> availableAnnotations()
		{
			Set<Integer> available = new HashSet<Integer>();
			
			for(int i = 0; i < nsa.getAnnotationCount();i++)
			{
				available.add(i);
			}
			
			available.removeAll(this.excluded);
			
			return available;
		}
		
		/*
		 * Next available annotation, in case node went green
		 */
		private int nextAvailableAnnotation() 
		{
			Set<Integer> available = this.availableAnnotations();
						
			if(available.isEmpty())
				return Integer.MAX_VALUE;
			
			else
			{
				for(int i = this.annotation + 1; i < nsa.getAnnotationCount() * 2 + 1; i++)
				{
					int anot = i % nsa.getAnnotationCount();
					
					if(available.contains(anot))
						return anot;
				}
			}
			
			return Integer.MAX_VALUE;
		}
		
		//Used for debugging
		public String toString()
		{
			return this.states.toString();
		}
		
	}
	
	/*
	 * A data container class for representing transitions
	 */
	public static class DNATransition
	{
		public DNATree originalState;
		public DNATree resultState;
		public int k;
		
		public DNATransition(DNATree originalState, DNATree resultState, int k) {
			this.resultState = resultState;
			this.originalState = originalState;
			this.k = k;
		}
		
	}
	

}
