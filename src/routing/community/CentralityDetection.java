package routing.community;

/*Kelas ini dibuat oleh: Elisabeth Kusuma
 * untuk diimplementasikan dalam kelas Betweenness Centrality
 * Universitas Sanata Dharma, Yogyakarta
 * :p */

import core.DTNHost;

import java.util.ArrayList;
import java.util.Map;

public interface CentralityDetection {
	
	public double getCentrality(double[][] matrixEgoNetwork);
	public CentralityDetection replicate();

}
