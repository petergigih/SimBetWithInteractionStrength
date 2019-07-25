/*
 * @(#)DeliveryCentralityReport.java
 *
 * Copyright 2010 by University of Pittsburgh, released under GPLv3.
 * 
 */
package report;

import java.util.*;

import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.community.SimBet;
import routing.simbet.SimBetWithFairRouting;


public class BetweennessCentralityReport extends Report implements UpdateListener
{
	/** Count of each node's betweenness value */
		protected Map<DTNHost, Double> betweennessVal;
		
		/** Default value for the snapshot interval */
		public static final int DEFAULT_BETWEENNES_REPORT_INTERVAL = 3600;
		public static final String BETWEENNESS_REPORT_INTERVAL = "betweennessInterval";
		private double lastRecord = Double.MIN_VALUE;
		private int interval;
	
	public BetweennessCentralityReport()
	{
		init();
		Settings settings = getSettings();
		if (settings.contains(BETWEENNESS_REPORT_INTERVAL)) {
			interval = settings.getInt(BETWEENNESS_REPORT_INTERVAL);
		} else {
			interval = -1; /* not found; use default */
		}
		
		if (interval < 0) { /* not found or invalid value -> use default */
			interval = DEFAULT_BETWEENNES_REPORT_INTERVAL;
		}
	}


	@Override
	public void updated(List<DTNHost> hosts) {
		if (isWarmup()) {
			return;
		}
		
		if (SimClock.getTime() - lastRecord >= interval) {
			lastRecord = SimClock.getTime();
			
			for (DTNHost h : hosts ) {
				MessageRouter otherRouter = h.getRouter();
				SimBetWithFairRouting sim= (SimBetWithFairRouting) ((DecisionEngineRouter)otherRouter).getDecisionEngine();
				if (lastRecord==interval) {
					for (int i = 0; i < hosts.size(); i++) {
						betweennessVal.put(hosts.get(i), sim.getBetweennessCentrality());
					}	
				}else{
					for (int i = 0; i < hosts.size(); i++) {
						betweennessVal.replace(hosts.get(i), sim.getBetweennessCentrality());
					}	
				}
				
			}
		}
	}
	
	@Override
	protected void init()
	{
		super.init();
		betweennessVal = new HashMap<DTNHost, Double>();
	}

	@Override
	public void done()
	{
		for(Map.Entry<DTNHost, Double> entry : betweennessVal.entrySet())
		{
			write("" + entry.getKey() + ' ' + entry.getValue());
		}
		super.done();
	}

	
}