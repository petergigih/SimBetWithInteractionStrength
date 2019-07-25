package routing.simbet;

import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import routing.community.*;


import java.util.*;
/**
 * @SimBet By Elisabeth Kusuma
 * @FairRouting By Bima Kharisma
 *
 * @Merged By Peter.G(155314027)
 */

public class SimBetWithFairRoutingNewFair implements RoutingDecisionEngine, SimilarityDetectionEngine {

    public static final String CENTRALITY_ALG_SETTING = "centralityAlg";
    public static final String SIMILARITY_SETTING = "similarityAlg";
    public static final String A_SETTING = "a";
    public static final String ROUTER_SIGMA = "shortTermR";
    public static final String ROUTER_LAMBDA = "longTermR";
    public static String SIGMA = "Sigma";
    public static String LAMBDA = "Lambda";
    public static String TIME = "Time";

    protected Map<DTNHost, Set<DTNHost>> neighboursNode; // menyimpan daftar tetangga dari ego node
    protected Map<DTNHost, Map<String, Double>> nodeHistory;
    private Map<DTNHost, Double> utilityForDestination;
    private double routerSigma;
    private double routerLambda;

    protected double[][] matrixEgoNetwork; // menyimpan nilai matrix ego network
    protected double[][] indirectNodeMatrix; //menyimpan matrix indirect node

    protected double betweennessCentrality;// menyimpan nilai betweenness centrality

    protected double a; //menyimpan konstanta untuk variabel similarity
    protected double b = 1 - a; //menyimpan konstanta untuk variabel betweenness

    ArrayList<DTNHost> indirectNode, directNode; //menyimpan indirect node => m dan direct node+host => n

    protected SimilarityCounterImprovedNEWFAIR similarity;
    protected CentralityDetection centrality;


    public SimBetWithFairRoutingNewFair(Settings s) {

        if (s.contains(CENTRALITY_ALG_SETTING))
            this.centrality = (CentralityDetection) s.createIntializedObject(s.getSetting(CENTRALITY_ALG_SETTING));
        else
            this.centrality = new BetweennessCentrality(s);

        if (s.contains(SIMILARITY_SETTING))
            this.similarity = (SimilarityCounterImprovedNEWFAIR) s.createIntializedObject(s.getSetting(SIMILARITY_SETTING));
        else
            this.similarity = new NeighbourhoodSimilarityImprovedNEWFAIR(s);

        this.a = s.getDouble(A_SETTING);

        if (s.contains(ROUTER_SIGMA)) {
            this.routerSigma = s.getDouble(ROUTER_SIGMA);
        } else {
            this.routerSigma = 0.6; //default
        }
        if (s.contains(ROUTER_LAMBDA)) {
            this.routerLambda = s.getDouble(ROUTER_LAMBDA);
        } else {
            this.routerLambda = 0.5; //default
        }
        nodeHistory = new HashMap<>();
        utilityForDestination = new HashMap<>();
    }

