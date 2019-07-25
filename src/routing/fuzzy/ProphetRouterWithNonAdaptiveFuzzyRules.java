/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.fuzzy;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 * @author Waiki
 */
public class ProphetRouterWithNonAdaptiveFuzzyRules implements RoutingDecisionEngine {

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
            OUTPUT_VERY_LOW = 0.1,
            OUTPUT_LOW = 0.3,
            OUTPUT_MEDIUM = 0.5,
            OUTPUT_HIGH = 0.7,
            OUTPUT_VERY_HIGH = 0.9;

    protected double beta;
    protected double pinit;
    protected double lastAgeUpdate;
    protected int secondsInTimeUnit;

    /** Delivery predictabilities */
    private Map<DTNHost, Double> predictability;
    
    /** Input variable*/
    private Map<DTNHost, Double> predsWithoutTransitivity;
    private Map<DTNHost, Double> predsWithTransitivity;
    /** Output from fuzzy inference */
    private Map<DTNHost, Double> defuzzificationPredictability;

    public ProphetRouterWithNonAdaptiveFuzzyRules(Settings s) {
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

        predictability = new HashMap<DTNHost, Double>();
        predsWithoutTransitivity = new HashMap<DTNHost, Double>();
        predsWithTransitivity = new HashMap<DTNHost, Double>();
        defuzzificationPredictability = new HashMap<DTNHost, Double>();
        this.lastAgeUpdate = 0.0;
    }

    public ProphetRouterWithNonAdaptiveFuzzyRules(ProphetRouterWithNonAdaptiveFuzzyRules prototype) {
        beta = prototype.beta;
        pinit = prototype.pinit;
        secondsInTimeUnit = prototype.secondsInTimeUnit;
        predictability = new HashMap<DTNHost, Double>();
        predsWithoutTransitivity = new HashMap<DTNHost, Double>();
        predsWithTransitivity = new HashMap<DTNHost, Double>();
        defuzzificationPredictability = new HashMap<DTNHost, Double>();
        this.lastAgeUpdate = prototype.lastAgeUpdate;
    }

    public RoutingDecisionEngine replicate() {
        return new ProphetRouterWithNonAdaptiveFuzzyRules(this);
    }

    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        ProphetRouterWithNonAdaptiveFuzzyRules myPartner = getOtherProphetDecisionEngine(peer);
        Set<DTNHost> hostSet = new HashSet<DTNHost>(this.predictability.size()
                + myPartner.predictability.size());
        hostSet.addAll(this.predictability.keySet());
        hostSet.addAll(myPartner.predictability.keySet());

        this.agePreds();
        myPartner.agePreds();

        // Update preds for this connection
        double myOldValue = this.getPredictabilityFor(thisHost),
                peerOldValue = myPartner.getPredictabilityFor(peer),
                myPforHost = myOldValue + (1 - myOldValue) * pinit,
                peerPforMe = peerOldValue + (1 - peerOldValue) * myPartner.pinit;
        this.predictability.put(thisHost, myPforHost);
        this.predsWithoutTransitivity.put(thisHost, myPforHost);
        myPartner.predictability.put(peer, peerPforMe);
        myPartner.predsWithoutTransitivity.put(peer, peerPforMe);

        // Update transistivities
        for (DTNHost h : hostSet) {
            myOldValue = 0.0;
            peerOldValue = 0.0;

            if (this.predictability.containsKey(h)) {
                myOldValue = this.predictability.get(h);
            }
            if (myPartner.predictability.containsKey(h)) {
                peerOldValue = myPartner.predictability.get(h);
            }

            if (h != thisHost) {
                this.predictability.put(h, myOldValue + (1 - myOldValue) * myPforHost * peerOldValue * beta);
                this.predsWithTransitivity.put(h, myOldValue + (1 - myOldValue) * myPforHost * peerOldValue * beta);
            }
            if (h != peer) {
                myPartner.predictability.put(h, peerOldValue + (1 - peerOldValue) * peerPforMe * myOldValue * beta);
                myPartner.predsWithTransitivity.put(h, peerOldValue + (1 - peerOldValue) * peerPforMe * myOldValue * beta);
            }
        }
        
