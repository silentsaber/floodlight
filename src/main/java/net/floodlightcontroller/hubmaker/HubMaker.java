package net.floodlightcontroller.hubmaker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
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
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

public class HubMaker implements IFloodlightModule, IOFMessageListener {

	private enum HubType {USE_PACKET_OUT, USE_FLOW_MOD};
    private IFloodlightProviderService floodlightProvider;
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
		OFMessage outMessage;
//		HubType ht = HubType.USE_PACKET_OUT;
		HubType ht = HubType.USE_FLOW_MOD;
		switch (ht) {
		case USE_FLOW_MOD:
			outMessage = createHubFlowMod(sw, msg);
			break;
		default:
		case USE_PACKET_OUT:
		outMessage = createHubPacketOut(sw, msg);
		break;
		}
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
			
				return l;
//		return null;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
//		floodlightProvider.addOFMessageListener(OFType.FLOW_REMOVED, this);
//		floodlightProvider.addOFMessageListener(OFType.ERROR, this);
		
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
	
		private OFMessage createHubPacketOut(IOFSwitch sw, OFMessage msg) {
		OFPacketIn pi = (OFPacketIn) msg;
		OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
		
		pob.setBufferId(pi.getBufferId()).setXid(pi.getXid()).setInPort((pi.getVersion().compareTo
		(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT)));
	
		// set actions
		if(sw.getId().getLong()==2)// match switch 2
		{
			//port 1:h3     port 2:h4
//			System.out.print(pi.getMatch().get(MatchField.IN_PORT));
//			System.out.print("\n");
//			if(pi.getInPort().getPortNumber()==2)
			if(pi.getMatch().get(MatchField.IN_PORT).getPortNumber()==2)return null;
		}
		OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
		actionBuilder.setPort(OFPort.FLOOD);
		pob.setActions(Collections.singletonList((OFAction) actionBuilder.build()));
		
	// set data if it is included in the packetin
		if (pi.getBufferId() == OFBufferId.NO_BUFFER) {
		byte[] packetData = pi.getData();
		pob.setData(packetData);
		}
		return pob.build(); 
	}
		
		
		
		
}
