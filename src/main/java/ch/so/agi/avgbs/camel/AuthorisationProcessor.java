package ch.so.agi.avgbs.camel;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.xtf.XtfReader;
import ch.interlis.iox.EndBasketEvent;
import ch.interlis.iox.EndTransferEvent;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.ObjectEvent;
import ch.interlis.iox.StartBasketEvent;
import ch.interlis.iox.StartTransferEvent;

@Component
public class AuthorisationProcessor implements Processor {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	private String serviceUrl = "https://geo.so.ch/api/data/v1/ch.so.agi.av.nachfuehrungsgemeinden.data/";
	
    @Autowired
    private ObjectMapper objectMapper;
	
	@Override
	public void process(Exchange exchange) throws Exception {
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) exchange.getIn().getHeader("CamelAuthentication");
        List<String> groups = (List<String>) authToken.getPrincipal().getAttributes().get("cognito:groups");
                
        File dataFile = exchange.getIn().getBody(File.class);
		
		// FIXME: Im Dataservice steckt der NBIdent nicht drin. Als
		// Näherung verwende ich die BFS-Nummer. Das geht aber nicht
		// immer ganz auf...
        // -> NBIdent in agi_av_gb_admin_einteilungen Modell aufnehmen.
        String nbident = parseTransferFile(dataFile);        
		String bfsnr = nbident.substring(nbident.length() - 4);
		
        URL url = new URL(serviceUrl + "?filter=[[\"bfsnr\",\"=\","+bfsnr+"]]");
        
        log.info(url.toString());
        
        URLConnection request = url.openConnection();
        request.connect();

        JsonNode root = objectMapper.readTree(new InputStreamReader((InputStream) request.getContent()));
        ArrayNode featureArray = (ArrayNode) root.get("features");
        Iterator<JsonNode> it = featureArray.iterator();
        String uid = "";
        while (it.hasNext()) { 
            JsonNode node = it.next();
            uid = node.get("properties").get("uid").textValue();
            break;
        }

        boolean match = false;
        for (String group : groups) {
        	if (group.contains(uid)) {
        		match = true;
        	}        	
        }

        if (!match) {
        	log.error("not allowed to send data");
        	log.error("bfsnr (data): " + bfsnr);
        	log.error("nbident (data): " + nbident);
        	log.error("group (data): " + uid);
        	log.error("groups (user): " + groups.stream().map(g -> g).collect(Collectors.toList()));
	        	
        	throw new AuthorisationProcessorException("not allowed to send data");
        }
	}
	
	/*
	 * 
	 */
    private String parseTransferFile(File xtfFilePath) throws IoxException {
    	String nbident = null;
    	XtfReader ioxReader = null;
    	try {
        	ioxReader = new XtfReader(xtfFilePath);
    		IoxEvent event = ioxReader.read();
    		while (event!=null) {
    			if (event instanceof StartBasketEvent) {
    				StartBasketEvent basket = (StartBasketEvent) event;
    			} else if (event instanceof ObjectEvent) {
    				IomObject iomObj = ((ObjectEvent)event).getIomObject();
    				String tag = iomObj.getobjecttag();    				
    				if (tag.equals("GB2AV.Mutationstabelle.AVMutation")) {
    					nbident = iomObj.getattrobj("MutationsNummer", 0).getattrvalue("NBIdent");
    					break;
    				}	
    			} else if(event instanceof EndBasketEvent) {
    			} else if(event instanceof EndTransferEvent) {
    				ioxReader.close();    				
    				break;
    			}
    			event = ioxReader.read();
    		}
    	} catch (Exception e) {
        	throw new IoxException(e.getMessage());
        } finally {
            if (ioxReader != null) {
                try {
                    ioxReader.close();
                } catch (IoxException e) {
                    throw new IoxException("Error closing IoxReader: " + e.getMessage());
                }
                ioxReader = null;
            }
        }
    	return nbident;
    }
}
