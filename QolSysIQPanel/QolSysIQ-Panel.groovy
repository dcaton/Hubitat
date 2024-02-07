/*
*  QolSys IQ Alarm Panel Driver
*
*  Copyright 2021 Don Caton <dcaton1220@gmail.com>
* 
*  Based on research and code from the following members of
*  the HomeAssistant community (https://community.home-assistant.io/)
*   @mzac
*   @crazeeeyez
*   @Smwoodward
*
*  My apologies if I have inadvertently omitted anyone.
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
*   Change Log:
*   2021-11-24: Initial version
*   2021-12-16: Better handling and retry logic if panel is unreachable
*
*/

import groovy.json.JsonSlurper
import groovy.transform.Field

metadata {
    definition (name: 'QolSys IQ Alarm Panel', namespace: 'dcaton-qolsysiqpanel', author: 'Don Caton', importUrl: 'https://raw.githubusercontent.com/dcaton/Hubitat/main/QolSysIQPanel/QolSysIQ-Panel.groovy') {
        capability 'Initialize'
        capability 'Refresh'
        capability 'Actuator'
        capability 'PresenceSensor'

        command 'armStay', [[name: 'partition_id', type: 'ENUM', description: 'Partition number to arm', constraints: [0, 1, 2, 3] ], [name: 'bypass', type: 'ENUM', description: 'Bypass open or faulted sensors', constraints: ['No', 'Yes']], [name: 'user_code', type: 'NUMBER', description: 'User code (optional if panel is configured to arm without a user code' ]]
        command 'armAway', [[name: 'partition_id', type: 'ENUM', description: 'Partition number to arm', constraints: [0, 1, 2, 3] ], [name: 'bypass', type: 'ENUM', description: 'Bypass open or faulted sensors', constraints: ['No', 'Yes']], [name:'exitDelay*', type: 'NUMBER', description: 'Exit delay in seconds'], [name: 'user_code', type: 'NUMBER', description: 'User code (optional if panel is configured to arm without a user code' ]]
        command 'disarm', [[name: 'partition_id', type: 'ENUM', description: 'Partition number to arm', constraints: [0, 1, 2, 3] ], [name: 'user_code*', type: 'NUMBER', description: 'User code to disarm' ]]
        command 'alarm', [[name: 'partition_id', type: 'ENUM', description: 'Partition number to arm', constraints: [0, 1, 2, 3] ], [name: 'alarmType', type: 'ENUM', description: 'Trigger an alarm condition', constraints: ['POLICE', 'FIRE', 'AUXILIARY'] ]]

        attribute 'Alarm_Mode_Partition_0', 'enum', ['DISARM', 'ENTRY_DELAY', 'EXIT_DELAY', 'ARM_STAY', 'ARM_AWAY', 'ALARM_POLICE', 'ALARM_FIRE', 'ALARM_AUXILIARY']
        attribute 'Alarm_Mode_Partition_1', 'enum', ['DISARM', 'ENTRY_DELAY', 'EXIT_DELAY', 'ARM_STAY', 'ARM_AWAY', 'ALARM_POLICE', 'ALARM_FIRE', 'ALARM_AUXILIARY']
        attribute 'Alarm_Mode_Partition_2', 'enum', ['DISARM', 'ENTRY_DELAY', 'EXIT_DELAY', 'ARM_STAY', 'ARM_AWAY', 'ALARM_POLICE', 'ALARM_FIRE', 'ALARM_AUXILIARY']
        attribute 'Alarm_Mode_Partition_3', 'enum', ['DISARM', 'ENTRY_DELAY', 'EXIT_DELAY', 'ARM_STAY', 'ARM_AWAY', 'ALARM_POLICE', 'ALARM_FIRE', 'ALARM_AUXILIARY']
        
        attribute 'Entry_Delay_Partition_0', 'number'
        attribute 'Entry_Delay_Partition_1', 'number'
        attribute 'Entry_Delay_Partition_2', 'number'
        attribute 'Entry_Delay_Partition_3', 'number'

        attribute 'Exit_Delay_Partition_0', 'number'
        attribute 'Exit_Delay_Partition_1', 'number'
        attribute 'Exit_Delay_Partition_2', 'number'
        attribute 'Exit_Delay_Partition_3', 'number'
        
        attribute 'Error_Partition_0', 'text'
        attribute 'Error_Partition_1', 'text'
        attribute 'Error_Partition_2', 'text'
        attribute 'Error_Partition_3', 'text'
    }
}

