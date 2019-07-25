/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.fuzzy;

import core.*;
import java.util.HashMap;
import java.util.Map;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 * @author Gregorius Bima, Sanata Dharma Univeristy
 */
public class ConventionalFuzzyRouter implements RoutingDecisionEngine {

    /** For setting the fuzzy input set */
    public static final String ENCOUNTER_TIME_1_SETTING = "encounterTime1";
    public static final String ENCOUNTER_TIME_2_SETTING = "encounterTime2";
    public static final String ENCOUNTER_TIME_3_SETTING = "encounterTime3";
    public static final String INTERMITTENT_ENCOUNTER_TIME_1_SETTING = "intermittentEncounterTime1";
    public static final String INTERMITTENT_ENCOUNTER_TIME_2_SETTING = "intermittentEncounterTime2";
    public static final String INTERMITTENT_ENCOUNTER_TIME_3_SETTING = "intermittentEncounterTime3";

    /** The default value of the setting for fuzzy works */
    protected static final double DEFAULT_ENCOUNTER_TIME_1 = 0,
            DEFAULT_ENCOUNTER_TIME_2 = 0,
            DEFAULT_ENCOUNTER_TIME_3 = 0,
            DEFAULT_INTERMITTENT_ENCOUNTER_TIME_1 = 0,
            DEFAULT_INTERMITTENT_ENCOUNTER_TIME_2 = 0,
            DEFAULT_INTERMITTENT_ENCOUNTER_TIME_3 = 0;

    /** The output value of the fuzzy set */
    protected static final double DELIVERY_VALUE_VERY_VERY_LOW = 0.2,
            DELIVERY_VALUE_VERY_LOW = 0.2,
            DELIVERY_VALUE_LOW = 0.2,
            DELIVERY_VALUE_LESS_MEDIUM = 0.2,
            DELIVERY_VALUE_MEDIUM = 0.2,
            DELIVERY_VALUE_MORE_MEDIUM = 0.2,
            DELIVERY_VALUE_HIGH = 0.2,
            DELIVERY_VALUE_VERY_HIGH = 0.2,
            DELIVERY_VALUE_VERY_VERY_HIGH = 0.2;

    protected double encounter1;
    protected double encounter2;
    protected double encounter3;
    protected double intermittent1;
    protected double intermittent2;
    protected double intermittent3;
    protected double deliveryValueVeryVeryLow;
    protected double deliveryValueVeryLow;
    protected double deliveryValueLow;
    protected double deliveryValueLessMedium;
    protected double deliveryValueMedium;
    protected double deliveryValueMoreMedium;
    protected double deliveryValueHigh;
    protected double deliveryValueVeryHigh;
    protected double deliveryValueVeryVeryHigh;

    /** The timer capture for input value */
    protected Map<DTNHost, Double> connectionUpEncounterTimer;
    protected Map<DTNHost, Double> connectionDownEncounterTimer;
    protected Map<DTNHost, Double> connectionUpIntermittentEncounterTimer;
    protected Map<DTNHost, Double> connectionDownIntermittentEncounterTimer;

    /** The input value */
    protected Map<DTNHost, Double> encounterTime;
    protected Map<DTNHost, Double> intermittentEncounterTime;
    /** The fuzzy value from defuzzification */
    protected Map<DTNHost, Double> deliveryValue;

