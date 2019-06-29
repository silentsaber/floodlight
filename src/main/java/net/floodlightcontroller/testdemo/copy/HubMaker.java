package net.floodlightcontroller.testdemo.copy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPAddress;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;

import com.fasterxml.jackson.databind.deser.DataFormatReaders.Match;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.restserver.IRestApiService;

public class HubMaker implements IFloodlightModule, IOFMessageListener {

	private enum HubType {USE_PACKET_OUT, USE_FLOW_MOD};
    private IFloodlightProviderService floodlightProvider;
    private IRestApiService restApiService;
    public static Map<IOFSwitch,Boolean>allswitch;//���еĽ�����  Ҫ�����н������·�����
    public static final int LEARNING_SWITCH_APP_ID = 1;
	// LOOK! This should probably go in some class that encapsulates
	// the app cookie management
	public static final int APP_ID_BITS = 12;
	public static final int APP_ID_SHIFT = (64 - APP_ID_BITS);
	public static final long LEARNING_SWITCH_COOKIE = (long) (LEARNING_SWITCH_APP_ID & ((1 << APP_ID_BITS) - 1)) << APP_ID_SHIFT;
    class pro//����
    {
    	String name;//��������
    	int priority;//���ȼ�
//    	OFAction action;//��Ϊ drop
    	short ideal_timeout;
    	short hard_timeout;
    	MacAddress srcMac;//ԴMac
    	MacAddress desMac;//Ŀ��Mac
    	IPAddress srcIP;//ԴIP
    	IPAddress desIP;//Ŀ��IP
    	IpProtocol IPpro;//IPЭ��
    	TransportPort srcPort;//Դ�˿�
    	TransportPort desPort;//Ŀ�Ķ˿�
    	pro()//��ʼ��
    	{
    		name=null;
    		priority=100;
//    		action="drop";
    		ideal_timeout=0;
    		hard_timeout=0;
    		srcMac=null;
    		desMac=null;
    		srcIP=null;
    		desIP=null;
    		IPpro=null;
    		srcPort=null;
    		desPort=null;
    	}
    	public void set()
    	{
    		MacAddress srcMac;//ԴMac
        	MacAddress desMac;//Ŀ��Mac
        	IPAddress srcIP;//ԴIP
        	IPAddress desIP;//Ŀ��IP
        	IpProtocol IPpro;//IPЭ��
        	TransportPort srcPort;//Դ�˿�
        	TransportPort desPort;//Ŀ�Ķ˿�
    	}
    }
    public static List<Map<String,Object>>Rules;//����
	public void setFloodlightProvider(IFloodlightProviderService floodlightProvider) {
	 this.floodlightProvider = floodlightProvider;
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return HubMaker.class.getPackage().getName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		
		HubMaker.allswitch.put(sw,true);//�¼���switch
//		System.out.println(msg);
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		System.out.println(eth.getSourceMACAddress());
		
		OFMessage outMessage;
		HubType ht = HubType.USE_FLOW_MOD;	
		outMessage = createHubFlowMod(sw, msg);
		sw.write(outMessage);
		return Command.CONTINUE;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends
				IFloodlightService>>();
				l.add(IFloodlightProviderService.class);
				l.add(IRestApiService.class);
				return l;
//		return null;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		restApiService = context.getServiceImpl(IRestApiService.class);
		HubMaker.allswitch=new HashMap<IOFSwitch,Boolean>();
		HubMaker.Rules=new ArrayList<>();
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
//		floodlightProvider.addOFMessageListener(OFType.FLOW_REMOVED, this);
//		floodlightProvider.addOFMessageListener(OFType.ERROR, this);
		restApiService.addRestletRoutable(new TestDemoWebRoutable());
		
	}

	
	//add code
	private OFMessage createHubFlowMod(IOFSwitch sw, OFMessage msg) {
		OFPacketIn pi = (OFPacketIn) msg;
		OFFlowAdd.Builder fmb = sw.getOFFactory().buildFlowAdd();
		fmb.setBufferId(pi.getBufferId()).setXid(pi.getXid());
		// set actions
		OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
		actionBuilder.setPort(OFPort.FLOOD);
		// import java.util.Collections
		fmb.setActions(Collections.singletonList((OFAction) actionBuilder.build()));
		return fmb.build();
	}
	
//		private OFMessage createHubPacketOut(IOFSwitch sw, OFMessage msg) {
//		OFPacketIn pi = (OFPacketIn) msg;
//		OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
//		
//		pob.setBufferId(pi.getBufferId()).setXid(pi.getXid()).setInPort((pi.getVersion().compareTo
//		(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT)));
//	
//		// set actions
//		if(sw.getId().getLong()==2)// match switch 2
//		{
//			//port 1:h3     port 2:h4
////			System.out.print(pi.getMatch().get(MatchField.IN_PORT));
////			System.out.print("\n");
////			if(pi.getInPort().getPortNumber()==2)
//			if(pi.getMatch().get(MatchField.IN_PORT).getPortNumber()==2)return null;
//		}
//		OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
//		actionBuilder.setPort(OFPort.FLOOD);
//		pob.setActions(Collections.singletonList((OFAction) actionBuilder.build()));
//		
//	// set data if it is included in the packetin
//		if (pi.getBufferId() == OFBufferId.NO_BUFFER) {
//		byte[] packetData = pi.getData();
//		pob.setData(packetData);
//		}
//		return pob.build(); 
//	}
		
		
		
		
}