preferences {
    input('panelip', 'text', title: 'Alarm Panel IP Address', description: '(IPv4 address in form of 192.168.1.45)', required: true)
    input('accessToken', 'text', title: 'Alarm Panel Access Token', description: '', required: true)
    input( type: 'bool', name: 'AllowArmAndDisarm', title: 'Allow HE to send arming and disarming commands', required: false, defaultValue: false )
    input( type: 'bool', name: 'AllowTriggerAlarm', title: 'Allow HE to trigger an alarm condition', required: false, defaultValue: false )

    input 'logInfo', 'bool', title: 'Show Info Logs?',  required: false, defaultValue: true
    input 'logWarn', 'bool', title: 'Show Warning Logs?', required: false, defaultValue: true
    input 'logDebug', 'bool', title: 'Show Debug Logs?', description: 'Only leave on when required', required: false, defaultValue: true
    input 'logTrace', 'bool', title: 'Show Detailed Logs?', description: 'Only leave on when required', required: false, defaultValue: true
}

@Field static final String drvThis = 'QolSys IQ Alarm Panel'
@Field static final String drvDoorWindow = 'QolSys IQ Door/Window Sensor'
@Field static final String drvSmoke = 'QolSys IQ Smoke Detector'
@Field static final String drvMotion = 'QolSys IQ Motion Sensor'
@Field static final String drvWater = 'QolSys IQ Water Sensor'
@Field static final String drvCO = 'QolSys IQ Carbon Monoxide Detector'
@Field static final String drvGlass = 'QolSys IQ Glass Break Sensor'
@Field static final String drvPendant = 'QolSys IQ Auxiliary Pendant'
@Field static final String drvTakeover = 'QolSys IQ Takeover Module'
@Field static final String drvShock = 'QolSys IQ Takeover Module'

@Field static String partialMessage = ''
@Field static Integer checkInterval = 600

//
// Commands
//

def installed() {
    logTrace('installed()')
    updated()
    runIn(1800, logsOff)
}

def uninstalled() {
    logTrace('uninstalled()')
    unschedule()
    interfaces.rawSocket.close()
}

def updated() {
    logTrace('updated()')
    initialize()
    refresh()
}

def configure() {
    logTrace('configure()')
    unschedule()
}

def initialize() {
    logTrace('initialize()')
    unschedule()
    interfaces.rawSocket.close();
    state.clear()
    partialMessage = '';

    if (!panelip) {
        logError 'IP Address of alarm panel not configured'
        return
    }

    if (!accessToken) {
        logError 'Alarm panel access token not configured.'
        return
    }

    try {
        logTrace("attempting to connect to panel at ${panelip}...");
        interfaces.rawSocket.connect([byteInterface: false, secureSocket: true, ignoreSSLIssues: true, convertReceivedDataToString: true, timeout : (checkInterval * 1000), bufferSize: 10240], panelip, 12345 );
        state.lastMessageReceivedAt = now();
        refresh();
    }
    catch (e) {
        logError( "${panelip} initialize error: ${e.message}" )
        runIn(60, "initialize");
    }
}

def refresh() {
    logTrace('refresh()')
    def msg = '{ "nonce": "", "action": "INFO", "info_type": "SUMMARY", "version": 1, "source": "C4", "token": "' + accessToken + '"}'
    sendCommand(msg)
}

def armStay( String partition_id, String bypass, BigDecimal user_code = 0 ) {
    logTrace( "armStay partition ${partition_id}" )

    if (checkAllowArming() && checkSecureArming(partition_id, user_code) && checkPartition(partition_id)) {
        def msg = _createArmCommand( partition_id, "ARM_STAY", user_code, 0, bypass);
        processEvent( "Error_Partition_${partition_id}", "(No error)" );
        sendCommand(msg);
    }
}

def armAway( String partition_id, String bypass, BigDecimal delay, BigDecimal user_code = 0 ) {
    logTrace( "armAway partition ${partition_id}" )

    if (checkAllowArming() && checkSecureArming(partition_id, user_code) && checkPartition(partition_id)) {
        def msg = _createArmCommand( partition_id, "ARM_AWAY", user_code, delay, bypass);
        processEvent( "Error_Partition_${partition_id}", "(No error)" );
        sendCommand(msg);
    }
}

def disarm( String partition_id, BigDecimal user_code ) {
    logTrace( "disarm partition ${partition_id} usercode ${user_code}" )

    if (!user_code) {
        logError( 'Alarm panel user code not specified.  You cannot disarm the system without a user code.')
        return
    }

    if (checkAllowArming() && checkPartition(partition_id)) {
        def msg = _createArmCommand( partition_id, "DISARM", user_code);
        processEvent( "Error_Partition_${partition_id}", "(No error)" );
        sendCommand(msg);
    }
}

