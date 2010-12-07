package population;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import treesimj.RecombinantLocus;
import treesimj.Recombineable;

import cern.jet.random.engine.RandomEngine;
import dnaModels.DNASequence;
import fitnessProviders.FitnessProvider;

/**
 * A single, potentially recombining genetic locus, with references to exactly one parent, zero or more offspring loci, and zero
 * or one 'recombination partners', which are members of the same generation which have exchanged genetic info with this 
 * locus. The root locus always has parent null. Loci have a FitnessProvider element which they query to determine their 
 * fitness.
 * 
 * @author brendan
 *
 */
public class Locus implements Serializable {

	private FitnessProvider fitnessData; //Describes fitness
	private double relativeFitness; 	//Fitness relative to population average in this generation - set & used by Population
	private long id;					//An almost certainly unique id
	private ArrayList<Locus> offspring; //List of all offspring individuals
	private Locus parent;				//Parent individual
	private RandomEngine rng;
	private String label;	
	private int depth = -1; //Handy for use when traversing big trees
	private int originPop = -1; 		//In multiple population models, the pop number in which this individual was created. -1 signals nothing has been set. 
	
	private String pop = "Main population";
	
	boolean preserve; //A flag to indicate whether or not this individual can be cleared from the population. Used for serial-tree sampling
	
	//Potentially null, this is a member of the same generation as this ind with whom we've exchanged genetic info via 'recombine'
	private Locus recombinationPartner = null;
	
	private int breakPointMin = 0; //These variables specify the boundaries of the recombinant region, such that this part of the chromosome is assumed
	private int breakPointMax = 0; //to have actually come from the locus 'recomPartner'	

	
	public Locus(RandomEngine rng) {
		offspring = new ArrayList<Locus>(2);
		parent = null;
		relativeFitness = 1.0;
		this.rng = rng;
		if (rng != null)
			id = rng.nextLong();
		else
			id = 0;
	}
	
	public void setOriginPopulation(int num) {
		originPop = num;
	}
	
	public int getOriginPop() {
		return originPop;
	}

	
	public void setLabel(String l) {
		label = l;
	}
	
	public String getLabel() {
		return label;
	}
	
	/**
	 * A flag to mark this individual for non-removal from the population. This is used by the SerialTreeSampler to collect serial trees
	 * @return
	 */
	public boolean isPreserve() {
		return preserve;
	}

	public void setPreserve(boolean preserve) {
		this.preserve = preserve;
	}
	
	/**
	 * A flag that is used by a few Statistics to keep track of an individuals depth in a genealogy. At some point we could
	 * implement an annotation-type system for Individuals, and thus generalize the ability to set a key=value label for Individuals
	 * @return
	 */
	public int getDepth() {
		return depth;
	}
	
	public void setDepth(int d) {
		depth =d ;
	}

	
	/**
	 * Returns index of a particular offspring, or -1 if kid is not an immediate descendant of this individual
	 * @param kid
	 * @return
	 */
	public Integer offspringIndex(Locus kid) {
		Integer index = offspring.indexOf(kid);
		if (index==-1) {
			return null;
		}
		else
			return index;
	}
	
	/**
	 * Fetches the DNA sequence used to calculate fitness, or if it doesn't exist, the first DNA sequence that
	 * exists in inheritables, or null if there's no DNA anywhere.
	 * @return
	 */
	public DNASequence getPrimaryDNA() {
		if (fitnessData.getSubstrate() instanceof DNASequence) {
			return (DNASequence)fitnessData.getSubstrate();
		}
		
		return null;
	}
	
	public long getID() {
		return id;
	}
	
	public String getReadableID() {
		StringBuffer buf = new StringBuffer("i");
		String num = String.valueOf(Math.abs(id)).substring(0, 5);
		buf.append(num);
		if (originPop>-1) {
			buf.append("_p" + String.valueOf(originPop));
		}

		return buf.toString();
	}
	
	public void setID(long newID) {
		id = newID;
	}
	
	/**
	 * Returns true if we have zero offspring. 
	 * @return
	 */
	public boolean isTip() {
		return offspring.size()==0;
	}
	
	/**
	 * Used by Population to determine if individuals reproduce. 
	 */
	public double getRelFitness() {
		return relativeFitness;
	}
	
	public void setRelFitness(double val) {
		relativeFitness = val;
	}
	