    public SimBetWithFairRoutingNewFair(SimBetWithFairRoutingNewFair proto) {

        neighboursNode = new HashMap<DTNHost, Set<DTNHost>>();
        indirectNode = new ArrayList<DTNHost>();
        directNode = new ArrayList<DTNHost>();
        this.a = proto.a;
        this.centrality = proto.centrality.replicate();
        this.similarity = proto.similarity.replicate();
        this.routerSigma = proto.routerSigma;
        this.routerLambda = proto.routerLambda;
        nodeHistory = new HashMap<>();
        utilityForDestination = new HashMap<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        double thisTime = SimClock.getTime();
        DTNHost myHost = con.getOtherNode(peer);
        SimBetWithFairRoutingNewFair de = this.getOtherDecisionEngine(peer);

        if (this.neighboursNode.containsKey(peer) && this.nodeHistory.containsKey(peer)) { // jika node sudah pernah bertemu sebelumnya
            de.neighboursNode.replace(myHost, this.neighboursNode.keySet());
            this.neighboursNode.replace(peer, de.neighboursNode.keySet());
            Map<String, Double> aggregatedComponent;
            for (Map.Entry<DTNHost, Map<String, Double>> entry : nodeHistory.entrySet()) {
                DTNHost key = entry.getKey();
                aggregatedComponent = entry.getValue();

                double sigmaBefore = aggregatedComponent.get(SIGMA);
                double lambdaBefore = aggregatedComponent.get(LAMBDA);
                if (key == peer) {
                    aggregatedComponent.put(SIGMA, sigmaBefore + 1);
                    aggregatedComponent.put(LAMBDA, lambdaBefore + 1);
                    aggregatedComponent.put(TIME, thisTime);
                    this.nodeHistory.put(key, aggregatedComponent);
                } else {
                    double timeBefore = aggregatedComponent.get(TIME);
                    double thisLambda = lambdaBefore * (Math.pow(Math.E, (-(this.routerLambda) * (thisTime - timeBefore))));
                    double thisSigma = sigmaBefore * (Math.pow(Math.E, (-(this.routerSigma) * (thisTime - timeBefore))));

                    aggregatedComponent.put(LAMBDA, thisLambda);
                    aggregatedComponent.put(SIGMA, thisSigma);

                    this.nodeHistory.put(key, aggregatedComponent);
                }
            }

        } else if (!this.neighboursNode.containsKey(peer) && !this.nodeHistory.containsKey(peer)) { // jika node baru pertama kali ditemui
            de.neighboursNode.put(myHost, this.neighboursNode.keySet());
            this.neighboursNode.put(peer, de.neighboursNode.keySet());

            Map<String, Double> aggregatedComponent = new HashMap<>();
            aggregatedComponent.put(LAMBDA, 0.0);
            aggregatedComponent.put(SIGMA, 0.0);
            aggregatedComponent.put(TIME, thisTime);
            this.nodeHistory.put(peer, aggregatedComponent);
        }

        this.updateBetweenness(myHost); // mengupdate nilai betweenness
        this.updateSimilarity(myHost); //mengupdate indirect node
        updatePerceiveInteractionStrengths(myHost, peer);
    }

    private void updatePerceiveInteractionStrengths(DTNHost thisHost, DTNHost peer) {
        SimBetWithFairRoutingNewFair partner = getOtherDecisionEngine(peer);

        for (Message m : thisHost.getMessageCollection()) {
            double myLambda = 0, mySigma = 0;
            double partnerLambda = 0, partnerSigma = 0;
            double utility = 0;
            if (this.nodeHistory.containsKey(m.getTo())) {
                myLambda = this.nodeHistory.get(m.getTo()).get(LAMBDA);
                mySigma = this.nodeHistory.get(m.getTo()).get(SIGMA);
            }
            if (partner.nodeHistory.containsKey(m.getTo())) {
                partnerLambda = partner.nodeHistory.get(m.getTo()).get(LAMBDA);
                partnerSigma = partner.nodeHistory.get(m.getTo()).get(SIGMA);
            }

            if ((myLambda + partnerLambda) > 0) {

                double myInteractStrength = this.countAgrIntStrength(myLambda, mySigma);
//                System.out.println("My Int = " +myInteractStrength);
                double partnerInteractStrength = partner.countAgrIntStrength(partnerLambda, partnerSigma);

                utility = this.countUtilityThroughPeer(partnerInteractStrength, myInteractStrength);
                this.utilityForDestination.put(m.getTo(), utility);
            } else {
                this.utilityForDestination.put(m.getTo(), 0.0);
            }

        }
    }

    private double countAgrIntStrength(double lambda, double sigma) {
        return lambda * (lambda - sigma);
    }