def alarm( String partition_id, String alarm_type ) {
    logTrace( "initiate alarm ${alarm_type}, partition ${partition_id}" )

    if (! (Boolean)settings.AllowTriggerAlarm) {
        logError( "AllowTriggerAlarm setting must be enabled in order to initiate an alarm.")
        return;
    }

    if (checkPartition(partition_id)) {
        def msg = '{ "nonce": "", "action": "ALARM", "alarm_type": "' + alarm_type + '", "version": 1, "source": "C4", "token": "' + accessToken + '", "partition_id":' + partition_id.toString() + ' }'
        logTrace( "initiate alarm: ${msg}" )
        processEvent( "Error_Partition_${partition_id}", "(No error)" );
        sendCommand(msg);
    }
}

private boolean checkAllowArming() {
    def allow = (Boolean)settings.AllowArmAndDisarm;
    if (! allow) {
        logError( "AllowArmAndDisarm setting must be enabled in order to use arm and disarm commands.")
    }
    return allow;
}
        
private boolean checkSecureArming(String partition_id, BigDecimal user_code = 0) {
     if (getDataValue( "Secure_Arm_Partition_${partition_id}" ) && user_code == 0) {
        logError( "Secure arming enabled for partition ${partition_id}.  User code must be specified in order to arm panel.")
        return false;
    }
    
    return true;
}

private boolean checkPartition(String partition_id) {
    if (partition_id.toInteger() >= getDataValue( 'Partitions' ).toInteger()) {
        logError( "Partition ${partition_id} is not enabled in the alarm panel.")
        return false;
    }
    
    return true;
}

private String _createArmCommand( String partition_id, String arming_type, BigDecimal user_code, BigDecimal delay = 0, String bypass = "" ) {
    def command = '{ "version": 1, "source": "C4", "action": "ARMING", "nonce": "", "token": "' + accessToken + '", "partition_id":' + partition_id.toString() + '", "arming_type": "' + arming_type + '", ' ;
    
    if (user_code) {
        command += '"usercode": "' + user_code.toString() + '"';
    }
    
    if (arming_type == "ARM_STAY" || arming_type == "ARM_AWAY" ) {
        command += ', "delay": ' + delay.toString() + ', "bypass": ' + (bypass == "Yes" ? "true" : "false");
    }
    
    command += '}';
    
    return command
}

//
// Socket stuff
//

def socketStatus(String message) {
    if (message == "receive error: String index out of range: -1") {
        // This is some error condition that repeats every 15ms.
        // Probably a bug in the rawsocket code.  Close the connection to prevent
        // the log being flooded with error messages.
        // Note: this may no longer be needed
        interfaces.rawSocket.close();       
        logError( "socketStatus: ${message}");
        logError( "Closing connection to alarm panel" )
        initialize()
    }
    else if (message == "receive error: Read timed out") {
        logWarn("no messages received in ${(now() - state.lastMessageReceivedAt)/60000} minutes, sending INFO command to panel to test connection...");
        refresh();
        runIn(10, "connectionCheck")
    }
    else {
        logError( "socketStatus: ${message}")
        initialize()
    }
}

