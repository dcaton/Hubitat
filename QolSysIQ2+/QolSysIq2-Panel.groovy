/*
*  Unofficial QolSys IQ2+ Alarm Panel Integration for Hubitat
*
*  QolSys IQ2+ Virtual Alarm Panel Driver
*
*  Copyright 2021 Don Caton <dcaton1220@gmail.com>
*
*  This driver enables status reporting of alarm sensors and alarm states
*  from a QolSys IQ2+ panel.  It also allows the alarm to be armed and disarmed.
*
*  Please see the documentation at ... 
*  for prerequisites and installation instructions.
*
*  !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! WARNING !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
*
*  This driver provides the ability to arm and disarm your alarm system.
*  Although you will need a valid user code for the alarm system, there is
*  nothing stopping you from hard-coding user codes into a rule that invokes
*  this driver's arming and disarming commands.
*
*  If you do so, take appropriate measures to secure your Hubutat hub, and any
*  other systems (Node-Red, webcore, etc.) where you create rules that contain
*  any alarm system user codes.  Likewise for any dashboards or other interfaces
*  that can invoke this driver's Disarm command.
*
*  !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
*
*  This driver is based on research and code from the following members
*  of the HomeAssistant community (https://community.home-assistant.io/)
*
*	@mzac
*   @crazeeeyez
*   @Smwoodward
*
*  My apoloigies if I have inadvertently omitted anyone.
*
*
*  MIT License
*  
*  Permission is hereby granted, free of charge, to any person obtaining a copy
*  of this software and associated documentation files (the "Software"), to deal
*  in the Software without restriction, including without limitation the rights
*  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
*  copies of the Software, and to permit persons to whom the Software is
*  furnished to do so, subject to the following conditions:
*  
*  The above copyright notice and this permission notice shall be included in all
*  copies or substantial portions of the Software.
*  
*  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
*  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
*  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
*  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
*  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
*  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
*  SOFTWARE.def version() {"v0.1.0"}
*
*/

def version() {"v0.1.0"}

import hubitat.helper.InterfaceUtils
import groovy.json.JsonSlurper
import groovy.transform.Field
import java.net.*;
import java.io.*;
import javax.net.ssl.*;

metadata {
    definition (name: "QolSys IQ2+ Alarm Panel", namespace: "dcaton-qolsysiq2", author: "Don Caton", importUrl: "") {
        capability "Initialize"
        capability "Refresh"
        capability "Actuator"

        command "armStay", [[name: "partition_id", type: "ENUM", description: "Partition number to arm", constraints: [0,1,2,3] ], [name: "user_code", type: "NUMBER", description: "User code (optional if panel is configured to arm without a user code" ]]
        command "armAway", [[name: "partition_id", type: "ENUM", description: "Partition number to arm", constraints: [0,1,2,3] ], [name: "user_code", type: "NUMBER", description: "User code (optional if panel is configured to arm without a user code" ]]
        command "disarm", [[name: "partition_id", type: "ENUM", description: "Partition number to arm", constraints: [0,1,2,3] ], [name: "user_code*", type: "NUMBER", description: "User code to disarm" ]]
        
        attribute "Alarm_Mode_Partition_0", "enum", ["DISARM", "ARM_STAY", "ARM_AWAY", "ALARM_INTRUSION", "ALARM_AUXILIARY", "ALARM_FIRE", "ALARM_POLICE"]
        attribute "Secure_Arm_Partition_0", "String"
        attribute "Alarm_Mode_Partition_1", "enum", ["DISARM", "ARM_STAY", "ARM_AWAY", "ALARM_INTRUSION", "ALARM_AUXILIARY", "ALARM_FIRE", "ALARM_POLICE"]
        attribute "Secure_Arm_Partition_1", "String"
        attribute "Alarm_Mode_Partition_2", "enum", ["DISARM", "ARM_STAY", "ARM_AWAY", "ALARM_INTRUSION", "ALARM_AUXILIARY", "ALARM_FIRE", "ALARM_POLICE"]
        attribute "Secure_Arm_Partition_2", "String"
        attribute "Alarm_Mode_Partition_3", "enum", ["DISARM", "ARM_STAY", "ARM_AWAY", "ALARM_INTRUSION", "ALARM_AUXILIARY", "ALARM_FIRE", "ALARM_POLICE"]
        attribute "Secure_Arm_Partition_3", "String"
    }
}



List<String> modeNames = [];
location.getModes().each {modeNames.add(it.name)};