        /** Each pair of node compute their peer fuzzy value for */
        this.defuzzificationFor(peer);
        myPartner.defuzzificationFor(thisHost);
    }

    public void connectionDown(DTNHost thisHost, DTNHost peer) {}

    public void doExchangeForNewConnection(Connection con, DTNHost peer) {}

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

        ProphetRouterWithNonAdaptiveFuzzyRules myPartner = getOtherProphetDecisionEngine(otherHost);

        return myPartner.getDefuzzificationPredictabilityFor(m.getTo()) > this.getDefuzzificationPredictabilityFor(m.getTo());
    }

    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() == hostReportingOld;
    }

    private ProphetRouterWithNonAdaptiveFuzzyRules getOtherProphetDecisionEngine(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (ProphetRouterWithNonAdaptiveFuzzyRules) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    /**
     * Called when the host update fuzzification process
     */
    private void defuzzificationFor(DTNHost host){
        double defuzzy = ((outputVeryLowFunction(host) *OUTPUT_VERY_LOW)+
                (outputLowLeftShoulderLowMediumFunciton(host)*OUTPUT_LOW)+
                (outputLowRightShoulderLowMediumFunciton(host)*OUTPUT_LOW)+
                (outputLowLeftShoulderMediumLowFunciton(host)*OUTPUT_LOW)+
                (outputLowRightShoulderMediumLowFunciton(host)*OUTPUT_LOW)+
                (outputMediumLeftShoulderLowHighFunciton(host)*OUTPUT_MEDIUM)+
                (outputMediumRightShoulderLowHighFunciton(host)*OUTPUT_MEDIUM)+
                (outputMediumLeftShoulderMediumMediumFunciton(host)*OUTPUT_MEDIUM)+
                (outputMediumRightShoulderMediumMediumFunciton(host)*OUTPUT_MEDIUM)+
                (outputMediumLeftShoulderHighLowFunciton(host)*OUTPUT_MEDIUM)+
                (outputMediumRightShoulderHighLowFunciton(host)*OUTPUT_MEDIUM)+
                (outputHighLeftShoulderMediumHighFunciton(host)*OUTPUT_HIGH)+
                (outputHighRightShoulderMediumHighFunciton(host)*OUTPUT_HIGH)+
                (outputHighLeftShoulderHighMediumFunciton(host)*OUTPUT_HIGH)+
                (outputHighRightShoulderHighMediumFunciton(host)*OUTPUT_HIGH)+
                (outputVeryHighFunction(host)*OUTPUT_VERY_HIGH))
                /sumOfTheMiu(host);
        defuzzificationPredictability.put(host, defuzzy);
     }
    
    /**
     *Called when fuzzification for summing the output 
     *of each of the rules
     */
    private double sumOfTheMiu(DTNHost host){
        return outputVeryLowFunction(host)+
                outputLowLeftShoulderLowMediumFunciton(host)+
                outputLowRightShoulderLowMediumFunciton(host)+
                outputLowLeftShoulderMediumLowFunciton(host)+
                outputLowRightShoulderMediumLowFunciton(host)+
                outputMediumLeftShoulderLowHighFunciton(host)+
                outputMediumRightShoulderLowHighFunciton(host)+
                outputMediumLeftShoulderMediumMediumFunciton(host)+
                outputMediumRightShoulderMediumMediumFunciton(host)+
                outputMediumLeftShoulderHighLowFunciton(host)+
                outputMediumRightShoulderHighLowFunciton(host)+
                outputHighLeftShoulderMediumHighFunciton(host)+
                outputHighRightShoulderMediumHighFunciton(host)+
                outputHighLeftShoulderHighMediumFunciton(host)+
                outputHighRightShoulderHighMediumFunciton(host)+
                outputVeryHighFunction(host);
    }
    
    private void agePreds() {
        double timeDiff = (SimClock.getTime() - this.lastAgeUpdate)
                / secondsInTimeUnit;

        if (timeDiff == 0) {
            return;
        }

        double mult = Math.pow(GAMMA, timeDiff);
        for (Map.Entry<DTNHost, Double> e : predictability.entrySet()) {
            e.setValue(e.getValue() * mult);
        }

        this.lastAgeUpdate = SimClock.getTime();
    }

    /**
     * Returns the current prediction (P) value for a host or 0 if entry for the
     * host doesn't exist.
     *
     * @param host The host to look the P for
     * @return the current P value
     */
    private double getPredictabilityFor(DTNHost host) {
        agePreds(); // make sure preds are updated before getting
        if (predictability.containsKey(host)) {
            return predictability.get(host);
        } else {
            return 0;
        }
    }
    
    /**
     * Return the current fuzzy value for this host
     * 
     * @param host 
     * @return the current fuzzy output value
     */
    private double getDefuzzificationPredictabilityFor(DTNHost host) {
        if (defuzzificationPredictability.containsKey(host)) {
            return defuzzificationPredictability.get(host);
        } else {
            return 0;
        }
    }
    
    /**
     * Return the current input value for this host
     * 
     * @param host The host to look the input value for
     * @return the current input value
     */
    private double getPredictabilityWithoutTransitivityFor(DTNHost host){
        if (predsWithoutTransitivity.containsKey(host)) {
            return predsWithoutTransitivity.get(host);
        }
        else {
            return 0;
        }
    }
    
    /**
     * Return the current input value for this host
     * 
     * @param host The host to look the input value for
     * @return the current input value
     */
    private double getPredictabilityWithTransitivityFor(DTNHost host){
        if (predsWithTransitivity.containsKey(host)) {
            return predsWithTransitivity.get(host);
        }
        else {
            return 0;
        }
    }
    
    private double withoutTransitivityLowFunciton(DTNHost host) {
        if (P_WITHOUT_TRANSITIVITY1 == getPredictabilityWithoutTransitivityFor(host)) return 1.0;
        else if (P_WITHOUT_TRANSITIVITY1 < getPredictabilityWithoutTransitivityFor(host) 
                && getPredictabilityWithoutTransitivityFor(host) < P_WITHOUT_TRANSITIVITY2) 
            return (P_WITHOUT_TRANSITIVITY2 - getPredictabilityWithoutTransitivityFor(host)) 
                    / (P_WITHOUT_TRANSITIVITY2 - P_WITHOUT_TRANSITIVITY1); 
        else return 0;
    }

    private double withoutTransitivityMediumFunciton(DTNHost host) {
        if (P_WITHOUT_TRANSITIVITY1 < getPredictabilityWithoutTransitivityFor(host) 
                && getPredictabilityWithoutTransitivityFor(host) < P_WITHOUT_TRANSITIVITY2 )
            return (getPredictabilityWithoutTransitivityFor(host) - P_WITHOUT_TRANSITIVITY1)
                    /(P_WITHOUT_TRANSITIVITY2 - P_WITHOUT_TRANSITIVITY1);
        else if (P_WITHOUT_TRANSITIVITY2 < getPredictabilityWithoutTransitivityFor(host) 
                && getPredictabilityWithoutTransitivityFor(host) <P_WITHOUT_TRANSITIVITY3 )
            return (P_WITHOUT_TRANSITIVITY3 - getPredictabilityWithoutTransitivityFor(host))
                    /(P_WITHOUT_TRANSITIVITY3 - P_WITHOUT_TRANSITIVITY2);
        else return 0;
    }

    private double withoutTransitivityHighFunciton(DTNHost host) {
        if (P_WITHOUT_TRANSITIVITY2 < getPredictabilityWithoutTransitivityFor(host) 
                && getPredictabilityWithoutTransitivityFor(host) < P_WITHOUT_TRANSITIVITY3) 
            return (getPredictabilityWithoutTransitivityFor(host) - P_WITHOUT_TRANSITIVITY2)
                    /(P_WITHOUT_TRANSITIVITY3 - P_WITHOUT_TRANSITIVITY2);
        else if (getPredictabilityWithoutTransitivityFor(host) == P_WITHOUT_TRANSITIVITY3) return 1;
        else return 0;
    }

    private double withTransitivityLowFunciton(DTNHost host) {
        if (P_WITH_TRANSITIVITY1 == getPredictabilityWithTransitivityFor(host)) return 1.0;
        else if (P_WITH_TRANSITIVITY1 < getPredictabilityWithTransitivityFor(host) 
                && getPredictabilityWithTransitivityFor(host) < P_WITH_TRANSITIVITY2)
            return (P_WITH_TRANSITIVITY2 - getPredictabilityWithTransitivityFor(host)) 
                    / (P_WITH_TRANSITIVITY2 - P_WITH_TRANSITIVITY1); 
        else return 0;
    }

    private double withTransitivityMediumFunciton(DTNHost host) {
        if (P_WITH_TRANSITIVITY1 < getPredictabilityWithTransitivityFor(host) 
                && getPredictabilityWithTransitivityFor(host) < P_WITH_TRANSITIVITY2 )
            return (getPredictabilityWithTransitivityFor(host) - P_WITH_TRANSITIVITY1)
                    /(P_WITH_TRANSITIVITY2 - P_WITH_TRANSITIVITY1);
        else if (P_WITH_TRANSITIVITY2 < getPredictabilityWithTransitivityFor(host) 
                && getPredictabilityWithTransitivityFor(host) <P_WITH_TRANSITIVITY3 ) 
            return (P_WITH_TRANSITIVITY3 - getPredictabilityWithTransitivityFor(host))
                    /(P_WITH_TRANSITIVITY3 - P_WITH_TRANSITIVITY2);
        else return 0;
    }

    private double withTransitivityHighFunciton(DTNHost host) {
        if (P_WITH_TRANSITIVITY2 < getPredictabilityWithTransitivityFor(host)
                && getPredictabilityWithTransitivityFor(host) < P_WITH_TRANSITIVITY3)
            return (getPredictabilityWithTransitivityFor(host) - P_WITH_TRANSITIVITY2)
                    /(P_WITH_TRANSITIVITY3 - P_WITH_TRANSITIVITY2);
        else if (getPredictabilityWithTransitivityFor(host) == P_WITH_TRANSITIVITY3) return 1;
        else return 0;
    }
    
    private double outputVeryLowFunction(DTNHost host){
        double miu;
        miu = Math.min(withoutTransitivityLowFunciton(host), withTransitivityLowFunciton(host));
        if (miu == 1.0) {
            double xVeryLow = 0;
            
            /**
             * The range of the X coordinate for this function is about [0,0.2]
             * I sum each of it from 0 until 0.2 for getting the X value
             */
            for (double i = 0; i <= 0.1; i = i + 0.01) {
                xVeryLow = xVeryLow + i;
            }
            return xVeryLow;
        } else {
            return (OUTPUT_LOW - miu) / (OUTPUT_LOW - OUTPUT_VERY_LOW);
        }
    }
    
    private double outputLowLeftShoulderLowMediumFunciton(DTNHost host) {
        double miu;
        miu = Math.min(withoutTransitivityLowFunciton(host), withTransitivityMediumFunciton(host));
        return (OUTPUT_MEDIUM - miu) / (OUTPUT_MEDIUM - OUTPUT_LOW);
    }
    
    private double outputLowRightShoulderLowMediumFunciton(DTNHost host) {
        double miu;
        miu = Math.min(withoutTransitivityLowFunciton(host), withTransitivityMediumFunciton(host));
        return (miu - OUTPUT_VERY_LOW) / (OUTPUT_LOW - OUTPUT_VERY_LOW);
    }
    
    private double outputLowLeftShoulderMediumLowFunciton(DTNHost host) {
        double miu;
        miu = Math.min(withoutTransitivityMediumFunciton(host), withTransitivityLowFunciton(host));
        return (OUTPUT_MEDIUM - miu) / (OUTPUT_MEDIUM - OUTPUT_LOW);
    }
    
    private double outputLowRightShoulderMediumLowFunciton(DTNHost host) {
        double miu;
        miu = Math.min(withoutTransitivityMediumFunciton(host), withTransitivityLowFunciton(host));
        return (miu - OUTPUT_VERY_LOW) / (OUTPUT_LOW - OUTPUT_VERY_LOW);
    }
    
    private double outputMediumLeftShoulderLowHighFunciton(DTNHost host) {
        double miu;
        miu = Math.min(withoutTransitivityLowFunciton(host), withTransitivityHighFunciton(host));
        return (OUTPUT_HIGH - miu) / (OUTPUT_HIGH - OUTPUT_MEDIUM);
    }
    
    private double outputMediumRightShoulderLowHighFunciton(DTNHost host) {
        double miu;
        miu = Math.min(withoutTransitivityLowFunciton(host), withTransitivityHighFunciton(host));
        return (miu - OUTPUT_LOW) / (OUTPUT_MEDIUM - OUTPUT_LOW);
    }
    
    private double outputMediumLeftShoulderMediumMediumFunciton(DTNHost host) {
        double miu;
        miu = Math.min(withoutTransitivityMediumFunciton(host), withTransitivityMediumFunciton(host));
        return (OUTPUT_HIGH - miu) / (OUTPUT_HIGH - OUTPUT_MEDIUM);
    }
    
    private double outputMediumRightShoulderMediumMediumFunciton(DTNHost host) {
        double miu;
        miu = Math.min(withoutTransitivityMediumFunciton(host), withTransitivityMediumFunciton(host));
        return (miu - OUTPUT_LOW) / (OUTPUT_MEDIUM - OUTPUT_LOW);
    }
    
    private double outputMediumLeftShoulderHighLowFunciton(DTNHost host) {
        double miu;
        miu = Math.min(withoutTransitivityHighFunciton(host), withTransitivityLowFunciton(host));
        return (OUTPUT_HIGH - miu) / (OUTPUT_HIGH - OUTPUT_MEDIUM);
    }
    
    private double outputMediumRightShoulderHighLowFunciton(DTNHost host) {
        double miu;
        miu = Math.min(withoutTransitivityHighFunciton(host), withTransitivityLowFunciton(host));
        return (miu - OUTPUT_LOW) / (OUTPUT_MEDIUM - OUTPUT_LOW);
    }
    
    private double outputHighLeftShoulderMediumHighFunciton(DTNHost host) {
        double miu;
        miu = Math.min(withoutTransitivityMediumFunciton(host), withTransitivityHighFunciton(host));
        return (OUTPUT_VERY_HIGH - miu) / (OUTPUT_VERY_HIGH - OUTPUT_HIGH );
    }
    
    private double outputHighRightShoulderMediumHighFunciton(DTNHost host) {
        double miu;
        miu = Math.min(withoutTransitivityMediumFunciton(host), withTransitivityHighFunciton(host));
        return (miu - OUTPUT_MEDIUM) / (OUTPUT_HIGH - OUTPUT_MEDIUM);
    }
    
    private double outputHighLeftShoulderHighMediumFunciton(DTNHost host) {
        double miu;
        miu = Math.min(withoutTransitivityHighFunciton(host), withTransitivityMediumFunciton(host));
        return (OUTPUT_VERY_HIGH - miu) / (OUTPUT_VERY_HIGH - OUTPUT_HIGH );
    }
    
    private double outputHighRightShoulderHighMediumFunciton(DTNHost host) {
        double miu;
        miu = Math.min(withoutTransitivityHighFunciton(host), withTransitivityMediumFunciton(host));
        return (miu - OUTPUT_MEDIUM) / (OUTPUT_HIGH - OUTPUT_MEDIUM);
    }
    
    private double outputVeryHighFunction(DTNHost host){
        double miu;
        miu = Math.min(withoutTransitivityHighFunciton(host), withTransitivityHighFunciton(host));
        if (miu == 1.0) {
            double xVeryLow = 0;
            /**
             * The range of the X coordinate for this function is about [0.9,1.0]
             * I sum each of it from 0.9 until 1.0 for getting the X value
             */
            for (double i = 0.9; i <= 1.0; i = i + 0.01) {
                xVeryLow = xVeryLow + i;
            }
            return xVeryLow;
        } else {
            return (miu - OUTPUT_HIGH) / (OUTPUT_VERY_HIGH - OUTPUT_HIGH);
        }
    }
    

}