    private double countUtilityThroughPeer(double partnerInteractStrength, double myInteractStrength) {
        return partnerInteractStrength / (partnerInteractStrength + myInteractStrength);
    }


    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {

        SimBetWithFairRoutingNewFair de = getOtherDecisionEngine(otherHost);
        DTNHost dest = m.getTo();

        if (isFinalDest(m, otherHost))
            return true;

        //hitung nilai simbet util saya
        double mySimbetUtil = this.countSimBetUtil(de.getSimilarity(dest), de.getBetweennessCentrality(),
                this.getSimilarity(dest), this.getBetweennessCentrality());
//        System.out.println( "--MYSIMBETUTIL = " +mySimbetUtil);
        //hitung nilai simbet util teman saya
        double peerSimBetUtil = this.countSimBetUtil(this.getSimilarity(dest), this.getBetweennessCentrality(),
                de.getSimilarity(dest), de.getBetweennessCentrality());
        // System.out.println(mySimbetUtil+" and "+peerSimBetUtil+" exchange their summary vector ");
//        System.out.println("PEERSIMBETUTIL = "+peerSimBetUtil);
        /*routing dengan kombinasi similarity & betweenness*/
        if (peerSimBetUtil > mySimbetUtil)
            return true;
        else
            return false;
    }


    // ambil nilai similarity ke node dest

    @Override
    public double getSimilarity(DTNHost dest) {

        int index = 0; //digunakan untuk membantu penghitungan index

        //cek apakah node dest merupakan direct node
        if (this.directNode.contains(dest)) {
            for (DTNHost dtnHost : this.directNode) {

                if (dtnHost == dest) {
                    return this.similarity.countSimilarity(this.matrixEgoNetwork, null, index);
                }
                index++;
            }
        }

        //cek apakah node dest merupakan indirect node
        if (this.indirectNode.contains(dest)) {

            //bangun matrix adjacency indirect node
            this.buildIndirectNodeMatrix(this.neighboursNode, dest);

            //hitung nilai similarity
            return this.similarity.countSimilarity(this.matrixEgoNetwork, this.indirectNodeMatrix, 0);

        }

        return 0;
    }

    // mengambil nilai betweenness yang sudah dihitung
    public double getBetweennessCentrality() {
        return this.betweennessCentrality;
    }

    // mengupdate nilai betweenness centrality
    protected void updateBetweenness(DTNHost myHost) {
        this.buildEgoNetwork(this.neighboursNode, myHost); // membangun ego network
        this.betweennessCentrality = this.centrality.getCentrality(this.matrixEgoNetwork); //menghitung nilai betweenness centrality
    }

    protected void updateSimilarity(DTNHost myHost) {

        //simpan data indirect node
        this.indirectNode.addAll(this.searchIndirectNeighbours(this.neighboursNode));

    }

    protected Set<DTNHost> searchIndirectNeighbours(Map<DTNHost, Set<DTNHost>> neighboursNode) {

        // mengambil daftar tetangga yang sudah ditemui secara langsung
        Set<DTNHost> directNeighbours = neighboursNode.keySet();

        // variabel untuk menyimpan daftar node yang belum pernah ditemui secara
        // langsung
        Set<DTNHost> setOfIndirectNeighbours = new HashSet<>();

        for (DTNHost dtnHost : directNeighbours) {

            // mengambil daftar tetangga dari peer yang sudah ditemui langsung
            Set<DTNHost> neighboursOfpeer = neighboursNode.get(dtnHost);

            for (DTNHost dtnHost1 : neighboursOfpeer) {

                // jika dtnHost1 belum pernah ditemui secara langsung
                if (!directNeighbours.contains(dtnHost1)) {

                    // cek apakah listOfUndirectNeighbours masih kosong
                    if (setOfIndirectNeighbours.isEmpty()) {

                        // jika masih kosong masukkan langsung dtnHost1 ke dalam
                        // listOfIndirectNeighbours
                        setOfIndirectNeighbours.add(dtnHost1);

                    } else {// jika listOfUndirectNeighbours tidak kosong

                        // cek apakah dtnHost1 sudah pernah dicatat ke dalam
                        // listOfindirectNeighbours
                        if (!setOfIndirectNeighbours.contains(dtnHost1)) {
                            setOfIndirectNeighbours.add(dtnHost1);
                        }
                    }
                }
            }
        }

        return setOfIndirectNeighbours;
    }

