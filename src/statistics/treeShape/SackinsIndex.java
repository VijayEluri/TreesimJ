package statistics.treeShape;

import java.util.ArrayList;
import java.util.List;

import population.Locus;

import statistics.Options;
import statistics.TreeStatistic;
import tree.DiscreteGenTree;

public class SackinsIndex extends TreeStatistic {


	public static final String identifier = "Sackin's index of tree imbalance (mean)";
	
	public String getIdentifier() {
		return identifier;
	}
	
	public SackinsIndex() {	}
	
	public SackinsIndex getNew(Options ops) {
		this.options = ops;
		return new SackinsIndex();
	}
	
	public void collect(DiscreteGenTree tree) {
		if (tree==null)
			return;

		Locus root = tree.getRoot();
		List<Locus> tips = tree.getTips();

		double sum = 0;
		for(Locus tip : tips) {
			sum += DiscreteGenTree.getNodesToRoot(tip);
		}
		values.add(sum/(double)tips.size());
	}

	public boolean showOnScreenLog() {
		return true;
	}
	
	public String getDescription() {
		return "Sackin's Index of tree imbalance";
	}

}