    public ConventionalFuzzyRouter(Settings setting) {
        if (setting.contains(ENCOUNTER_TIME_1_SETTING)) {
            encounter1 = setting.getDouble(ENCOUNTER_TIME_1_SETTING);
        } else {
            encounter1 = DEFAULT_INTERMITTENT_ENCOUNTER_TIME_1;
        }

        if (setting.contains(ENCOUNTER_TIME_2_SETTING)) {
            encounter2 = setting.getDouble(ENCOUNTER_TIME_2_SETTING);
        } else {
            encounter2 = DEFAULT_INTERMITTENT_ENCOUNTER_TIME_2;
        }

        if (setting.contains(ENCOUNTER_TIME_3_SETTING)) {
            encounter3 = setting.getDouble(ENCOUNTER_TIME_3_SETTING);
        } else {
            encounter3 = DEFAULT_INTERMITTENT_ENCOUNTER_TIME_3;
        }

        if (setting.contains(INTERMITTENT_ENCOUNTER_TIME_1_SETTING)) {
            intermittent1 = setting.getDouble(INTERMITTENT_ENCOUNTER_TIME_1_SETTING);
        } else {
            intermittent1 = DEFAULT_INTERMITTENT_ENCOUNTER_TIME_1;
        }

        if (setting.contains(INTERMITTENT_ENCOUNTER_TIME_2_SETTING)) {
            intermittent2 = setting.getDouble(INTERMITTENT_ENCOUNTER_TIME_2_SETTING);
        } else {
            intermittent2 = DEFAULT_INTERMITTENT_ENCOUNTER_TIME_2;
        }

        if (setting.contains(INTERMITTENT_ENCOUNTER_TIME_3_SETTING)) {
            intermittent3 = setting.getDouble(INTERMITTENT_ENCOUNTER_TIME_3_SETTING);
        } else {
            intermittent3 = DEFAULT_INTERMITTENT_ENCOUNTER_TIME_3;
        }

        deliveryValueVeryVeryLow = DELIVERY_VALUE_VERY_VERY_LOW;
        deliveryValueVeryLow = DELIVERY_VALUE_VERY_LOW;
        deliveryValueLow = DELIVERY_VALUE_LOW;
        deliveryValueLessMedium = DELIVERY_VALUE_LESS_MEDIUM;
        deliveryValueMedium = DELIVERY_VALUE_MEDIUM;
        deliveryValueMoreMedium = DELIVERY_VALUE_MORE_MEDIUM;
        deliveryValueHigh = DELIVERY_VALUE_HIGH;
        deliveryValueVeryHigh = DELIVERY_VALUE_VERY_HIGH;
        deliveryValueVeryVeryHigh = DELIVERY_VALUE_VERY_VERY_HIGH;

        
        connectionUpEncounterTimer = new HashMap<DTNHost, Double>();
        connectionDownEncounterTimer = new HashMap<DTNHost, Double>();
        connectionUpIntermittentEncounterTimer = new HashMap<DTNHost, Double>();
        connectionDownIntermittentEncounterTimer = new HashMap<DTNHost, Double>();

        encounterTime = new HashMap<DTNHost, Double>();
        intermittentEncounterTime = new HashMap<DTNHost, Double>();
        deliveryValue = new HashMap<DTNHost, Double>();
    }

