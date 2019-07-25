/*
 * @(#)SimpleCommunityDetection.java
 *
 * Copyright 2010 by University of Pittsburgh, released under GPLv3.
 *
 */
package routing.community;

import core.DTNHost;
import core.Settings;

import java.util.ArrayList;
import java.util.Map;

public class NeighbourhoodSimilarityImprovedNEWFAIR implements SimilarityCounterImprovedNEWFAIR {


	public NeighbourhoodSimilarityImprovedNEWFAIR(Settings s) {	}
	public NeighbourhoodSimilarityImprovedNEWFAIR(NeighbourhoodSimilarityImprovedNEWFAIR proto) {	}

	public double countSimilarity(double[][] matrixEgoNetwork, double[][] matrixIndirectNode, int index) {

		if (matrixIndirectNode == null) {
			return this.countDirectSimilarity(matrixEgoNetwork, index);
		}

		double sim = 0;

		for (int i = 0; i < matrixEgoNetwork.length; i++) {
			for (int j = 0; j < matrixEgoNetwork[0].length ; j++) {
				if (matrixEgoNetwork[i][j] == matrixIndirectNode[i][index] && matrixEgoNetwork[i][j]!=0.0){
					sim++;
				}
			}

		}

		return sim;

	}

	private double countDirectSimilarity(double[][] matrixEgoNetwork, int index) {
		double sim = 0;


		for (int i = 0; i < matrixEgoNetwork.length; i++) {
			for (int j = 0; j < matrixEgoNetwork[0].length; j++) {
				if (matrixEgoNetwork[i][j] == matrixEgoNetwork[i][index] && matrixEgoNetwork[i][j] != 0.0) {
					sim++;
				}
			}


		}

		return sim;

	}


		@Override
	public SimilarityCounterImprovedNEWFAIR replicate() {
		// TODO Auto-generated method stub
		return new NeighbourhoodSimilarityImprovedNEWFAIR(this);

	}
}
