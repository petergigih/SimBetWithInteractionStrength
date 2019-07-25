/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import routing.community.CommunityDetectionEngine;
import routing.community.Matrix;

import java.util.*;

/**
 * Reports the node contact time (i.e., how long they were in the range
 * of each other) distribution. Report file contains the count of connections
 * that lasted for certain amount of time. Syntax:<br>
 * <code>time nrofContacts</code>
 */
public class MatrixPerNodeReport extends Report {


	/**
	 * Constructor.
	 */
	public MatrixPerNodeReport() {
		init();
	}
		

	@Override
	public void done() {
		List<DTNHost> nodes = SimScenario.getInstance().getHosts();
		List<double[][]> matrixEgoNetwork = new LinkedList<double[][]>();


		for(DTNHost h : nodes)
		{
			MessageRouter r = h.getRouter();
			if(!(r instanceof DecisionEngineRouter) )
				continue;
			RoutingDecisionEngine de = ((DecisionEngineRouter)r).getDecisionEngine();
			if(!(de instanceof Matrix))
				continue;
			Matrix cd = (Matrix)de;

			boolean alreadyHaveMatrix = false;
			double[][] nodeComm = cd.getMatrix();



			if( nodeComm.length > 0)
			{
				matrixEgoNetwork.add(nodeComm);
			}
		}

		// print each matrix and its size out to the file
		for(double[][] c : matrixEgoNetwork)

//			write("" + c.length + ' ' + c);
		write("" + c.length + ' ' +Arrays.deepToString(c));
//
//			write("" + c.length + ' ' +Arrays.deepToString(c));
		super.done();
	}

}