    public ConventionalFuzzyRouter(ConventionalFuzzyRouter prototype) {
        deliveryValueVeryVeryLow = prototype.deliveryValueVeryVeryLow;
        deliveryValueVeryLow = prototype.deliveryValueVeryLow;
        deliveryValueLow = prototype.deliveryValueLow;
        deliveryValueLessMedium = prototype.deliveryValueLessMedium;
        deliveryValueMedium = prototype.deliveryValueMedium;
        deliveryValueMoreMedium = prototype.deliveryValueMoreMedium;
        deliveryValueHigh = prototype.deliveryValueHigh;
        deliveryValueVeryHigh = prototype.deliveryValueVeryHigh;
        deliveryValueVeryVeryHigh = prototype.deliveryValueVeryVeryHigh;

        connectionUpEncounterTimer = new HashMap<DTNHost, Double>();
        connectionDownEncounterTimer = new HashMap<DTNHost, Double>();
        connectionUpIntermittentEncounterTimer = new HashMap<DTNHost, Double>();
        connectionDownIntermittentEncounterTimer = new HashMap<DTNHost, Double>();

        encounterTime = new HashMap<DTNHost, Double>();
        intermittentEncounterTime = new HashMap<DTNHost, Double>();
        deliveryValue = new HashMap<DTNHost, Double>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        ConventionalFuzzyRouter myPartner = getOtherProphetDecisionEngine(peer);

        /** Put the encounter time when the pair of nodes meet up */
        if (!this.connectionUpEncounterTimer.containsKey(peer)
                && !myPartner.connectionUpEncounterTimer.containsKey(thisHost)) {
            this.connectionUpEncounterTimer.put(peer, SimClock.getTime());
            myPartner.connectionUpEncounterTimer.put(thisHost, SimClock.getTime());
        } else {
            this.connectionUpEncounterTimer.replace(peer, SimClock.getTime());
            myPartner.connectionUpEncounterTimer.replace(thisHost, SimClock.getTime());
        }

        /** Put the intermittent time when the pair of nodes meet up */
        if (!this.connectionUpIntermittentEncounterTimer.containsKey(peer)
                && !myPartner.connectionUpIntermittentEncounterTimer.containsKey(thisHost)) {
            this.connectionUpIntermittentEncounterTimer.put(peer, SimClock.getTime());
            myPartner.connectionUpIntermittentEncounterTimer.put(thisHost, SimClock.getTime());
        } else {
            this.connectionUpIntermittentEncounterTimer.replace(peer, SimClock.getTime());
            myPartner.connectionUpIntermittentEncounterTimer.replace(thisHost, SimClock.getTime());
        }
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        ConventionalFuzzyRouter myPartner = getOtherProphetDecisionEngine(peer);

        /** Put the encounter time when the pair of nodes meet up */
        if (!this.connectionDownEncounterTimer.containsKey(peer)
                && !myPartner.connectionDownEncounterTimer.containsKey(thisHost)) {
            this.connectionDownEncounterTimer.put(peer, SimClock.getTime());
            myPartner.connectionDownEncounterTimer.put(thisHost, SimClock.getTime());
        } else {
            this.connectionDownEncounterTimer.replace(peer, SimClock.getTime());
            myPartner.connectionDownEncounterTimer.replace(thisHost, SimClock.getTime());
        }

        /** Put the intermittent time when the pair of nodes meet up */
        if (!this.connectionUpIntermittentEncounterTimer.containsKey(peer)
                && !myPartner.connectionUpIntermittentEncounterTimer.containsKey(thisHost)) {
            this.connectionUpIntermittentEncounterTimer.put(peer, SimClock.getTime());
            myPartner.connectionUpIntermittentEncounterTimer.put(thisHost, SimClock.getTime());
        } else {
            this.connectionUpIntermittentEncounterTimer.replace(peer, SimClock.getTime());
            myPartner.connectionUpIntermittentEncounterTimer.replace(thisHost, SimClock.getTime());
        }
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost thisHost = con.getOtherNode(peer);
        ConventionalFuzzyRouter myPartner = getOtherProphetDecisionEngine(peer);
        
        /** This and partner compute the encounter time */
        this.setEncounterTimeFor(thisHost);
        myPartner.setEncounterTimeFor(peer);
        /** This and partner compute the intermittent time */
        this.setIntermittentEncounterTimeFor(thisHost);
        myPartner.setIntermittentEncounterTimeFor(peer);
        /** Compute the defuzzification */
        this.computeDefuzzificationFor(thisHost);
        myPartner.computeDefuzzificationFor(peer);
    }

    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return true;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        ConventionalFuzzyRouter myPartner = getOtherProphetDecisionEngine(otherHost);
        if (m.getTo() == otherHost) {
            return true;
        }
        
