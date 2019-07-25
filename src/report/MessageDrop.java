package report;

import java.util.HashMap;

import core.DTNHost;
import core.Message;
import core.MessageListener;

public class MessageDrop extends Report implements MessageListener {
	private HashMap<DTNHost, Integer> drop;
	
	public MessageDrop(){
		init();
	}
	
	@Override
	public void init(){
		super.init();
		this.drop=new HashMap<>();
	}

	@Override
	public void newMessage(Message m) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		if (dropped) {
			if(drop.containsKey(where)){
				drop.put(where, drop.get(where)+1);
			}else{
				drop.put(where, 1);
			}
		}
		else {
			
		}
		
	}

	@Override
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean firstDelivery) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void done() {
		write("Message Drop for scenario " + getScenarioName() + 
				"\nsim_time: " + format(getSimTime())+"\n");
		
		
		String statsText = "" ;
		for(DTNHost key:drop.keySet()){
			statsText=statsText+key.getAddress()+"\t"+drop.get(key)+"\n";
		}
			
		write(statsText);

		super.done();
	}

    @Override
    public void connectionUp(DTNHost thisHost) {
    }

}