def parse(message) {
    state.lastMessageReceived = new Date(now()).toString()
    state.lastMessageReceivedAt = now();

    if (message.length() > 0) {

        logDebug("parse() received ${message.length()} bytes : '${message}'")

        if (message.length() >= 3 && message.substring(0,3) == 'ACK') {
            // This is sent by the panel when a command has been received
            partialMessage = '';
            logDebug("'ACK' received");
        }
        else {
            if (partialMessage != null && partialMessage.length() > 0) {
                message = partialMessage + message
            }

            try {
                def payload = new JsonSlurper().parseText(message)
                logDebug("json: ${payload}")

                switch (payload.event) {
                    case 'INFO':
                        switch (payload.info_type) {
                            case 'SUMMARY':
                                logInfo('Summary Info received')
                                processSummary(payload)
                                break
                            default:
                                logError("Unhandled INFO message: ${message}")
                                break
                        }

                        break

                    case 'ZONE_EVENT':
                        switch (payload.zone_event_type) {
                            case 'ZONE_UPDATE':
                                logInfo("Zone update received: ${payload.zone.name}, ${payload.zone.status}")
                                processZoneUpdate(payload.zone)
                                break
                            case 'ZONE_ACTIVE':
                                logInfo("Zone active received: ${payload.zone.zone_id}, ${payload.zone.status}")
                                processZoneActive(payload.zone)
                                break
                            default:
                                logError("Unhandled ZONE_EVENT message: ${message}")
                                break
                        }
                        break

                    case'ARMING':
                        logInfo("Arming event received: ${payload.arming_type}")
                        processEvent( "Alarm_Mode_Partition_${payload.partition_id}", payload.arming_type )
                        switch (payload.arming_type) {
                            case 'EXIT_DELAY':
                                processEvent( "Exit_Delay_Partition_${payload.partition_id}", payload.delay )
                                break;
                            
                            case 'ENTRY_DELAY':
                                processEvent( "Entry_Delay_Partition_${payload.partition_id}", payload.delay )
                                break;
                            
                            case 'DISARM':
                                processEvent( "Entry_Delay_Partition_${payload.partition_id}", 0 )
                                if (payload.partition_id == 0) {
                                    sendEvent( name: 'presence', value: 'present', isStateChanged: true )
                                }
                                break

                            case 'ARM_STAY':
                                break

                            case 'ARM_AWAY':
                                processEvent( "Exit_Delay_Partition_${payload.partition_id}", 0 )
                                if (payload.partition_id == 0) {
                                    sendEvent( name: 'presence', value: 'not present', isStateChanged: true )
                                }
                                break
                            
                            default:
                                logError("Unhandled ARMING message: ${message}")
                                break;
                        }
                        break

                    case'ALARM':
                        logInfo("Alarm event received: ${payload.alarm_type}")

                        // payload.alarm_type can be "POLICE", "FIRE" OR "AUXILIARY"
                        // If it's empty, append "INTRUSION"
                    
                        // Note there is no distinction made between an audible and silent alarm,
                        // or an alarm initiated by a sensor vs an alarm initiated on the keypad

                        processEvent( "Alarm_Mode_Partition_${payload.partition_id}", "ALARM_" + payload.alarm_type )
                        break
                    
                    case 'ERROR':
                        logInfo("Error received: '${payload.error_type}' '${payload.description}'");
                        processEvent( "Error_Partition_${payload.partition_id}", "${payload.error_type}: ${payload.description}" )
                        break;

                    default:
                        logError("Unhandled message: ${message}")
                }

                partialMessage = ''
            }
            catch (ex) {
                // can't parse into json, probably a partial message that fills the socket buffer
                logError(ex.toString())
                logDebug("storing partial message '${message}'")
                partialMessage = message
            }
        }
    }
}

def connectionCheck() {
    def now = now();
    
    if ( now - state.lastMessageReceivedAt > (checkInterval * 1000)) { 
        logError("no messages received in ${(now - state.lastMessageReceivedAt)/60000} minutes, reconnecting...");
        initialize();
    }
    else {
        logDebug("connectionCheck ok");
        runIn(checkInterval, "connectionCheck");
    }
}

//
// Internal stuff
//

private sendCommand(String s) {
    logDebug("sendCommand ${s}")
    interfaces.rawSocket.sendMessage(s)    
}

private processSummary(payload) {
    logTrace 'processSummary'

    updateDataValue( 'Partitions', payload.partition_list.size().toString() )

    def partitions = payload.partition_list.size() > 1

    payload.partition_list.each {
        try {
            processEvent( "Alarm_Mode_Partition_${it.partition_id}", it.status )
            updateDataValue( "Secure_Arm_Partition_${it.partition_id}", it.secure_arm.toString() )
            updateDataValue( "Partition ${it.partition_id} Name", it.name )
            state.unrecognizedDevices = "false";

            def partition = it
            def zoneList = it.zone_list

            zoneList.each {
                switch (it.type) {
                    case [ 'Door_Window', 'Tilt', 'Doorbell' ]:
                        createChildDevice( drvDoorWindow, it, partition, partitions )
                        break
                
                    case [ 'GlassBreak', 'Panel Glass Break' ]:
                        createChildDevice( drvGlass, it, partition, partitions )
                        break
                
                    case [ 'SmokeDetector', 'Smoke_M', 'Heat' ]:
                        createChildDevice( drvSmoke, it, partition, partitions )
                        break
                
                    case 'CODetector':
                        createChildDevice( drvCO, it, partition, partitions )
                        break
                
                    case [ 'Motion', 'Panel Motion' ]:
                        createChildDevice( drvMotion, it, partition, partitions )
                        break
                
                    case 'Water':
                        createChildDevice( drvWater, it, partition, partitions )
                        break
                
                    case 'Auxiliary Pendant':
                        createChildDevice( drvPendant, it, partition, partitions )
                        break
                
                    case 'TakeoverModule':
                        createChildDevice( drvTakeover, it, partition, partitions )
                        break
                        
                    case 'Shock':
                        createChildDevice( drvShock, it, partition, partitions )
                        break

                    case [ 'Siren', 'Keypad', 'Bluetooth' ]:
                        // ignore types that don't make sense as a child device
                        break
                
                    default:
                        logError("Unhandled device type ${it.type}")
                        state.unrecognizedDevices = "true";
                }
            }

            def partitionId = it.partition_id

            logDebug( 'Searching for orphaned devices...' )

            getChildDevices().each {
                def zoneid = it.id

                if (it.partition_id == partitionId && zoneList?.find { it.id == zoneid } == null ) {
                    logInfo( "Deleting orphaned device ${it.deviceNetworkId}" )
                    deleteChildDevice(it.deviceNetworkId)
                }   
            }
        }
        catch (e) {
            logError('Error in processSummary(): ${e.message}')
        }
    }
}

