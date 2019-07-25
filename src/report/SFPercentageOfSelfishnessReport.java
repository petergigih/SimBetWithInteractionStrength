package report;

//import core.DTNHost;
//import core.UpdateListener;
//import routing.community.selfisness.BRPercentageOfSelfishness;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;

public class SFPercentageOfSelfishnessReport
//        extends Report implements UpdateListener
{
//    private Map<Integer, Double> map = new HashMap<>();
////    private String[] sfValue = BRPercentageOfSelfishness.sfValue;
//
//    public SFPercentageOfSelfishnessReport()
//    {
//        init();
//    }
//
//    /**
//     * Method is called on every update cycle.
//     *
//     * @param hosts A list of all hosts in the world
//     */
//    @Override
//    public void updated(List<DTNHost> hosts) {
//        if(map.isEmpty()){
//            for (DTNHost host : hosts)
//            {
//                map.put(host.getAddress(),1.0);
//            }
////            for (String host : sfValue)
////            {
////                map.put(Integer.parseInt(host),0.0);
////            }
//        }
//    }
//
//    @Override
//    public void done()
//    {
//        for (Map.Entry<Integer, Double> entry : map.entrySet())
//        {
//            int host = entry.getKey();
//            double value = entry.getValue();
//            write(""+host+" "+value);
//        }
//        super.done();
//    }
}
