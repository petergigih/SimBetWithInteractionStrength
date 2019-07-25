package routing.community;

import core.DTNHost;

import java.util.*;

public interface CentralityDetectionImproved {

    /**
     * Called when get Centrality for
     * compute aggregation interaction strength
     *
     * @param matrixEgoNetwork

     * @return
     */
    public double getCentrality(double[][] matrixEgoNetwork);

    /**
     * replicate the Centrality Object
     *
     * @return
     */
    public CentralityDetectionImproved replicate();
}