	public void setFitnessProvider(FitnessProvider fitness) {
		fitnessData = fitness;
	}
	
	public double getFitness() {
		return fitnessData.getFitness();
	}
	
	
	//Removes this individual from the offspring list, if it's in there
	public boolean removeOffspring(Locus kid) {
		return offspring.remove(kid);
	}
	
	/**
	 * Remove this individual from the population and all ancestors of this individual, as
	 * long as the ancestors all have exactly one offspring (leading 'up to' this individual).  
	 * We must be aware that some individuals, potentially ancestors of this individual, are 'preserved', 
	 * meaning that they should not be removed from the population (this is for compatibility with 
	 * serial-sampling stuff, which is not implemented in a release version)
	 * 
	 */
	public void removeFromPop() {
		if (preserve)
			return;
		
		int depth = 0;
		Locus ref = this;
		
		while(ref.getParent().numOffspring()==1 && !ref.getParent().isPreserve()) {
			ref = ref.getParent();
			if (ref.getParent()==null) {
				System.out.println("Individual has no parent at depth: " + depth);
				System.out.println("ID is : " + ref.getReadableID());
			}
			depth++;
		}

		
		ref.getParent().removeOffspring(ref);
		ref.setParent(null);
	}
	
	/**
	 * Return the number of generations (Individuals / nodes in the tree) between this individual and 
	 * the root
	 * @return
	 */
	public int distToRoot() {
		int dist = 0;
		Locus ref = this;
		while(ref.getParent() != null) {
			dist++;
			ref = ref.getParent();
		}
		return dist;
	}
	
	/**
	 * Calls mutate on the fitness data and all inheritables. 
	 */
	public void mutate() {
		fitnessData.mutate();
	}
	
	public void setParent(Locus par) {
		parent = par;
	}
	
	public void addOffspring(Locus kid) {
		offspring.add(kid);
	}
	
	public ArrayList<Locus> getOffspring() {
		return offspring;
	}
	
	public Locus getOffspring(int which) {
		return offspring.get(which);
	}
	
	public int numOffspring() {
		return offspring.size();
	}
	
	public Locus getParent() {
		return parent;
	}
	
	
	/**
	 * Returns an individual with the same data AND the same parents and offspring as this
	 * Note that data is cloned, but parents and offspring are just references
	 */
	public Locus getCompleteCopy() {
		Locus ind = new Locus(rng);
		ind.copyDataFrom(this);
		ind.setParent( parent );
		for(Locus kid : offspring)
			ind.addOffspring(kid);
		
		return ind;
	}
	
	/**
	 * Creates a clone of this individual and copies all data to the new individual using .copyDataFrom(this)
	 * @return
	 */
	public Locus getDataCopy() {
		Locus ind = new Locus(rng);
		ind.copyDataFrom(this);
		return ind;
	}
	
	/**
	 * This just passes references from the parent data to the offspring
	 * @param parent
	 */
	public void inheritFrom(Locus parent) {
		fitnessData = parent.getFitnessData();
	}
	
	
	/**
	 * Copies data from source individual ind. 
	 * @param ind
	 */
	public void copyDataFrom(Locus ind) {
		originPop = ind.originPop;
		fitnessData = ind.getFitnessData().getCopy();
	}
	
	public FitnessProvider getFitnessData() {
		return fitnessData;
	}
	
	public String getPop() {
		return pop;
	}
	
	public void setPop(String newpop) {
		pop = newpop;
	}
	
	/************************ Recombination related stuff **************************************/
	
	/**
	 * Return the other locus that we've recombined with, or null if none
	 */
	public Locus getRecombinationPartner() {
		return recombinationPartner;
	}

	 /**
	  * Return true if we have recombined with some other locus
	  * @return
	  */
	public boolean hasRecombination() {
		return recombinationPartner != null;
	}
	
