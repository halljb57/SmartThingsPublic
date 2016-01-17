/**
 *  ESP8266_Device_Handler
 *
 *  Copyright 2016 Joseph Hall
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
import groovy.json.JsonSlurper
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
 
 preferences 
{
        input("ip", "string", title:"IP Address", description: "192.168.1.150", defaultValue: "192.168.11.168", required: true, displayDuringSetup: true)
        input("port", "string", title:"Port", description: "80", defaultValue: "80", required: true, displayDuringSetup: true)
        //input("username", "string", title:"Username", description: "pi", defaultValue: "webiopi" , required: true, displayDuringSetup: true)
        //input("password", "password", title:"Password", description: "raspberry", defaultValue: "raspberry" , required: true, displayDuringSetup: true)
        
}
 
metadata 
{
    definition (name: "ESP8266_Device_Handler", namespace: "halljb57", author: "Joseph Hall", description: "Control a web app on the esp8266") 
    {
        capability "Polling"
        capability "Refresh"
        capability "Switch"
        capability "Sensor"
        capability "Actuator"
        capability "Contact Sensor"
        
        command "restart"
        command "pushButtonOn"
        command "pushButtonOff"
    }

    simulator 
    {}

    tiles {
        standardTile("button", "device.switch", width: 1, height: 1, canChangeIcon: true) 
        {
            state "on", label: '${name}', icon: "st.Electronics.electronics18", backgroundColor: "#fffccc", nextState: "off", action: "pushButtonOn"
            state "off", label: '${name}', icon: "st.Electronics.electronics18", backgroundColor: "#79b821", nextState: "on", action: "pushButtonOff"
        }
        standardTile("contact", "device.contact", width: 1, height: 1, canChangeIcon: true) 
        {
            state "closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#79b821", action: "pushButton"
            state "open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#ffa81e", action: "pushButton"
        }
        standardTile("restart", "device.restart", inactiveLabel: false, decoration: "flat") {
        
            state "default", action:"restart", label: "Restart", displayName: "Restart"
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") 
        {
            state "default", action:"refresh.refresh", icon: "st.secondary.refresh"
        }
        
        main "button"
        details(["button", "contact", "restart", "refresh"])
    }
}

// ------------------------------------------------------------------

// parse events into attributes
def parse(String description) {
    log.debug "Parsing Description: '${description}'"
	def msg = parseLanMessage(description)
    
	log.debug "data ${msg.data}"
	log.debug "body ${msg.body}"
	log.debug "headers ${msg.headers}"
    
    if(msg.headers.toString().contains("get /socket1on"))
    {
    	log.debug "Parse returned Button Set One - button ON..."
        sendEvent(name: "switch", value: "off")
    }
    if(msg.headers.toString().contains("get /socket1off"))
    {
    	log.debug "Parse returned Button Set One - button OFF..."
        sendEvent(name: "switch", value: "on")
    } 
    if(msg.headers.toString().contains("get /socket2on"))
    {
    	log.debug "Parse returned Button Set Two - button ON..."
        sendEvent(name: "switch", value: "off")
    }
    if(msg.headers.toString().contains("get /socket2off"))
    {
    	log.debug "Parse returned Button Set Two - button OFF..."
        sendEvent(name: "switch", value: "on")
    } 
}

// ------------------------------------------------------------------
// handle commands
// ------------------------------------------------------------------
def poll() {
    log.debug "Executing 'poll'"
    sendEvent(name: "switch", value: "off")
    getRPiData()
}

def refresh() {
    sendEvent(name: "switch", value: "on")
    log.debug "Executing 'refresh'"
    getRPiData()
}

def restart(){
    log.debug "Restart was pressed"
    sendEvent(name: "switch", value: "off")
    def uri = "/api_command/reboot"
    postAction(uri)
}

// Get CPU percentage reading
private getRPiData() {
    // def uri = "/api_command/smartthings"
    def uri = "/socket1Off"
    postAction(uri)
}

// ------------------------------------------------------------------

private postAction(uri){
  setDeviceNetworkId(ip,port)  
  
  def userpass = encodeCredentials(username, password)
  //log.debug("userpass: " + userpass) 
  
  // Set header info...
  def headers = getHeader(userpass)
  log.debug("headers: " + headers) 
  
  def hubAction = new physicalgraph.device.HubAction(
    method: "POST",
    path: uri,
    headers: headers
  )//,delayAction(1000), refresh()]
  //log.debug("Executing hubAction on " + getHostAddress())
  log.debug hubAction
  hubAction    
}

// ------------------------------------------------------------------
// Helper methods
// ------------------------------------------------------------------

def parseDescriptionAsMap(description) {
    description.split(",").inject([:]) { map, param ->
        def nameAndValue = param.split(":")
        map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
    }
}

def toAscii(s){
        StringBuilder sb = new StringBuilder();
        String ascString = null;
        long asciiInt;
                for (int i = 0; i < s.length(); i++){
                    sb.append((int)s.charAt(i));
                    sb.append("|");
                    char c = s.charAt(i);
                }
                ascString = sb.toString();
                asciiInt = Long.parseLong(ascString);
                return asciiInt;
    }

private encodeCredentials(username, password){
    //log.debug "Encoding credentials"
    def userpassascii = "${username}:${password}"
    def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    //log.debug "ASCII credentials are ${userpassascii}"
    //log.debug "Credentials are ${userpass}"
    return userpass
}

// Get host inf and add it to the header.....
private getHeader(userpass){
    //log.debug "Getting headers"
    def headers = [:]
    headers.put("HOST", getHostAddress())
    //headers.put("Authorization", userpass)
    //log.debug "Headers are ${headers}"
    return headers
}

private delayAction(long time) {
    new physicalgraph.device.HubAction("delay $time")
}

private setDeviceNetworkId(ip,port){
    //def iphex = convertIPtoHex(ip)
    //def porthex = convertPortToHex(port)
    //device.deviceNetworkId = "$iphex:$porthex"
    //log.debug "Device Network Id set to ${iphex}:${porthex}"
    device.deviceNetworkId = "18FE34F47686"
    log.debug "Device Network Id set to ${device.deviceNetworkId}"
    
}

private getHostAddress() {
    return "${ip}:${port}"
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}

// ------------------------------------------------------------------
// Button controls
// ------------------------------------------------------------------
def pushButtonOn() 
{
	log.debug "On Button pressed"
    def uri = "/socket1On"
    postAction(uri)
}

def pushButtonOff() 
{
    log.debug "Off Button pressed"
    def uri = "/socket1Off"
    postAction(uri)
}