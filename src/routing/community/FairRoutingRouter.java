/*
 * 
 *
 * Copyright 2010 by University of Pittsburgh, released under GPLv3.
 * 
 */
package routing.community;

import java.util.*;

import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 */
public class FairRoutingRouter implements RoutingDecisionEngine {

    /**
     * short term -setting id {@value}
     */
    public static final String R_SIGMA = "shortTermR";

    /**
     * long term -setting id {@value}
     */
    public static final String R_LAMBDA = "longTermR";
    private static final Double R_LAMBDA_DEFAULT = 0.1;
    private static final Double R_SIGMA_DEFAULT = 0.2;

    protected Map<DTNHost, ArrayList<Double>> neighborsHistory;

    private double rSigma; //eksponential rate short term
    private double rLambda; //eksponential rate long term

    double util; //nilai util
    double s_ik, //interaction strength node i to k
            s_jk, //interaction strength node j to k
            lambda_ik = 0, //
            lambda_jk = 0,
            sigma_ik = 0,
            sigma_jk = 0;

    /**
     *
     * @param s Settings to configure the object
     */
    public FairRoutingRouter(Settings s) {
        if (s.contains(R_SIGMA)) {
            this.rSigma = s.getDouble(R_SIGMA);
        } else {
            this.rSigma = R_SIGMA_DEFAULT;
        }

        if (s.contains(R_LAMBDA)) {
            this.rLambda = s.getDouble(R_LAMBDA);
        } else {
            this.rLambda = R_LAMBDA_DEFAULT;
        }
    }

    /**
     *
     * @param proto Prototype DistributedBubbleRap upon which to base this
     * object
     */
    public FairRoutingRouter(FairRoutingRouter proto) {
        this.rLambda = proto.rLambda;
        this.rSigma = proto.rSigma;
        neighborsHistory = new HashMap<DTNHost, ArrayList<Double>>();
    }

    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    /**
     * @see RoutingDecisionEngine#doExchangeForNewConnection(Connection,
     * DTNHost)
     */
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        FairRoutingRouter de = this.getOtherDecisionEngine(peer);

        double sigma = 0;
        double lambda = 0;
        double time = 0;

        //jika node baru ditemui simpan ke neighborsHistory 
        if (!this.neighborsHistory.containsKey(peer)) {

            ArrayList<Double> nodeInformationList = new ArrayList<Double>();
            nodeInformationList.add(lambda);
            nodeInformationList.add(sigma);
            nodeInformationList.add(time);

            this.neighborsHistory.put(peer, nodeInformationList);
            de.neighborsHistory.put(myHost, nodeInformationList);

        }

        //update lambda dan sigma ke semua node kontak
        this.updatePercieveInteractionStrength(peer);

    }

    //update nilai lamda dan sigma 
    protected void updatePercieveInteractionStrength(DTNHost peer) {
        double sigma;
        double lambda;
        double timeLastEncountered;
        double timeNew = SimClock.getTime();

        ArrayList<Double> nodeInformationList;

        for (Map.Entry<DTNHost, ArrayList<Double>> data : this.neighborsHistory.entrySet()) {

            if (data.getKey() == peer) {
                nodeInformationList = data.getValue();
                lambda = nodeInformationList.get(0);
                sigma = nodeInformationList.get(1);

                lambda++;
                sigma++;

                nodeInformationList.set(0, lambda);
                nodeInformationList.set(1, sigma);
                nodeInformationList.set(2, timeNew);

                this.neighborsHistory.replace(data.getKey(), nodeInformationList);
            } else {
                nodeInformationList = data.getValue();
                lambda = nodeInformationList.get(0);
                sigma = nodeInformationList.get(1);
                timeLastEncountered = nodeInformationList.get(2);

                lambda = lambda * (Math.pow(Math.E, (-(this.rLambda) * (timeNew - timeLastEncountered))));
                sigma = sigma * (Math.pow(Math.E, (-(this.rSigma) * (timeNew - timeLastEncountered))));

                nodeInformationList.set(0, lambda);
                nodeInformationList.set(1, sigma);

                this.neighborsHistory.replace(data.getKey(), nodeInformationList);

            }
        }
    }

    public double getUtil() {
        return this.util;
    }

    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    public boolean newMessage(Message m) {
        return true; // Always keep and attempt to forward a created message
    }

    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost; // Unicast Routing
    }

    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    //tambahin control buffer
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        FairRoutingRouter de = getOtherDecisionEngine(otherHost);
        DTNHost dest = m.getTo();

        if (isFinalDest(m, otherHost)) {
            return true;
        } else {

            //cek apakah this host pernah bertemu dest
            if (this.neighborsHistory.containsKey(dest)) {
                lambda_ik = this.neighborsHistory.get(dest).get(0);
                sigma_ik = this.neighborsHistory.get(dest).get(1);
            }

            //cek apakah peer pernah bertemu dest
            if (de.neighborsHistory.containsKey(dest)) {
                lambda_jk = de.neighborsHistory.get(dest).get(0);
                sigma_jk = de.neighborsHistory.get(dest).get(1);
            }

            if ((lambda_ik + lambda_jk) > 0) {
                s_jk = this.countAgrIntStrength(lambda_jk, sigma_jk);
                s_ik = this.countAgrIntStrength(lambda_ik, sigma_ik);
//                System.out.println("sjk = " + s_jk);
                //hitung u_ijk
                util = this.countUtil(s_jk, s_ik);

            } else {

                double sumOf_s_jk = this.countSumOfAgrIntStrength(de.neighborsHistory);
                double sumOf_s_ik = this.countSumOfAgrIntStrength(this.neighborsHistory);
//                System.out.println("sjk = " + sumOf_s_jk);
//                System.out.println("sik = " + sumOf_s_ik );

                //hitung u_ij
                util = this.countUtil(sumOf_s_jk, sumOf_s_ik);
            }

            if (util > 0.5) {
                return true;
            }
        }
        return false;
    }

    protected double countAgrIntStrength(double lambda, double sigma) {
        return lambda * (lambda - sigma);
    }

    protected double countUtil(double s_jk, double s_ik) {
        return s_jk / (s_jk + s_ik);
    }

    protected double countSumOfAgrIntStrength(Map<DTNHost, ArrayList<Double>> neighborsHist) {
        double sumOfIntStrength = 0;
        double lambda = 0, sigma = 0;
        for (Map.Entry<DTNHost, ArrayList<Double>> data : neighborsHist.entrySet()) {
            lambda = data.getValue().get(0);
            sigma = data.getValue().get(1);
//            System.out.println("Lambda = " + lambda);
//            System.out.println("Sigma = " + sigma);
            sumOfIntStrength = sumOfIntStrength + this.countAgrIntStrength(lambda, sigma);
//            System.out.println("SumOfInt = " + sumOfIntStrength);
        }


        return sumOfIntStrength;
    }

    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return true;
    }

    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return false;
    }

    public RoutingDecisionEngine replicate() {
        return new FairRoutingRouter(this);
    }

    private FairRoutingRouter getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (FairRoutingRouter) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

}
