package net.floodlightcontroller.testdemo.copy;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.match.MatchFields;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPAddress;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U64;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.OFSwitch;
import net.floodlightcontroller.learningswitch.ILearningSwitchService;
import net.floodlightcontroller.mynewcode.LearningSwitch;

//import net.floodlightcontroller.staticentry.StaticEntryPusher;

public class testdemorule extends ServerResource{
	public  Map<String, Object> getjson(String json) throws IOException {
		Map<String, Object> entry = new HashMap<String, Object>();
		MappingJsonFactory f = new MappingJsonFactory();
		JsonParser jp;
		try {
			jp = f.createParser(json);
		} catch (JsonParseException e) {
			throw new IOException(e);
		}

		jp.nextToken();
		if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
			throw new IOException("Expected START_OBJECT");
		}

		while (jp.nextToken() != JsonToken.END_OBJECT) {
			if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
				throw new IOException("Expected FIELD_NAME");
			}

			String n = jp.getCurrentName().trim();
			jp.nextToken();
			entry.put(n, jp.getText()); /* All others are 'key':'value' pairs */
			
		}
		return entry;
	}
	@Get("json")
	public List<Map<String,Object>> allrule()//获取所有防火墙规则
	{
		return HubMaker.Rules;
	}
	@Post
	public void addrule(String json)throws IOException//增加一条防火墙规则
	{
		System.out.println(json);
		
		/*
		 * 定义规则:源MAC   目的MAC  源IP  目的IP  源端口  目的端口   协议:TCP/UDP
		 * 定义规则名称: name
		 * 定义规则优先级: priority
		 * 默认行为: drop
		 * 默认时长: infinite
		*/
		Map<String, Object> data=getjson(json);//新加入的一条规则
//		System.out.println(data.get("active"));
//		System.out.println(data.get("x"));
	//	HubMaker.Rules.add(data);
		System.out.println(data);
		System.out.println(data.get("name"));
//		if(data.get("name")==null)return ;
		HubMaker.Rules.add(data);
		for(IOFSwitch sw:HubMaker.allswitch.keySet())
		{
			OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
			Match.Builder match=sw.getOFFactory().buildMatch();
			if(data.get("srcMac")!=null)
			{
				match.setExact(MatchField.ETH_SRC, MacAddress.of((String)data.get("srcMac")));
				
			}
			if(data.get("desMac")!=null)match.setExact(MatchField.ETH_DST, MacAddress.of((String)data.get("desMac")));
			if(data.get("IPpro")!=null)
			{
				match.setExact(MatchField.ETH_TYPE, EthType.of(0x0800));
				match.setExact(MatchField.IP_PROTO, IpProtocol.of((data.get("IPpro").equals("TCP"))?(short)0x06:(short)0x11));//0x06 TCP 0x11 UDP
				if(data.get("srcPort")!=null)
				{
					if(data.get("IPpro").equals("TCP"))match.setExact(MatchField.TCP_SRC, TransportPort.of(Integer.parseInt((String)data.get("srcPort"))));
					else if(data.get("IPpro").equals("UDP"))match.setExact(MatchField.UDP_SRC, TransportPort.of(Integer.parseInt((String)data.get("srcPort"))));
				}
				if(data.get("desPort")!=null)
				{
					if(data.get("IPpro").equals("TCP"))match.setExact(MatchField.TCP_DST, TransportPort.of(Integer.parseInt((String)data.get("desPort"))));
					else if(data.get("IPpro").equals("UDP"))match.setExact(MatchField.UDP_DST, TransportPort.of(Integer.parseInt((String)data.get("desPort"))));
				}
			}
			if(data.get("srcIP")!=null)
			{
				match.setExact(MatchField.ETH_TYPE, EthType.of(0x0800));
//				match.setExact(MatchField.IP_PROTO, IpProtocol.of((short)0x06));
//				match.setExact(MatchField.ETH_TYPE, EthType.of((String)data.get("srcIP")));
				match.setExact(MatchField.IPV4_SRC, IPv4Address.of((String)data.get("srcIP")));
				
			}
			if(data.get("desIP")!=null)
			{
				match.setExact(MatchField.ETH_TYPE, EthType.of(0x0800));
//				match.setExact(MatchField.IP_PROTO, IpProtocol.of((short)0x06));
				match.setExact(MatchField.IPV4_DST, IPv4Address.of((String)data.get("desIP")));
			}
			
	
			fmb.setMatch(match.build());
			fmb.setCookie((U64.of(HubMaker.LEARNING_SWITCH_COOKIE)));
			fmb.setIdleTimeout(0);
//			fmb.setTableId(TableId.of((int)data.get("name")));
//			fmb.setTableId(TableId.of(Integer.parseInt((String)data.get("name"))));
			fmb.setHardTimeout(0);
			fmb.setPriority(100);
			fmb.setBufferId(OFBufferId.NO_BUFFER);
			sw.write(fmb.build());			
			System.out.println("match:"+fmb.build());
			System.out.println(sw);
	
		}
//		System.out.println(HubMaker.allswitch);
		
		
	}
	@Delete
	public void detelerule(String json)throws IOException//删除一条防火墙规则
	{
		System.out.print(json);
		Map<String, Object> data=getjson(json);
		System.out.println(data);
//		HubMaker.Rules.add(data);
		HubMaker.Rules.remove(data);
		for(IOFSwitch sw:HubMaker.allswitch.keySet())
		{
			OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowDelete();
			Match.Builder match=sw.getOFFactory().buildMatch();
			if(data.get("srcMac")!=null)
			{
				match.setExact(MatchField.ETH_SRC, MacAddress.of((String)data.get("srcMac")));
				
			}
			if(data.get("desMac")!=null)match.setExact(MatchField.ETH_DST, MacAddress.of((String)data.get("desMac")));
			if(data.get("IPpro")!=null)
			{
				match.setExact(MatchField.ETH_TYPE, EthType.of(0x0800));
				match.setExact(MatchField.IP_PROTO, IpProtocol.of((data.get("IPpro").equals("TCP"))?(short)0x06:(short)0x11));//0x06 TCP 0x11 UDP
				if(data.get("srcPort")!=null)
				{
					if(data.get("IPpro").equals("TCP"))match.setExact(MatchField.TCP_SRC, TransportPort.of(Integer.parseInt((String)data.get("srcPort"))));
					else if(data.get("IPpro").equals("UDP"))match.setExact(MatchField.UDP_SRC, TransportPort.of(Integer.parseInt((String)data.get("srcPort"))));
				}
				if(data.get("desPort")!=null)
				{
					if(data.get("IPpro").equals("TCP"))match.setExact(MatchField.TCP_DST, TransportPort.of(Integer.parseInt((String)data.get("desPort"))));
					else if(data.get("IPpro").equals("UDP"))match.setExact(MatchField.UDP_DST, TransportPort.of(Integer.parseInt((String)data.get("desPort"))));
				}
			}
			if(data.get("srcIP")!=null)
			{
				match.setExact(MatchField.ETH_TYPE, EthType.of(0x0800));
//				match.setExact(MatchField.IP_PROTO, IpProtocol.of((short)0x06));
//				match.setExact(MatchField.ETH_TYPE, EthType.of((String)data.get("srcIP")));
				match.setExact(MatchField.IPV4_SRC, IPv4Address.of((String)data.get("srcIP")));
				
			}
			if(data.get("desIP")!=null)
			{
				match.setExact(MatchField.ETH_TYPE, EthType.of(0x0800));
//				match.setExact(MatchField.IP_PROTO, IpProtocol.of((short)0x06));
				match.setExact(MatchField.IPV4_DST, IPv4Address.of((String)data.get("desIP")));
			}
			fmb.setMatch(match.build());
			fmb.setCookie((U64.of(HubMaker.LEARNING_SWITCH_COOKIE)));
			fmb.setIdleTimeout(0);
			fmb.setHardTimeout(0);
			fmb.setPriority(100);
			fmb.setBufferId(OFBufferId.NO_BUFFER);
			fmb.setOutPort(OFPort.ANY);
			sw.write(fmb.build());			
			System.out.println("match:"+fmb.build());
			System.out.println(sw);
		}
	}
}