        return this.getDeliveryValueFor(m.getTo()) < myPartner.getDeliveryValueFor(m.getTo());
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return false;
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new ConventionalFuzzyRouter(this);
    }
    
    private ConventionalFuzzyRouter getOtherProphetDecisionEngine(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (ConventionalFuzzyRouter) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }
    
    private void computeDefuzzificationFor(DTNHost host){
        double defuzzification = ((outputVeryVeryLowFunction(host) * deliveryValueVeryVeryLow)+
                (outputVeryLowFunciton(host)* deliveryValueVeryLow)+(outputLowFunciton(host)* deliveryValueLow)+
                (outputLessMediumFunciton(host)* deliveryValueLessMedium)+(outputMediumFunciton(host)* deliveryValueMedium)+
                (outputMoreMediumFunciton(host)* deliveryValueMoreMedium)+(outputHighFunciton(host)* deliveryValueHigh)+
                (outputVeryHighFunciton(host)* deliveryValueVeryHigh)+(outputVeryVeryHighFunction(host)* deliveryValueVeryVeryHigh))
                / sumOfTheMiuFor(host);
        deliveryValue.put(host, defuzzification);
     }
    private double sumOfTheMiuFor(DTNHost host){
        return (outputVeryVeryLowFunction(host) + outputVeryLowFunciton(host) +
                outputLowFunciton(host) + outputLessMediumFunciton(host) + 
                outputMediumFunciton(host) + outputMoreMediumFunciton(host) + 
                outputHighFunciton(host) + outputVeryHighFunciton(host) + 
                outputVeryVeryHighFunction(host));
    }
    
    private void setEncounterTimeFor(DTNHost host){
        double encounter = getConnectionUpEncounterTimeFor(host) - getConnectionDownEncounterTimeFor(host);
        encounterTime.put(host, encounter);
    }
    
    private void setIntermittentEncounterTimeFor(DTNHost host){
        double intermittent = getConnectionUpIntermittentEncounterTimeFor(host) - getConnectionDownIntermittentEncounterTimeFor(host);
        intermittentEncounterTime.put(host, intermittent);
    }
    
    private double getConnectionUpEncounterTimeFor(DTNHost host){
        if (connectionUpEncounterTimer.containsKey(host)) {
            return connectionUpEncounterTimer.get(host);
        } else {
            return 0;
        }
    }
    
    private double getConnectionUpIntermittentEncounterTimeFor(DTNHost host){
        if (connectionUpIntermittentEncounterTimer.containsKey(host)) {
            return connectionUpIntermittentEncounterTimer.get(host);
        } else {
            return 0;
        }
    }
    
    private double getConnectionDownEncounterTimeFor(DTNHost host){
        if (connectionDownEncounterTimer.containsKey(host)) {
            return connectionDownEncounterTimer.get(host);
        } else {
            return 0;
        }
    }
    
    private double getConnectionDownIntermittentEncounterTimeFor(DTNHost host){
        if (connectionDownIntermittentEncounterTimer.containsKey(host)) {
            return connectionDownIntermittentEncounterTimer.get(host);
        } else {
            return 0;
        }
    }
    
    private double getEncounterTimeFor(DTNHost host){
        if (encounterTime.containsKey(host)) {
            return encounterTime.get(host);
        } else {
            return 0;
        }
    }
    
    private double getIntermittentEncounterTimeFor(DTNHost host){
        if (intermittentEncounterTime.containsKey(host)) {
            return intermittentEncounterTime.get(host);
        } else {
            return 0;
        }
    }
    
    private double getDeliveryValueFor(DTNHost host){
        if (deliveryValue.containsKey(host)) {
            return deliveryValue.get(host);
        } else {
            return 0;
        }
    }

    private double encounterTimeLowFunciton(DTNHost host) {
        if (encounter1 == getEncounterTimeFor(host)) return 1.0;
        else if (encounter1 < getEncounterTimeFor(host) && getEncounterTimeFor(host) < encounter2) 
            return (encounter2 - getEncounterTimeFor(host)) / (encounter2 - encounter1); 
        else return 0;
    }

    private double encounterTimeMediumFunciton(DTNHost host) {
        if (encounter1 < getEncounterTimeFor(host) && getEncounterTimeFor(host) < encounter2 )
            return (getEncounterTimeFor(host) - encounter1)/(encounter2 - encounter1);
        else if (encounter2 < getEncounterTimeFor(host) && getEncounterTimeFor(host) < encounter3 )
            return (encounter3 - getEncounterTimeFor(host))/(encounter3 - encounter2);
        else return 0;
    }

    private double encounterTimeHighFunciton(DTNHost host) {
        if (encounter2 < getEncounterTimeFor(host) && getEncounterTimeFor(host) < encounter3) 
            return (getEncounterTimeFor(host) - encounter2)/(encounter3 - encounter2);
        else if (getEncounterTimeFor(host) == encounter3) return 1;
        else return 0;
    }

    private double intermittentEncounterTimeLowFunciton(DTNHost host) {
        if (intermittent1 == getIntermittentEncounterTimeFor(host)) return 1.0;
        else if (intermittent1 < getIntermittentEncounterTimeFor(host) && getIntermittentEncounterTimeFor(host) < intermittent2)
            return (intermittent2 - getIntermittentEncounterTimeFor(host)) / (intermittent2 - intermittent1); 
        else return 0;
    }

    private double intermittentEncounterTimeMediumFunciton(DTNHost host) {
        if (intermittent1 < getIntermittentEncounterTimeFor(host) && getIntermittentEncounterTimeFor(host) < intermittent2 )
            return (getIntermittentEncounterTimeFor(host) - intermittent1)/(intermittent2 - intermittent1);
        else if (intermittent2 < getIntermittentEncounterTimeFor(host) && getIntermittentEncounterTimeFor(host) < intermittent3 ) 
            return (intermittent3 - getIntermittentEncounterTimeFor(host))/(intermittent3 - intermittent2);
        else return 0;
    }

    private double intermittentEncounterTimeHighFunciton(DTNHost host) {
        if (intermittent2 < getIntermittentEncounterTimeFor(host) && getIntermittentEncounterTimeFor(host) < intermittent3)
            return (getIntermittentEncounterTimeFor(host) - intermittent2)/(intermittent3 - intermittent2);
        else if (getIntermittentEncounterTimeFor(host) == intermittent3) return 1;
        else return 0;
    }

    private double outputVeryVeryLowFunction(DTNHost host){
        return Math.min(encounterTimeLowFunciton(host), intermittentEncounterTimeLowFunciton(host));
    }
    
    private double outputVeryLowFunciton(DTNHost host) {
        return Math.min(encounterTimeLowFunciton(host), intermittentEncounterTimeMediumFunciton(host));
    }
    
    private double outputLowFunciton(DTNHost host) {
        return Math.min(encounterTimeMediumFunciton(host), intermittentEncounterTimeLowFunciton(host));
    }
    
    private double outputLessMediumFunciton(DTNHost host) {
        return Math.min(encounterTimeLowFunciton(host), intermittentEncounterTimeHighFunciton(host));
    }
    
    private double outputMediumFunciton(DTNHost host) {
        return Math.min(encounterTimeHighFunciton(host), intermittentEncounterTimeLowFunciton(host));
    }
    
    private double outputMoreMediumFunciton(DTNHost host) {
        return Math.min(encounterTimeMediumFunciton(host), intermittentEncounterTimeMediumFunciton(host));
    }
    
    private double outputHighFunciton(DTNHost host) {
        return Math.min(encounterTimeMediumFunciton(host), intermittentEncounterTimeHighFunciton(host));
    }
    
    private double outputVeryHighFunciton(DTNHost host) {
        return Math.min(encounterTimeHighFunciton(host), intermittentEncounterTimeMediumFunciton(host));
    }
    
    private double outputVeryVeryHighFunction(DTNHost host){
        return Math.min(encounterTimeHighFunciton(host), intermittentEncounterTimeHighFunciton(host));
    }

}