	//Does this make sense?
//	public List<Locus> getOffspringForSite(int site) {
//		List<Locus> siteOffspring = new ArrayList<Locus>(2);
//		if (offspring == null)
//			return siteOffspring;
//		
//		for(Locus kid : offspring) {
//			if (kid.getParentForSite(site)==this)
//				siteOffspring.add(kid);
//			else {
//				Locus rKid = kid.getRecombinationPartner();
//				if (rKid == null) {
//					System.out.println("Hmm, this kid thinks we're not his parent and he has no recombination partner!");
//				}
//				if (rKid.getParentForSite(site)!=this) {
//					System.out.println("Hmm this kid doesn't think we're his parent, and his recomb partner thinks we're not his parent either, at site " + site);
//					System.out.println("Me: " + this.getID() + " partner is: " + rKid.getID() + " partner's parent at site: " + rKid.getParentForSite(site).getID());
//				}
//				siteOffspring.add(rKid);
//			}
//		}
//		return siteOffspring;
//	}

	 /**
	  * Return the locus that donated the genetic info at the given site. If site >= breakPointMin AND < breakPointMax, we
	  * return the recombination partner, thus breakpoints are half open, and INCLUSIVE OF MIN, EXCLUSIVE OF MAX
	  */
	public Locus getParentForSite(int site) {
		
		if (site >= breakPointMin && site < breakPointMax) {
			if (recombinationPartner==null) 
				System.out.println("Hmm, breakPoints for this individual are>0, but there's no recomb. partner...");
			
			return recombinationPartner.getParent();
		}
		
		return parent;
	}
	
	/**
	 * Return the minimum value (inclusive) of recombination breakpoint
	 * @return
	 */
	public int getBreakPointMin() {
		return breakPointMin;
	}
	
	/**
	 * Return one site past recombination breakpoint
	 * @return
	 */
	public int getBreakPointMax() {
		return breakPointMax;
	}
	
	/**
	 * Set parent and recombination partner references to null. This should happen so this Locus can be GC'd
	 */
	public void clearReferences() {
		parent = null;
		recombinationPartner = null;
	}
	
	
	public void recombine(Locus one, Locus two) {
		if (one.hasRecombination() || two.hasRecombination()) {
			throw new IllegalArgumentException("one of the recombining inds already has a breakpoint");
		}
		
		//We try to find a site that is not at zero or the end.. not sure what would happen then
		double min = Math.min(one.getRecombineableData().getLength(), two.getRecombineableData().getLength())-1;
		int site = (int)Math.floor( min*Math.random() )+1;
		while (site==0) {
			site = (int)Math.floor( min*Math.random() )+1;
		}
		
		if (site==0 || site==one.getLength()) {
			System.out.println("Hmm, we managed to pick a recombination site at the edge..");
		}
		
		boolean upper = Math.random() > 0.5; //loci will swap segments above breakpoint
		
		if (upper) {
			//System.out.println("Recombining upper portions of " + one.getID() + " and " + two.getID() + " at site " + site);
			//System.out.println("Parent of one is: " + one.getUpperParent() + "\n parent of two is: " + two.getUpperParent());		
			one.setRecombinationPartner(site, one.getLength(), two);
			two.setRecombinationPartner(site, one.getLength(), one);
			
			Recombineable rOne = one.getRecombineableData();
			Recombineable rTwo = two.getRecombineableData();
			
			Object rOneData = rOne.getRegion(site, one.getLength());
			Object rTwoData = rTwo.getRegion(site, one.getLength());
			rOne.setRegion(site, one.getLength(), rTwoData);
			rTwo.setRegion(site, one.getLength(), rOneData);
		}
		else {
			//System.out.println("Recombining lower portions of " + one.getID() + " and " + two.getID() + " at site " + site);
			//System.out.println("Parent of one is: " + one.getUpperParent() + "\n parent of two is: " + two.getUpperParent());
			one.setRecombinationPartner(0, site, two);
			two.setRecombinationPartner(0, site, one);
			
			Recombineable rOne = one.getRecombineableData();
			Recombineable rTwo = two.getRecombineableData();
			
			Object rOneData = rOne.getRegion(0, site);
			Object rTwoData = rTwo.getRegion(0, site);
			rOne.setRegion(0, site, rTwoData);
			rTwo.setRegion(0, site, rOneData);
		}
	}

	 
	public Recombineable getRecombineableData() {
		if (fitnessData instanceof Recombineable) 
			return (Recombineable)fitnessData;
		else
			return null;
	}
	
	
	/**
	 * Return interior breakpoint, confusing because boundary seems to change according to whether or not the upper
	 * or lower half of the segment is the recombinant one 
	 * @return
	 */
//	public int getBreakPoint() {
//		return breakPointMin > 0 ? breakPointMin : breakPointMax;
//	}
	
}