    // method yang digunakan untuk membangun matriks ego network
    public void buildIndirectNodeMatrix(Map<DTNHost, Set<DTNHost>> neighboursNode, DTNHost dest) {
        ArrayList<DTNHost> dummyArrayN = this.directNode;


        double[][] neighboursAdj = new double[dummyArrayN.size()][1];

        for (int i = 0; i < dummyArrayN.size(); i++) {
            for (int j = 0; j < 1; j++) {
                if (i == 0) {
                    neighboursAdj[i][j] = 0;
                } else if (neighboursNode.get(dummyArrayN.get(i)).contains(dest)) {
                    neighboursAdj[i][j] = this.countAggrIntStrength(nodeHistory);;

                } else {
                    neighboursAdj[i][j] = 0;

                }
            }
        }

        this.indirectNodeMatrix = neighboursAdj;
    }

    // method yang digunakan untuk membangun matriks ego network
    protected void buildEgoNetwork(Map<DTNHost, Set<DTNHost>> neighboursNode, DTNHost host) {
        ArrayList<DTNHost> dummyArray = buildDummyArray(neighboursNode, host);

        double[][] neighboursAdj = new double[dummyArray.size()][dummyArray.size()];

        for (int i = 0; i < dummyArray.size(); i++) {
            for (int j = i; j < dummyArray.size(); j++) {
                if (i == j) {
                    neighboursAdj[i][j] = 0;
                } else if (neighboursNode.get(dummyArray.get(j)).contains(dummyArray.get(i))) {
                    neighboursAdj[i][j] = this.countAggrIntStrength(nodeHistory);
                    neighboursAdj[j][i] = neighboursAdj[i][j];
//                    System.out.println("count = " + +neighboursAdj[i][j]);
                } else {
                    neighboursAdj[i][j] = 0;
                    neighboursAdj[j][i] = neighboursAdj[i][j];
                }
            }
        }

        this.matrixEgoNetwork = neighboursAdj;

    }

    protected double countAggrIntStrength(Map<DTNHost, Map<String,Double>> nodeHistory) {

        double thisHostForOther = 0;
        for (Map.Entry<DTNHost, Map<String, Double>> entry : this.nodeHistory.entrySet()) {
            Map<String, Double> value = entry.getValue();
            double otherLambda = value.get(LAMBDA);
            double otherSigma = value.get(SIGMA);
            thisHostForOther = countAgrIntStrength(otherLambda, otherSigma);

        }
        return thisHostForOther;
    }


    protected ArrayList<DTNHost> buildDummyArray(Map<DTNHost, Set<DTNHost>> neighbours, DTNHost myHost) {
        ArrayList<DTNHost> dummyArray = new ArrayList<>();
        dummyArray.add(myHost);
        dummyArray.addAll(neighbours.keySet());
        this.directNode = dummyArray; //mengisi himpunan n pada matrix
        return dummyArray;
    }

    protected double countSimBetUtil(double simPeerForDest, double betweennessPeer, double mySimForDest, double myBetweenness ){
        double simBetUtil, simUtilForDest, betUtil;

        simUtilForDest=mySimForDest/(mySimForDest+simPeerForDest);

        if (Double.toString(simUtilForDest) == "NaN"){
            simUtilForDest = 0;
        }

        betUtil = myBetweenness / (myBetweenness + betweennessPeer);

        if (Double.toString(betUtil) == "NaN"){
            betUtil = 0;
        }
//        System.out.println("simUtil = " + simUtilForDest);
//        System.out.println("betUtil = " + betUtil);
        simBetUtil = (this.a*simUtilForDest) + ((1-this.a)*betUtil);
//        System.out.println("simBetUtil = "+simBetUtil);
        return simBetUtil;
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
        // TODO Auto-generated method stub
        return m.getTo() != thisHost;
    }

    private SimBetWithFairRoutingNewFair getOtherDecisionEngine(DTNHost otherHost) {
        MessageRouter otherRouter = otherHost.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (SimBetWithFairRoutingNewFair) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
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
        return new SimBetWithFairRoutingNewFair(this);
    }

}