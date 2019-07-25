package routing.prophet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 * @author Gregorius Bima, Sanata Dharma Univeristy
 */
public class ProphetFuzzyNineRules implements RoutingDecisionEngine {

    protected final static String BETA_SETTING = "beta";
    protected final static String P_INIT_SETTING = "initial_p";
    protected final static String SECONDS_IN_UNIT_S = "secondsInTimeUnit";

    protected static final double DEFAULT_P_INIT = 0.75;
    protected static final double GAMMA = 0.92;
    protected static final double DEFAULT_BETA = 0.45;
    protected static final int DEFAULT_UNIT = 30;

    protected static final double P_WITHOUT_TRANSITIVITY1 = 0,
            P_WITHOUT_TRANSITIVITY2 = 0.5,
            P_WITHOUT_TRANSITIVITY3 = 1.0,
            P_WITH_TRANSITIVITY1 = 0,
            P_WITH_TRANSITIVITY2 = 0.5,
            P_WITH_TRANSITIVITY3 = 1.0,
            OUTPUT_VERY_LOW = 0.2,
            OUTPUT_LOW = 0.4,
            OUTPUT_MEDIUM = 0.6,
            OUTPUT_HIGH = 0.8,
            OUTPUT_VERY_HIGH = 1.0;
            
    protected double beta;
    protected double pinit;
    protected double lastAgeUpdate;
    protected int secondsInTimeUnit;

    /** delivery predictabilities*/
    private Map<DTNHost, Double> preds;
    private Map<DTNHost, Double> fuzzypreds;
    /** Input variable*/
    private Map<DTNHost, Double> predswithouttransitivity;
    private Map<DTNHost, Double> predswithtransitivity;
    

    public ProphetFuzzyNineRules(Settings s) {
        if (s.contains(BETA_SETTING)) {
            beta = s.getDouble(BETA_SETTING);
        } else {
            beta = DEFAULT_BETA;
        }

        if (s.contains(P_INIT_SETTING)) {
            pinit = s.getDouble(P_INIT_SETTING);
        } else {
            pinit = DEFAULT_P_INIT;
        }

        if (s.contains(SECONDS_IN_UNIT_S)) {
            secondsInTimeUnit = s.getInt(SECONDS_IN_UNIT_S);
        } else {
            secondsInTimeUnit = DEFAULT_UNIT;
        }

        preds = new HashMap<DTNHost, Double>();
        fuzzypreds = new HashMap<DTNHost, Double>();
        predswithouttransitivity = new HashMap<DTNHost, Double>();
        predswithtransitivity = new HashMap<DTNHost, Double>();
        this.lastAgeUpdate = 0.0;
    }

    public ProphetFuzzyNineRules(ProphetFuzzyNineRules de) {
        beta = de.beta;
        pinit = de.pinit;
        secondsInTimeUnit = de.secondsInTimeUnit;
        preds = new HashMap<DTNHost, Double>();
        fuzzypreds = new HashMap<DTNHost, Double>();
        predswithouttransitivity = new HashMap<DTNHost, Double>();
        predswithtransitivity = new HashMap<DTNHost, Double>();
        this.lastAgeUpdate = de.lastAgeUpdate;
    }

    public RoutingDecisionEngine replicate() {
        return new ProphetFuzzyNineRules(this);
    }

    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        ProphetFuzzyNineRules de = getOtherProphetDecisionEngine(peer);
        
        this.updateDeliveryPredFor(myHost, con);
        de.updateDeliveryPredFor(peer, con);
        this.updateTransitivePreds(myHost, con);
        de.updateTransitivePreds(peer, con);
        
