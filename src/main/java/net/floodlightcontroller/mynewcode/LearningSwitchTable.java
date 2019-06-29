/**
 *    Copyright 2013, Big Switch Networks, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.floodlightcontroller.mynewcode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.types.MacVlanPair;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.learningswitch.ILearningSwitchService;
import net.floodlightcontroller.staticentry.StaticEntryPusher;
import net.floodlightcontroller.testdemo.copy.HubMaker;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U64;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class LearningSwitchTable extends ServerResource {
    protected static Logger log = LoggerFactory.getLogger(LearningSwitchTable.class);

    protected Map<String, Object> formatTableEntry(MacVlanPair key, OFPort port) {
        Map<String, Object> entry = new HashMap<String, Object>();
        entry.put("mac", key.mac.toString());
        entry.put("vlan", key.vlan.getVlan());
        entry.put("port", port.getPortNumber());
        return entry;
    }

    protected List<Map<String, Object>> getOneSwitchTable(Map<MacVlanPair, OFPort> switchMap) {
        List<Map<String, Object>> switchTable = new ArrayList<Map<String, Object>>();
        for (Entry<MacVlanPair, OFPort> entry : switchMap.entrySet()) {
            switchTable.add(formatTableEntry(entry.getKey(), entry.getValue()));
        }
        return switchTable;
    }

    @Get("json")
    public Map<String, List<Map<String, Object>>> getSwitchTableJson() {
    	System.out.print("test json!");
        ILearningSwitchService lsp =
                (ILearningSwitchService)getContext().getAttributes().
                    get(ILearningSwitchService.class.getCanonicalName());

        Map<IOFSwitch, Map<MacVlanPair, OFPort>> table = lsp.getTable();
        Map<String, List<Map<String, Object>>> allSwitchTableJson = new HashMap<String, List<Map<String, Object>>>();

        String switchId = (String) getRequestAttributes().get("switch");
        System.out.print(switchId);
        if (switchId.toLowerCase().equals("all")) {
            for (IOFSwitch sw : table.keySet()) {
                allSwitchTableJson.put(sw.getId().toString(), getOneSwitchTable(table.get(sw)));
            }
        } else {
            try {
                IOFSwitchService switchService =
                        (IOFSwitchService) getContext().getAttributes().
                            get(IOFSwitchService.class.getCanonicalName());
                IOFSwitch sw = switchService.getSwitch(DatapathId.of(switchId));
                allSwitchTableJson.put(sw.getId().toString(), getOneSwitchTable(table.get(sw)));
            } catch (NumberFormatException e) {
                log.error("Could not decode switch ID = " + switchId);
                setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }

        return allSwitchTableJson;
    }
    
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
		Map<String, Object> data=getjson(json);
		System.out.println(data);
		if(data.get("name")==null)return ;
		for(IOFSwitch sw:LearningSwitch.allswitch.keySet())
		{
			OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
			Match.Builder match=sw.getOFFactory().buildMatch();
			if(data.get("srcMac")!=null)
			{
				match.setExact(MatchField.ETH_SRC, MacAddress.of((String)data.get("srcMac")));
			}
			if(data.get("desMac")!=null)
			{
				match.setExact(MatchField.ETH_DST, MacAddress.of((String)data.get("desMac")));
			}
			
//			match.setMasked(MatchField.IP_PROTO, EthType.of(0x0800));
			if(data.get("IPpro")!=null)
			{
				//match.setExact(MatchField.ETH_TYPE, EthType.of(0x0800));
				match.setExact(MatchField.IP_PROTO, IpProtocol.of((data.get("IPpro").equals("TCP")?(short)0x06:(short)0x11)));//0x06 TCP 0x11 UDP
				
			}
			if(data.get("srcIP")!=null)
			{
				match.setExact(MatchField.ETH_TYPE, EthType.of(0x0800));
				match.setExact(MatchField.IPV4_SRC, IPv4Address.of((String)data.get("srcIP")));
				
//				System.out.println(IPv4Address.of((String)data.get("srcIP")));
			}
			if(data.get("desIP")!=null)
			{
				match.setExact(MatchField.ETH_TYPE, EthType.of(0x0800));
				match.setExact(MatchField.IPV4_DST, IPv4Address.of((String)data.get("desIP")));
			}
			if(data.get("srcPort")!=null)match.setExact(MatchField.TCP_SRC, TransportPort.of((int)data.get("srcPort")));
			if(data.get("desPort")!=null)match.setExact(MatchField.TCP_DST, TransportPort.of((int)data.get("desPort")));
			
			System.out.println("match:"+match.build());
			fmb.setMatch(match.build());
//			fmb.setCookie(LearningSwitch.computeEntryCookie(0, (String)data.get("name")));
			fmb.setCookie(U64.of(HubMaker.LEARNING_SWITCH_COOKIE));
			fmb.setIdleTimeout(0);
			fmb.setHardTimeout(0);
			fmb.setTableId(TableId.of(Integer.parseInt((String)data.get("name"))));
			fmb.setPriority(1000);
			fmb.setBufferId(OFBufferId.NO_BUFFER);
			List<OFAction> actions=new ArrayList<OFAction>();
			fmb.setActions(actions);
			sw.write(fmb.build());
			System.out.println("match:"+fmb.build());
			System.out.println(sw);
		}

		
	}
	@Delete
	public void detelerule(String json)throws IOException//删除一条防火墙规则
	{
		System.out.print(json);
		Map<String, Object> data=getjson(json);
		System.out.println(data);

		for(IOFSwitch sw:LearningSwitch.allswitch.keySet())
		{
			OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowDelete();
			
			
		}
	}
    
}
