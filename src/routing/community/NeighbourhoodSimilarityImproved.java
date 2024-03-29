/*
 * @(#)SimpleCommunityDetection.java
 *
 * Copyright 2010 by University of Pittsburgh, released under GPLv3.
 * 
 */
package routing.community;

import core.*;

import java.util.ArrayList;
import java.util.Map;

public class NeighbourhoodSimilarityImproved implements SimilarityCounterImproved {

	public NeighbourhoodSimilarityImproved(Settings s) {	}
	public NeighbourhoodSimilarityImproved(NeighbourhoodSimilarityImproved proto) {	}

	public double countSimilarity(double[][] matrixEgoNetwork, double[][] matrixIndirectNode, int index,
								  Map<DTNHost,ArrayList<Double>> neighborsHistory) {

		if (matrixIndirectNode == null) {
			return this.countDirectSimilarity(matrixEgoNetwork, index,neighborsHistory);
		}

		double sim=1;

		for (int i = 0; i < matrixEgoNetwork.length; i++) {

			if (matrixEgoNetwork[i][0]==this.countAggrIntStrength(neighborsHistory) && matrixIndirectNode[i][index]==this.countAggrIntStrength(neighborsHistory)) {
				sim++;
			}

		}

		return sim;

	}

	private double countDirectSimilarity(double[][] matrixEgoNetwork, int index, Map<DTNHost,ArrayList<Double>> neighborsHistory) {
		double sim=1;

		for (int i = 0; i < matrixEgoNetwork.length; i++) {

			if (matrixEgoNetwork[i][0]==this.countAggrIntStrength(neighborsHistory) && matrixEgoNetwork[i][index]==this.countAggrIntStrength(neighborsHistory)) {
				sim++;
			}
		}

		return sim;

	}

	protected double countAgrIntStrength(double lambda, double sigma) {
		return lambda * (lambda - sigma);
	}

	public double countAggrIntStrength(Map<DTNHost, ArrayList<Double>> neighborsHist) {
		double AggrIntStrength = 0;
		double lambda = 0, sigma = 0;
		for (Map.Entry<DTNHost, ArrayList<Double>> data : neighborsHist.entrySet()) {
			lambda = data.getValue().get(0);
			sigma = data.getValue().get(1);

			AggrIntStrength =  this.countAgrIntStrength(lambda, sigma);
		}

		return AggrIntStrength;
	}



	@Override
	public SimilarityCounterImproved replicate() {
		// TODO Auto-generated method stub
		return new NeighbourhoodSimilarityImproved(this);

	}
}
