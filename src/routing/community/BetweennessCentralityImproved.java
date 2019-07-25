package routing.community;

import java.util.*;

import core.*;

import javax.naming.NameNotFoundException;

public class BetweennessCentralityImproved implements CentralityDetectionImproved{
	//protected Map<DTNHost, ArrayList<Double>> neighborsHistory;
	public BetweennessCentralityImproved(Settings s) {}

	public BetweennessCentralityImproved(BetweennessCentralityImproved proto) {}

	public double getCentrality(double[][] matrixEgoNetwork) {//di edit kie!
		double[][] ones= new double[matrixEgoNetwork.length][matrixEgoNetwork.length];
		for(double[] ones1 : ones){
			for (int i = 0; i < ones.length; i++) {
				ones1[i]=1;


			}
		}

		double[][] result = matrixMultiplexing(neighboursAdjSquare(matrixEgoNetwork), matrixDecrement(ones, matrixEgoNetwork));


		ArrayList<Double> val= new ArrayList<>();
		double max = 0;
		for (int i = 0; i < result.length; i++) {
			for (int j = i + 1; j < result.length; j++) {

				if (i == 0 && j == i + 1 && result[i][j] != 0) {
					max = result[i][j];
				} else {
					if (max < result[i][j] && result[i][j] != 0) {
						max = result[i][j];
					}
				}
			}
			if (max != 0) { 
				val.add(max);

			}
		}

		double betweennessVal=0;
		for (Double val1 : val) {
			betweennessVal=betweennessVal+(1/val1);

		}

		return betweennessVal;
	}


	public double[][] neighboursAdjSquare(double[][] neighboursAdj){

		double result[][]=new double[neighboursAdj.length][neighboursAdj[0].length];
        for(int i=0;i<result.length;i++)
        {
            for(int j=0;j<result[0].length;j++)
            {
                for(int k=0;k<neighboursAdj[0].length;k++)
                {
                    result[i][j]+=neighboursAdj[i][k]*neighboursAdj[k][j];
                }
            }
        }
        return (result);
	}
	
	public double[][] matrixDecrement(double[][] ones, double[][] neighboursAdj) {
		double[][] result= new double[ones.length][ones.length];
		
		for (int i = 0; i < result.length; i++) {
			for (int j = 0; j < result.length; j++) {
				result[i][j]= ones[i][j]-neighboursAdj[i][j];
			}
		}
		
		return result;
	}
	
	public double[][] matrixMultiplexing(double[][] neighboursAdjSquare, double[][] decrementMatrix) {
		double[][] result= new double[neighboursAdjSquare.length][neighboursAdjSquare.length];
		
		for (int i = 0; i < result.length; i++) {
			for (int j = 0; j < result.length; j++) {
				result[i][j]= neighboursAdjSquare[i][j]*decrementMatrix[i][j];
			}
		}
		
		return result;
	}
	


	@Override
	public CentralityDetectionImproved replicate() {
		// TODO Auto-generated method stub
		return new BetweennessCentralityImproved(this);
	}


}
