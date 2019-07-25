/* 
 * 
 * 
 */
package report;

/** 
 * Records the average buffer occupancy and its variance with format:
 * <p>
 * <Simulation time> <average buffer occupancy % [0..100]> <variance>
 * </p>
 * 
 * 
 */

//import core.DTNHost;
//import core.Settings;
//import core.SimClock;
//import core.UpdateListener;
//import routing.community.selfisness.*;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;

public class SelfishnessReport
//		extends Report implements UpdateListener
{

//	public static final String REPORT_INTERVAL = "sfReportInterval";
//	public static final String SELECT_DISTRIBUTION = "status";
//	public static final int DEFAULT_REPORT_INTERVAL = 3600;
//
//	private Map<DTNHost,Double> map = new HashMap<DTNHost, Double>();
//
//	private String modelDistribution;
//	private double lastRecord = Double.MIN_VALUE;
//	private int interval;
//
//	public SelfishnessReport()
//	{
//		super();
//
//		Settings settings = getSettings();
//		if (settings.contains(REPORT_INTERVAL)) {
//			interval = settings.getInt(REPORT_INTERVAL);
//		} else {
//			interval = -1; /* not found; use default */
//		}
//
//		if (interval < 0) { /* not found or invalid value -> use default */
//			interval = DEFAULT_REPORT_INTERVAL;
//		}
//
//	}
//
//	/**
//	 * Method is called on every update cycle.
//	 *
//	 * @param hosts A list of all hosts in the world
//	 */
//	@Override
//	public void updated(List<DTNHost> hosts) {
//
//		Settings settings=getSettings();
//		modelDistribution = settings.getSetting(SELECT_DISTRIBUTION);
//		if (SimClock.getTime() - lastRecord >= interval) {
//			lastRecord = SimClock.getTime();
//			for (DTNHost host : hosts)
//			{
//				if (modelDistribution.equals("PercentageOfSelfishness")&&!BRPercentageOfSelfishness.sfList.isEmpty())
//					map.put(host,BRPercentageOfSelfishness.sfList.get(host));
//				if (modelDistribution.equals("UniformDistribution")&&!BRUniformDistribution.sfList.isEmpty())
//					map.put(host,BRUniformDistribution.sfList.get(host));
//				if (modelDistribution.equals("GlobalNodeBiasedDistribution")&&!BRGlobalNodeBiasedDistribution.sfList.isEmpty())
//					map.put(host,BRGlobalNodeBiasedDistribution.sfList.get(host));
//				if (modelDistribution.equals("CommunityBiasedDistribution")&&!BRCommunityBiasedDistribution.sfList.isEmpty())
//					map.put(host,BRCommunityBiasedDistribution.sfList.get(host));
//				if (modelDistribution.equals("NormalDistribution")&&!BRNormalDistribution.sfList.isEmpty())
//					map.put(host,BRNormalDistribution.sfList.get(host));
//				if (modelDistribution.equals("GeometricDistribution")&&!BRGeometricDistribution.sfList.isEmpty())
//					map.put(host,BRGeometricDistribution.sfList.get(host));
//			}
//		}
//
//	}
//
//	@Override
//	public void done()
//	{
//		for (Map.Entry<DTNHost, Double> entry : map.entrySet())
//		{
//			DTNHost host = entry.getKey();
//			double value = entry.getValue();
//			write(""+host+" "+value);
//		}
//		super.done();
//	}

}

//	/**
//	 * Record occupancy every nth second -setting id ({@value}).
//	 * Defines the interval how often (seconds) a new snapshot of buffer
//	 * occupancy is taken previous:5
//	 */
//	public static final String BUFFER_REPORT_INTERVAL = "occupancyInterval";
//	/** Default value for the snapshot interval */
//	public static final int DEFAULT_BUFFER_REPORT_INTERVAL = 3600;
//
//	private double lastRecord = Double.MIN_VALUE;
//	private int interval;
//
//	private Map<DTNHost, Double> bufferCounts = new HashMap<DTNHost, Double>();
//	private int updateCounter = 0;  //new added
//
//
//	public SelfishnessReport() {
//		super();
//
//		Settings settings = getSettings();
//		if (settings.contains(BUFFER_REPORT_INTERVAL)) {
//			interval = settings.getInt(BUFFER_REPORT_INTERVAL);
//		} else {
//			interval = -1; /* not found; use default */
//		}
//
//		if (interval < 0) { /* not found or invalid value -> use default */
//			interval = DEFAULT_BUFFER_REPORT_INTERVAL;
//		}
//	}
//
//	public void updated(List<DTNHost> hosts) {
//		if (isWarmup()) {
//			return;
//		}
//
//		if (SimClock.getTime() - lastRecord >= interval) {
//			lastRecord = SimClock.getTime();
//			printLine(hosts);
//			updateCounter++; // new added
//		}
//			/**
//			for (DTNHost ho : hosts ) {
//				double temp = ho.getBufferOccupancy();
//				temp = (temp<=100.0)?(temp):(100.0);
//				if (bufferCounts.containsKey(ho.getAddress()))
//					bufferCounts.put(ho.getAddress(), (bufferCounts.get(ho.getAddress()+temp))/2);
//				else
//				bufferCounts.put(ho.getAddress(), temp);
//			}
//			}
//		*/
//	}
//
//	/**
//	 * Prints a snapshot of the average buffer occupancy
//	 * @param hosts The list of hosts in the simulation
//	 */
//
//	private void printLine(List<DTNHost> hosts) {
//		/**
//		double bufferOccupancy = 0.0;
//		double bo2 = 0.0;
//
//		for (DTNHost h : hosts) {
//			double tmp = h.getBufferOccupancy();
//			tmp = (tmp<=100.0)?(tmp):(100.0);
//			bufferOccupancy += tmp;
//			bo2 += (tmp*tmp)/100.0;
//		}
//
//		double E_X = bufferOccupancy / hosts.size();
//		double Var_X = bo2 / hosts.size() - (E_X*E_X)/100.0;
//
//		String output = format(SimClock.getTime()) + " " + format(E_X) + " " +
//			format(Var_X);
//		write(output);
//		*/
//		for (DTNHost h : hosts ) {
//			double temp = h.getBufferOccupancy();
//			temp = (temp<=100.0)?(temp):(100.0);
//			if (bufferCounts.containsKey(h)){
//				//bufferCounts.put(h, (bufferCounts.get(h)+temp)/2); seems WRONG
//
//				bufferCounts.put(h, bufferCounts.get(h)+temp);
//				//write (""+ bufferCounts.get(h));
//			}
//			else {
//			bufferCounts.put(h, temp);
//			//write (""+ bufferCounts.get(h));
//			}
//		}
//
//
//	}
//
//
//
//	@Override
//	public void done()
//	{
//
//		for (Map.Entry<DTNHost, Double> entry : bufferCounts.entrySet()) {
//
//			DTNHost a = entry.getKey();
//			Integer b = a.getAddress();
//			Double avgBuffer = entry.getValue()/updateCounter;
//			write("" + b + ' ' + avgBuffer);
//
//			//write("" + b + ' ' + entry.getValue());
//		}
//		super.done();
//	}
//
//