package routing.community;

import core.DTNHost;

import java.util.ArrayList;
import java.util.Map;

public interface SimilarityCounterImprovedNEWFAIR {

    public double countSimilarity(double[][] matrixEgoNetwork,
                                  double[][] matrixIndirectNode,
                                  int index
                                  );



    public SimilarityCounterImprovedNEWFAIR replicate();
}
