
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import com.sun.management.ThreadMXBean;

import JaCoP.core.*;
import JaCoP.constraints.*;
import JaCoP.search.*;


public class TabuSearch {
	
	private Store store;
	private IntVar[] vars;
	private IntVar[] bestSolution;
	private IntDomain[] domains;
	private TabuList tabuList;
	
	int maxIteration = 100;
	int maxIterationWhithoutBetterSolution = 5;
	int tabuListMaxLength = 50;

	public TabuSearch(Store store, IntVar[] vars) {
		this.store = store;
		this.vars = vars;
		this.domains = new IntDomain[vars.length];
		this.bestSolution = new IntVar[vars.length];
		tabuList = new TabuList(tabuListMaxLength);
	}
	
	
	public void search() {
		
		// Saving domains
		for(int i = 0; i < vars.length; i++) {
			domains[i] = (IntDomain)vars[i].dom().clone();
		}
		
		// Step 1 : generate a random solution
		vars = this.generate_random();
		
		
		// Step 2 : save the best known solution
		bestSolution = this.cloneIntVar(vars);
		
		
		// Step 3 : search solution while stopping conditions not met
		int iterations = 0;
		boolean optimalSolutionFound = false;
		int noBetterSolution = 0;
		while(!optimalSolutionFound && (iterations < maxIteration) && (noBetterSolution < maxIterationWhithoutBetterSolution))
		{		
			// Updating values for stopping conditions
			iterations++;
			System.out.println("Iteration n°"+iterations);
			
			
			// Step 3.1 : search candidate solutions
			ArrayList<IntVar[]> candidateSolutions = getCandidateSolutions(vars, domains, tabuList);
		
			// Step 3.2 : select best candidate solution
			IntVar[] bestCandidateSolution = getBestSolution(candidateSolutions);
			
			// Step 3.3 update current solution
			vars = cloneIntVar(bestCandidateSolution);
		
			// Step 3.4 : If current cost is better than cost of best known solution, update best known solution
			if(getViolatedConstraints(bestCandidateSolution) < getViolatedConstraints(bestSolution)) {
				bestSolution = bestCandidateSolution;
				noBetterSolution = 0;
			}
			else {
				noBetterSolution++;
			}
			
			if(getViolatedConstraints(bestSolution) == 0) {
				optimalSolutionFound = true;
			}
		
			// Step 3.5 : update tabu list
			tabuList.insert(bestCandidateSolution);
			
			
			// Print the best solution
			if(noBetterSolution == 0) {
				System.out.println("New best solution found");
				for(int i = 0 ; i < bestSolution.length ; i++) {
					System.out.println(bestSolution[i]);
				}
			}
			
		}
		
		String whyStopped = "";
		if(optimalSolutionFound) 
			whyStopped = " optimal solution found ";
		else if(iterations >= maxIteration)
			whyStopped = " max iterations reached ";
		else if(noBetterSolution >= maxIterationWhithoutBetterSolution)
			whyStopped = " no better solution found in " + noBetterSolution + "iterations ";
		
		System.out.println("\n\nEnd of search because" + whyStopped + ". Best solution is :");
		for(int i = 0 ; i < bestSolution.length ; i++) {
			System.out.println(bestSolution[i]);
		}
		
	}
	
	
	public IntVar[] generate_random() {
		
		System.out.println("Generating random solution...");
		
		for(int i = 0 ; i < vars.length ; i++) {
			int randomNum = domains[i].getRandomValue();
			vars[i].setDomain(randomNum, randomNum);
			System.out.println(vars[i]);
		}
		System.out.println("___________");
		
		return vars;
		
	}
	
	public ArrayList<IntVar[]> getCandidateSolutions(IntVar[] vars, IntDomain[] domains, TabuList tabuList) {
		
		ArrayList<IntVar[]> candidateSolutions = new ArrayList<IntVar[]>();
		
		for(int i = 0 ; i < vars.length ; i++) {
			for(int value = domains[i].min() ; value <= domains[i].max(); value++) {
				if(value != vars[i].value()) {
					IntVar[] neighboor = this.cloneIntVar(vars);
					neighboor[i].setDomain(value, value);
					
					if(!tabuList.contains(neighboor)) {
						candidateSolutions.add(neighboor);
					}
				}
			}
		}
		/*System.out.println("Candidate solutions :");
		for(int j = 0 ; j < candidateSolutions.size() ; j++) {
			for(int i = 0 ; i < candidateSolutions.get(j).length ; i++) {

				System.out.println(candidateSolutions.get(j)[i]);
			}
			System.out.println("____");
		}*/
		
		return candidateSolutions;
	}
	
	public IntVar[] cloneIntVar(IntVar[] toClone) {
		IntVar[] cloned = new IntVar[toClone.length];

		for(int i = 0 ; i < toClone.length ; i++) {
			cloned[i] = new IntVar(toClone[i].store,
					toClone[i].id,
					toClone[i].value(),
					toClone[i].value());
		}
		
		return cloned;
	}
	
	
	public IntVar[] getBestSolution(ArrayList<IntVar[]> candidateSolutions) {
		
		IntVar[] bestSolution = new IntVar[candidateSolutions.get(0).length];
		bestSolution = cloneIntVar(candidateSolutions.get(0));
		int bestCost = getViolatedConstraints(candidateSolutions.get(0));
		
		for(int i = 1 ; i < candidateSolutions.size() ; i++) {
			if(getViolatedConstraints(candidateSolutions.get(i)) < bestCost)
				bestSolution = candidateSolutions.get(i);
		}
		
		return cloneIntVar(bestSolution);
	}
	
	
	// Cost or fitness of an alldifferent constraint
	public int costAllDifferent(int[] sol) {
		int n = 0;
		for (int i=0; i<sol.length; ++i) {
			for (int j=i+1; j<sol.length; ++j) {
				if (sol[i] == sol[j]) ++n;
			}
		}
		return n;
	}
	
	// Fitness of a solution for the n-queens problem
	public int getViolatedConstraints(IntVar[] candidateSolution) {
		
		int[] sol = new int[candidateSolution.length];
		for(int i = 0 ; i < sol.length ; i++) {
			sol[i] = candidateSolution[i].value();
		}
		
		int n = 0;

		// allDifferent on Q
		n += costAllDifferent(sol);

		// allDifferent on y
		int[] aux = new int[sol.length];
		for (int i=0; i<sol.length; ++i) {
			aux[i] = sol[i] + i;
		}
		n += costAllDifferent(aux);

		// allDifferent on z
		for (int i=0; i<sol.length; ++i) {
			aux[i] = sol[i] - i;
		}
		n += costAllDifferent(aux);
		
		
		return n;
	}
	

}