preferences {
    input("mqttip", "text", title: "MQTT Broker IP Address", description: "(IPv4 address in form of 192.168.1.45)", required: true)
    input("mqttport", "number", title: "MQTT Broker Port", description: "", required: true, defaultValue: 1883)
    input("panelip", "text", title: "Alarm Panel IP Address", description: "(IPv4 address in form of 192.168.1.45)", required: true)
    input("accessToken", "text", title: "Alarm Panel Access Token", description: "Only required if you want to arm/disarm the system from HE", required: false)
    input( type: "enum", name: "DisarmMode", title: "Set HE mode when alarm is disarmed", required: false, multiple: false, options: modeNames )
    input( type: "enum", name: "ArmStayMode", title: "Set HE Mode when alarm is armed-stay", required: false, multiple: false, options: modeNames )
    input( type: "enum", name: "ArmAwayMode", title: "Set HE mode when alarm is armed away", required: false, multiple: false, options: modeNames )
    
    input "logInfo", "bool", title: "Show Info Logs?",  required: false, defaultValue: true
    input "logWarn", "bool", title: "Show Warning Logs?", required: false, defaultValue: true
    input "logDebug", "bool", title: "Show Debug Logs?", description: "Only leave on when required", required: false, defaultValue: true
    input "logTrace", "bool", title: "Show Detailed Logs?", description: "Only leave on when required", required: false, defaultValue: true
}

@Field static final String drvThis = "QolSys IQ2+ Alarm Panel"
@Field static final String drvDoorWindow = "QolSys IQ2+ Door/Window Sensor"
@Field static final String drvSmoke = "QolSys IQ2+ Smoke Detector"
@Field static final String drvMotion = "QolSys IQ2+ Motion Sensor"
@Field static final String drvWater = "QolSys IQ2+ Water Sensor"
@Field static final String drvCO = "QolSys IQ2+ Carbon Monoxide Detector"
@Field static final String drvGlass = "QolSys IQ2+ Glass Break Sensor"
@Field static final String drvPendant = "QolSys IQ2+ Auxiliary Pendant"

//
// Commands
//

def installed() {
    logTrace("installed()");
    updated();
    runIn(1800,logsOff)
}

def uninstalled() {
    logTrace("uninstalled()");
    unschedule();
    interfaces.mqtt.disconnect();
}

def updated() {
    logTrace("updated()");
    
    state.version = version()
    
    initialize()
    refresh()
}

def configure() {
    logTrace("configure()")
    unschedule();
}

def initialize() {
    logTrace("initialize()");
    unschedule()
    state.clear()
    state.version = version()
    state.reconnectDelay = 2;
    
    if (!ip) {
        logError "IP Address of MQTT broker not configured"
        return
    }
    
    if (!accessToken) {
        logWarn( "Alarm panel access token not configured.  You will not be able to arm or disarm the system from Hubitat.", true );
        return
    }
	
    try {
        
        if (interfaces.mqtt.isConnected()) {
            interfaces.mqtt.disconnect()
        }
        interfaces.mqtt.connect("tcp://${mqttip}:${mqttport}", "qolsysiq2", null, null)
        interfaces.mqtt.subscribe("qolsysiq2-out")
        
        if (getChildDevices().size() == 0) {
            refresh();
        }
    } 
    catch(e) {
        logError( "initialize error: ${e.message}" );
        reconnectMqttClient();
    }
}

def socketStatus(String message) {
    logError( "socketStatus: ${message}");
}

def refresh() {
    logTrace("refresh()");
    def msg = '{ "nonce": "", "action": "INFO", "info_type": "SUMMARY", "version": 0, "source": "C4", "token": "' + accessToken + '"}'
    sendCommand(msg)
}

def armStay( String partition_id, BigDecimal user_code = 0 ) {
    logTrace( "armStay partition ${partition_id} usercode ${user_code}" );

    if (!accessToken) {
        logWarn( "Alarm panel access token not configured.  You can not arm or disarm the system from Hubitat until the access token is configured.", true );
        return
    }
}

def armAway( String partition_id, BigDecimal user_code = 0 ) {
    logTrace( "armAway partition ${partition_id} usercode ${user_code}" );

    if (!accessToken) {
        logWarn( "Alarm panel access token not configured.  You can not arm or disarm the system from Hubitat until the access token is configured.", true );
        return
    }
}

def disarm( String partition_id, BigDecimal user_code ) {
    logTrace( "disarm partition ${partition_id} usercode ${user_code}" );

    if (!accessToken) {
        logWarn( "Alarm panel access token not configured.  You can not arm or disarm the system from Hubitat until the access token is configured.", true );
        return
    }
}

//
// Socket stuff
//

def mqttClientStatus(String status){
    if (status == "Status: Connection succeeded") {
        logInfo( "mqtt client is connected" );
    }
    else { // if(status.startsWith('failure: ') || status.toLowerCase().contains("connection lost")) {
        logError( "mqttClientStatus: '${status}'" );
        reconnectMqttClient()
    }
}