        this.updateFuzzification(con, myHost);
        de.updateFuzzification(con, peer);
    }

    public boolean newMessage(Message m) {
        return true;
    }

    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        if (m.getTo() == otherHost) {
            return true;
        }

        ProphetFuzzyNineRules de = getOtherProphetDecisionEngine(otherHost);

        return de.getFuzzyPredFor(m.getTo()) > this.getFuzzyPredFor(m.getTo());
    }

    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() == hostReportingOld;
    }

    private ProphetFuzzyNineRules getOtherProphetDecisionEngine(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (ProphetFuzzyNineRules) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    private void updateDeliveryPredFor(DTNHost host, Connection con) {
        DTNHost myHost = con.getOtherNode(host);
        ProphetFuzzyNineRules de = getOtherProphetDecisionEngine(host);
        Set<DTNHost> hostSet = new HashSet<DTNHost>(this.preds.size()
                + de.preds.size());
        hostSet.addAll(this.preds.keySet());
        hostSet.addAll(de.preds.keySet());

        this.agePreds();
        de.agePreds();

        double myOldValue = this.getPredFor(host),
                peerOldValue = de.getPredFor(myHost),
                myPforHost = myOldValue + (1 - myOldValue) * pinit,
                peerPforMe = peerOldValue + (1 - peerOldValue) * de.pinit;
        preds.put(host, myPforHost);
        predswithouttransitivity.put(host, myPforHost);
        de.preds.put(myHost, peerPforMe);
        de.predswithouttransitivity.put(myHost, peerPforMe);
    }

    private void updateTransitivePreds(DTNHost host, Connection con) {
        DTNHost myHost = con.getOtherNode(host);
        ProphetFuzzyNineRules de = getOtherProphetDecisionEngine(host);
        Set<DTNHost> hostSet = new HashSet<DTNHost>(this.preds.size()
                + de.preds.size());

        hostSet.addAll(this.preds.keySet());
        hostSet.addAll(de.preds.keySet());

        double myOldValue = this.getPredFor(host),
                peerOldValue = de.getPredFor(myHost),
                myPforHost = myOldValue + (1 - myOldValue) * pinit,
                peerPforMe = peerOldValue + (1 - peerOldValue) * de.pinit;

        for (DTNHost h : hostSet) {
            myOldValue = 0.0;
            peerOldValue = 0.0;

            if (preds.containsKey(h)) {
                myOldValue = preds.get(h);
            }
            if (de.preds.containsKey(h)) {
                peerOldValue = de.preds.get(h);
            }

            if (h != myHost) {
                preds.put(h, myOldValue + (1 - myOldValue) * myPforHost * peerOldValue * beta);
                predswithtransitivity.put(h, myOldValue + (1 - myOldValue) * myPforHost * peerOldValue * beta);
            }
            if (h != host) {
                de.preds.put(h, peerOldValue + (1 - peerOldValue) * peerPforMe * myOldValue * beta);
                de.predswithtransitivity.put(h, peerOldValue + (1 - peerOldValue) * peerPforMe * myOldValue * beta);
            }
        }
    }
    
    private void updateFuzzification(Connection con, DTNHost host){
        DTNHost myHost = con.getOtherNode(host);
        ProphetFuzzyNineRules de = getOtherProphetDecisionEngine(host);
        Set<DTNHost> fuzzyHostSet = new HashSet<DTNHost>(this.predswithouttransitivity.size() +
                this.predswithtransitivity.size() + de.predswithouttransitivity.size()
                + de.predswithtransitivity.size());
        fuzzyHostSet.addAll(this.predswithouttransitivity.keySet());
        fuzzyHostSet.addAll(this.predswithtransitivity.keySet());
        fuzzyHostSet.addAll(de.predswithouttransitivity.keySet());
        fuzzyHostSet.addAll(de.predswithtransitivity.keySet());
        
        for (DTNHost h : fuzzyHostSet) {
            if (h != myHost) {
                fuzzypreds.put(h, defuzzyfication(host));
            }
            if (h != host) {
                de.fuzzypreds.put(h, defuzzyfication(myHost));
            }
        }
    }
    
    private double defuzzyfication(DTNHost host){
        double defuzzy = ((outputVeryLowFunction(host) *OUTPUT_VERY_LOW)+
                (outputLowFunciton(host)*OUTPUT_LOW)+(outputLow1Funciton(host)*OUTPUT_LOW)+
                (outputMediumFunciton(host)*OUTPUT_MEDIUM)+(outputMedium1Funciton(host)*OUTPUT_MEDIUM)+
                (outputMedium2Funciton(host)*OUTPUT_MEDIUM)+(outputHighFunciton(host)*OUTPUT_HIGH)+
                (outputHigh1Funciton(host)*OUTPUT_HIGH)+(outputVeryHighFunction(host)*OUTPUT_VERY_HIGH))
                /sumOfTheMiu(host);
        fuzzypreds.put(host, defuzzy);
        return fuzzypreds.get(host);
     }
    
    private double sumOfTheMiu(DTNHost host){
        return outputVeryLowFunction(host)+outputLowFunciton(host)+
                outputLow1Funciton(host)+outputMediumFunciton(host)+
                outputMedium1Funciton(host)+outputMedium2Funciton(host)+
                outputHighFunciton(host)+outputHigh1Funciton(host)+
                outputVeryHighFunction(host);
    }
    
    private void agePreds() {
        double timeDiff = (SimClock.getTime() - this.lastAgeUpdate)
                / secondsInTimeUnit;

        if (timeDiff == 0) {
            return;
        }

        double mult = Math.pow(GAMMA, timeDiff);
        for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
            e.setValue(e.getValue() * mult);
        }

        this.lastAgeUpdate = SimClock.getTime();
    }

    private double getPredFor(DTNHost host) {
        agePreds(); // make sure preds are updated before getting
        if (preds.containsKey(host)) {
            return preds.get(host);
        } 
        else {
            return 0;
        }
    }
    
    private double getPredNonTransitivityFor(DTNHost host){
        if (predswithouttransitivity.containsKey(host)) {
            return predswithouttransitivity.get(host);
        }
        else {
            return 0;
        }
    }
    
    private double getPredWithTransitivityFor(DTNHost host){
        if (predswithtransitivity.containsKey(host)) {
            return predswithtransitivity.get(host);
        }
        else {
            return 0;
        }
    }
    
    private double getFuzzyPredFor(DTNHost host){
        if (fuzzypreds.containsKey(host)) {
            return fuzzypreds.get(host);
        } else {
            return 0;
        }
    }

    private double nonTransitivityLowFunciton(DTNHost host) {
        if (P_WITHOUT_TRANSITIVITY1 == getPredNonTransitivityFor(host)) return 1.0;
        else if (P_WITHOUT_TRANSITIVITY1 < getPredNonTransitivityFor(host) && getPredNonTransitivityFor(host) < P_WITHOUT_TRANSITIVITY2) 
            return (P_WITHOUT_TRANSITIVITY2 - getPredNonTransitivityFor(host)) / (P_WITHOUT_TRANSITIVITY2 - P_WITHOUT_TRANSITIVITY1); 
        else return 0;
    }

    private double nonTransitivityMediumFunciton(DTNHost host) {
        if (P_WITHOUT_TRANSITIVITY1 < getPredNonTransitivityFor(host) && getPredNonTransitivityFor(host) < P_WITHOUT_TRANSITIVITY2 )
            return (getPredFor(host) - P_WITHOUT_TRANSITIVITY1)/(P_WITHOUT_TRANSITIVITY2 - P_WITHOUT_TRANSITIVITY1);
        else if (P_WITHOUT_TRANSITIVITY2 < getPredNonTransitivityFor(host) && getPredNonTransitivityFor(host) <P_WITHOUT_TRANSITIVITY3 )
            return (P_WITHOUT_TRANSITIVITY3 - getPredNonTransitivityFor(host))/(P_WITHOUT_TRANSITIVITY3 - P_WITHOUT_TRANSITIVITY2);
        else return 0;
    }

    private double nonTransitivityHighFunciton(DTNHost host) {
        if (P_WITHOUT_TRANSITIVITY2 < getPredNonTransitivityFor(host) && getPredNonTransitivityFor(host) < P_WITHOUT_TRANSITIVITY3) 
            return (getPredNonTransitivityFor(host) - P_WITHOUT_TRANSITIVITY2)/(P_WITHOUT_TRANSITIVITY3 - P_WITHOUT_TRANSITIVITY2);
        else if (getPredNonTransitivityFor(host) == P_WITHOUT_TRANSITIVITY3) return 1;
        else return 0;
    }

    private double transitivityLowFunciton(DTNHost host) {
        if (P_WITH_TRANSITIVITY1 == getPredWithTransitivityFor(host)) return 1.0;
        else if (P_WITH_TRANSITIVITY1 < getPredWithTransitivityFor(host) && getPredWithTransitivityFor(host) < P_WITH_TRANSITIVITY2)
            return (P_WITH_TRANSITIVITY2 - getPredWithTransitivityFor(host)) / (P_WITH_TRANSITIVITY2 - P_WITH_TRANSITIVITY1); 
        else return 0;
    }

    private double transitivityMediumFunciton(DTNHost host) {
        if (P_WITH_TRANSITIVITY1 < getPredWithTransitivityFor(host) && getPredWithTransitivityFor(host) < P_WITH_TRANSITIVITY2 )
            return (getPredWithTransitivityFor(host) - P_WITH_TRANSITIVITY1)/(P_WITH_TRANSITIVITY2 - P_WITH_TRANSITIVITY1);
        else if (P_WITH_TRANSITIVITY2 < getPredWithTransitivityFor(host) && getPredWithTransitivityFor(host) <P_WITH_TRANSITIVITY3 ) 
            return (P_WITH_TRANSITIVITY3 - getPredWithTransitivityFor(host))/(P_WITH_TRANSITIVITY3 - P_WITH_TRANSITIVITY2);
        else return 0;
    }

    private double transitivityHighFunciton(DTNHost host) {
        if (P_WITH_TRANSITIVITY2 < getPredWithTransitivityFor(host) && getPredWithTransitivityFor(host) < P_WITH_TRANSITIVITY3)
            return (getPredWithTransitivityFor(host) - P_WITH_TRANSITIVITY2)/(P_WITH_TRANSITIVITY3 - P_WITH_TRANSITIVITY2);
        else if (getPredWithTransitivityFor(host) == P_WITH_TRANSITIVITY3) return 1;
        else return 0;
    }

    private double outputVeryLowFunction(DTNHost host){
        double miu;
        miu = Math.min(nonTransitivityLowFunciton(host), transitivityLowFunciton(host));
        return miu;
    }
    
    private double outputLowFunciton(DTNHost host) {
        double miu;
        miu = Math.min(nonTransitivityLowFunciton(host), transitivityMediumFunciton(host));
        return miu;
    }
    
    private double outputLow1Funciton(DTNHost host) {
        double miu;
        miu = Math.min(nonTransitivityMediumFunciton(host), transitivityLowFunciton(host));
        return miu;
    }
    
    private double outputMedium2Funciton(DTNHost host) {
        double miu;
        miu = Math.min(nonTransitivityLowFunciton(host), transitivityHighFunciton(host));
        return miu;
    }
    
    private double outputMedium1Funciton(DTNHost host) {
        double miu;
        miu = Math.min(nonTransitivityHighFunciton(host), transitivityLowFunciton(host));
        return miu;
    }
    
    private double outputMediumFunciton(DTNHost host) {
        double miu;
        miu = Math.min(nonTransitivityMediumFunciton(host), transitivityMediumFunciton(host));
        return miu;
    }
    
    private double outputHighFunciton(DTNHost host) {
        double miu;
        miu = Math.min(nonTransitivityMediumFunciton(host), transitivityHighFunciton(host));
        return miu;
    }
    
    private double outputHigh1Funciton(DTNHost host) {
        double miu;
        miu = Math.min(nonTransitivityHighFunciton(host), transitivityMediumFunciton(host));
        return miu;
    }
    
    private double outputVeryHighFunction(DTNHost host){
        double miu;
        miu = Math.min(nonTransitivityHighFunciton(host), transitivityHighFunciton(host));
        return miu;
    }
    
}