private createChildDevice(deviceName, zone, partition, partitions) {
    def dni = "${device.deviceNetworkId}-z${zone.zone_id}"
    def name = (partitions ? partition.name + ': ' : '') + zone.name
    try {
        def currentchild = getChildDevices()?.find { it.deviceNetworkId == dni };
        if (currentchild == null) {
            log.debug "Creating child device '${deviceName}' dni: '${dni}'"
            currentchild = addChildDevice('dcaton-qolsysiqpanel', deviceName, dni, [name: deviceName /* "${zone.name}" */, label: name, isComponent: true])
        }
        else {
            log.debug "Updating existing child device '${deviceName}' dni: '${dni}'"
        }
        currentchild.label = name
        currentchild.updateDataValue('id', zone.id.toString())
        currentchild.updateDataValue('type', zone.type)
        currentchild.updateDataValue('group', zone.group)
        currentchild.updateDataValue('zone_id', zone.zone_id.toString())
        currentchild.updateDataValue('zone_physical_type', zone.zone_physical_type.toString())
        currentchild.updateDataValue('zone_alarm_type', zone.zone_alarm_type.toString())
        currentchild.updateDataValue('zone_type', zone.zone_type.toString())
        currentchild.updateDataValue('partition_id', zone.partition_id.toString() + " (${partition.name})")
        currentchild.ProcessZoneUpdate(zone)
    }
    catch (e) {
        logError("Error creating or updating child device '${dni}': ${e.message}")
    }
}

private processZoneActive(zone) {
    logTrace 'processZoneActive'
    def dni = "${device.deviceNetworkId}-z${zone.zone_id}"
    try {
        def currentchild = getChildDevices()?.find { it.deviceNetworkId == dni };
        currentchild.ProcessZoneActive(zone)
    }
    catch (e) {
        logError("child device ${dni} not found!  Refreshing device list, ${e.message}")
        refresh()
    }
}

private processZoneUpdate(zone) {
    logTrace 'processZoneUpdate'
    def dni = "${device.deviceNetworkId}-z${zone.zone_id}"
    try {
        def currentchild = getChildDevices()?.find { it.deviceNetworkId == dni };
        currentchild.ProcessZoneUpdate(zone)
    }
    catch (e) {
        logError("child device ${dni} not found!  Refreshing device list, ${e.message}")
        refresh()
    }
}

private processEvent( Variable, Value ) {
    if ( state."${ Variable }" != Value ) {
        state."${ Variable }" = Value
        logDebug( "Event: ${ Variable } = ${ Value }" )
        sendEvent( name: "${ Variable }", value: Value, isStateChanged: true )
    }
}

//
// Logging helpers
//

def logsOff() {
    log.warn 'logging disabled...'
    device.updateSetting('logInfo', [value:'false', type:'bool'])
    device.updateSetting('logWarn', [value:'false', type:'bool'])
    device.updateSetting('logDebug', [value:'false', type:'bool'])
    device.updateSetting('logTrace', [value:'false', type:'bool'])
}

void logDebug(String msg) {
    if ((Boolean)settings.logDebug != false) {
        log.debug "${drvThis}: ${msg}"
    }
}

void logInfo(String msg) {
    if ((Boolean)settings.logInfo != false) {
        log.info "${drvThis}: ${msg}"
    }
}

void logTrace(String msg) {
    if ((Boolean)settings.logTrace != false) {
        log.trace "${drvThis}: ${msg}"
    }
}

void logWarn(String msg, boolean force = false) {
    if (force || (Boolean)settings.logWarn != false) {
        log.warn "${drvThis}: ${msg}"
    }
}

void logError(String msg) {
    log.error "${drvThis}: ${msg}"
}