def parse(message) {
    
    // this just prints jibberish, message isn't usable until mqtt.parseMessage is called
    //logDebug("parse() received: '${message} length ${message.length()}'");
    
    if (message.length() > 0) {
    def msg = interfaces.mqtt.parseMessage(message);
    def payload = new JsonSlurper().parseText(msg.payload);
    logDebug("topic: '${msg.topic}' payload: '${payload}'");
    
    switch (payload.event) {
        case 'INFO':
            switch (payload.info_type) {
                case 'SUMMARY':
                    logInfo('Summary Info received');
                    processSummary(payload);
                    break;
                default:
                    logError("Unhandled INFO message: ${message}");
                     break;        
            }
            break;
    
        case 'ZONE_EVENT':
            switch (payload.zone_event_type) {
                case 'ZONE_UPDATE':
                    logInfo("Zone update received: ${payload.zone.name}, ${payload.zone.status}");
                    processZoneUpdate(payload.zone);
                    break;
                case 'ZONE_ACTIVE':
                    logInfo("Zone active received: ${payload.zone.zone_id}, ${payload.zone.status}");
                    processZoneActive(payload.zone);
                    break;
                default:
                    logError("Unhandled ZONE_EVENT message: ${message}");
                    break; 
            }
            break;
        
        case'ARMING':
            logInfo("Arming event received: ${payload.arming_type}");
            processEvent( "Alarm_Mode_Partition_${payload.partition_id}", payload.arming_type )
            switch (payload.arming_type) {
                case 'DISARM':
                    setHEMode( payload.arming_type, DisarmMode );
                    break;
                
                case 'ARM_STAY':
                    setHEMode( payload.arming_type, ArmStayMode );
                    break;
                
                case 'ARM_AWAY':
                    setHEMode( payload.arming_type, ArmAwayMode );
                    break;
            }
            break;
        
        case'ALARM':
            logInfo("Alarm event received: ${payload.arming_type} ${payload.alarm_type}");

            def alarmType = "ALARM_"
        
            // payload.alarm_type can be empty (for intrusion alarm), "AUXILIARY", "FIRE", "POLICE", maybe others...
            // If it's empty, append "INTRUSION"
        
            if (payload.alarm_type == "") {
                alarmType += "INTRUSION";
            }
            else {
                alarmType += payload.alarm_type;
            }
        
            processEvent( "Alarm_Mode_Partition_${payload.partition_id}", alarmType )
            break;
        
        default:
            logError("Unhandled message: ${message}");
    }
    }
}

private setHEMode(event, mode) {
    if (mode != null) { 
        if (location.getModes().find{e -> e.name == mode} ) {
            location.setMode(mode);
            logInfo( "Set mode to ${mode}");
        }
        else {
            logError( "${event} attempting to set non-existing mode ${mode}; please update configuration for this device");
        }
    }
}
        
private def reconnectMqttClient() {
    logTrace( "reconnectMqttClient()" );
    state.reconnectDelay = (state.reconnectDelay ?: 1) * 2
    if(state.reconnectDelay > 60) state.reconnectDelay = 60

    runIn(state.reconnectDelay, initialize)
}

//
// Internal stuff
//

private def sendCommand(String s) {
    logDebug("sendCommand ${s}");
    interfaces.mqtt.publish("qolsysiq2-in", s)
}

private def processSummary(payload) {
    logTrace "processSummary";
    
    updateDataValue( "Partitions", payload.partition_list.size().toString() );
    
    def partitions = payload.partition_list.size() > 1;

    payload.partition_list.each {
        
        if (true) {
           try {
               processEvent( "Alarm_Mode_Partition_${it.partition_id}", it.status );
               processEvent( "Secure_Arm_Partition_${it.partition_id}", it.secure_arm );
               updateDataValue( "Partition ${it.partition_id} Name", it.name );
               
               def partition = it;
               def zoneList = it.zone_list;
               
               zoneList.each {
                   if ( it.type == 'Door_Window' ) {
                       createChildDevice( drvDoorWindow, it, partition, partitions );
                    }
                    else if ( it.type == 'GlassBreak' | it.type == 'Panel Glass Break' ) {
                       createChildDevice( drvGlass, it, partition, partitions );
                    }
                    else if ( it.type == 'SmokeDetector' | it.type == 'Smoke_M' | it.type == 'Heat' ) {
                       createChildDevice( drvSmoke, it, partition, partitions );
                    }
                    else if ( it.type == 'CODetector' ) {
                       createChildDevice( drvCO, it, partition, partitions );
                    }
                    else if ( it.type == 'Motion' | it.type == 'Panel Motion' ) {
                       createChildDevice( drvMotion, it, partition, partitions );
                    }
                    else if ( it.type == 'Water' ) {
                        createChildDevice( drvWater, it, partition, partitions );
                    }
                    else if ( it.type == 'Auxiliary Pendant' ) {
                        createChildDevice( drvPendant, it, partition, partitions );
                    }
                    else {
                        logError("Unhandled device type ${it.type}");
                    }
               }
               
               def partitionId = it.partition_id;
               
               logInfo( "Searching for orphaned devices..." );
               
               getChildDevices().each {
                   def zoneid = it.id;
                   
                   if (it.partition_id == partitionId && zoneList?.find() { it.id = zoneid } == null ) {
                       logInfo( "Deleting orphaned device ${it.deviceNetworkId}" );
                       deleteChildDevice(it.deviceNetworkId);    
                   }
               };
               
           }
           catch (e) {
               logError("Error in processSummary()", e);
           }
        }
        else logTrace "false"
    }
}

private def createChildDevice(deviceName, zone, partition, partitions){
	log.debug "Adding Child Alarm Device"
    def dni = "${device.deviceNetworkId}-z${zone.zone_id}"; 
    def name = (partitions ? partition.name + ": " : "") + zone.name;
    try {
        log.debug "Trying to create child device ${deviceName} dni: ${dni} if it doesn't already exist";
        def currentchild = getChildDevices()?.find { it.deviceNetworkId == dni };
        if (currentchild == null) {
            log.debug "Creating ${deviceName} child for ${dni}"
            currentchild = addChildDevice("dcaton-qolsysiq2", deviceName, dni, [name: deviceName /* "${zone.name}" */, label: name, isComponent: true])
        }
        currentchild.updateDataValue("id", zone.id.toString());
        currentchild.updateDataValue("type", zone.type);
        currentchild.updateDataValue("name", zone.name);
        currentchild.updateDataValue("group", zone.group);
        currentchild.updateDataValue("zone_id", zone.zone_id.toString());
        currentchild.updateDataValue("zone_physical_type", zone.zone_physical_type.toString());
        currentchild.updateDataValue("zone_alarm_type", zone.zone_alarm_type.toString());
        currentchild.updateDataValue("zone_type", zone.zone_type.toString());
        currentchild.updateDataValue("partition_id", zone.partition_id.toString() + " (${partition.name})");
        currentchild.ProcessZoneUpdate(zone);
    }
    catch (e) {
        logError("Error creating child device ${dni}", e);
    }
}

private def processZoneActive(zone) {
	logTrace "processZoneActive"
    def dni = "${device.deviceNetworkId}-z${zone.zone_id}"; 
    try {
        def currentchild = getChildDevices()?.find { it.deviceNetworkId == dni };
        currentchild.ProcessZoneActive(zone);
    }
    catch (e) {
        logError("child device ${dni} not found!  Refreshing device list", e);
        refresh();
    }
}

private def processZoneUpdate(zone) {
	logTrace "processZoneUpdate"
    def dni = "${device.deviceNetworkId}-z${zone.zone_id}"; 
    try {
        def currentchild = getChildDevices()?.find { it.deviceNetworkId == dni };
        currentchild.ProcessZoneUpdate(zone);
    }
    catch (e) {
        logError("child device ${dni} not found!  Refreshing device list", e);
        refresh();
    }
}
                                                                                   
private def processEvent( Variable, Value, Unit = null ){
    if( state."${ Variable }" != Value ){
        state."${ Variable }" = Value
        if( Unit != null ){
            logDebug( "Event: ${ Variable } = ${ Value }${ Unit }" );
            sendEvent( name: "${ Variable }", value: Value, unit: Unit, isStateChanged: true )
        } else {
            logDebug( "Event: ${ Variable } = ${ Value }" );
            sendEvent( name: "${ Variable }", value: Value, isStateChanged: true )
        }
    }
    else {
        //sendEvent( name: "${ Variable }", value: Value, isStateChanged: false )
    }
}

//
// Logging helpers
//

def logsOff(){
    log.warn "logging disabled..."
    device.updateSetting("logInfo",[value:"false",type:"bool"])
    device.updateSetting("logWarn",[value:"false",type:"bool"])
    device.updateSetting("logDebug",[value:"false",type:"bool"])
    device.updateSetting("logTrace",[value:"false",type:"bool"])
}

void logDebug(String msg) {
    if((Boolean)settings.logDebug != false) {
        log.debug "${drvThis}: ${msg}"
    }
}

void logInfo(String msg) {
    if((Boolean)settings.logInfo != false) {
        log.info "${drvThis}: ${msg}"
    }
}

void logTrace(String msg) {
    if((Boolean)settings.logTrace != false) {
        log.trace "${drvThis}: ${msg}"
    }
}

void logWarn(String msg, boolean force = false) {
    if(force ||(Boolean)settings.logWarn != false) {
        log.warn "${drvThis}: ${msg}"
    }
}

void logError(String msg, ex = null) {
    log.error "${drvThis}: ${msg}"
    try {
        if (ex) log.error getExceptionMessageWithLine(ex)
    } catch (e) {
    }
}
