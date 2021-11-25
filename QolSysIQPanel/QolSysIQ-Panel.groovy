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
*
*/

import groovy.json.JsonSlurper
import groovy.transform.Field

metadata {
    definition (name: 'QolSys IQ Alarm Panel', namespace: 'dcaton-qolsysiqpanel', author: 'Don Caton', importUrl: 'https://raw.githubusercontent.com/dcaton/Hubitat/main/QolSysIQPanel/QolSysIQ-Panel.groovy') {
        capability 'Initialize'
        capability 'Refresh'
        capability 'Actuator'

        command 'armStay', [[name: 'partition_id', type: 'ENUM', description: 'Partition number to arm', constraints: [0, 1, 2, 3] ], [name: 'bypass', type: 'ENUM', description: 'Bypass open or faulted sensors', constraints: ['No', 'Yes']], [name: 'user_code', type: 'NUMBER', description: 'User code (optional if panel is configured to arm without a user code' ]]
        command 'armAway', [[name: 'partition_id', type: 'ENUM', description: 'Partition number to arm', constraints: [0, 1, 2, 3] ], [name: 'bypass', type: 'ENUM', description: 'Bypass open or faulted sensors', constraints: ['No', 'Yes']], [name:'exitDelay*', type: 'NUMBER', description: 'Exit delay in seconds'], [name: 'user_code', type: 'NUMBER', description: 'User code (optional if panel is configured to arm without a user code' ]]
        command 'disarm', [[name: 'partition_id', type: 'ENUM', description: 'Partition number to arm', constraints: [0, 1, 2, 3] ], [name: 'user_code*', type: 'NUMBER', description: 'User code to disarm' ]]

        attribute 'Alarm_Mode_Partition_0', 'enum', ['DISARM', 'ARM_STAY', 'ARM_AWAY', 'ALARM_INTRUSION', 'ALARM_AUXILIARY', 'ALARM_FIRE', 'ALARM_POLICE']
        attribute 'Alarm_Mode_Partition_1', 'enum', ['DISARM', 'ARM_STAY', 'ARM_AWAY', 'ALARM_INTRUSION', 'ALARM_AUXILIARY', 'ALARM_FIRE', 'ALARM_POLICE']
        attribute 'Alarm_Mode_Partition_2', 'enum', ['DISARM', 'ARM_STAY', 'ARM_AWAY', 'ALARM_INTRUSION', 'ALARM_AUXILIARY', 'ALARM_FIRE', 'ALARM_POLICE']
        attribute 'Alarm_Mode_Partition_3', 'enum', ['DISARM', 'ARM_STAY', 'ARM_AWAY', 'ALARM_INTRUSION', 'ALARM_AUXILIARY', 'ALARM_FIRE', 'ALARM_POLICE']
    }
}

List<String> modeNames = []
location.getModes().each { modeNames.add(it.name) };

preferences {
    input('panelip', 'text', title: 'Alarm Panel IP Address', description: '(IPv4 address in form of 192.168.1.45)', required: true)
    input('accessToken', 'text', title: 'Alarm Panel Access Token', description: 'Only required if you want to arm/disarm the system from HE', required: false)
    input( type: 'enum', name: 'DisarmMode', title: 'Set HE mode when alarm is disarmed', required: false, multiple: false, options: modeNames )
    input( type: 'enum', name: 'ArmStayMode', title: 'Set HE Mode when alarm is armed stay', required: false, multiple: false, options: modeNames )
    input( type: 'enum', name: 'ArmAwayMode', title: 'Set HE mode when alarm is armed away', required: false, multiple: false, options: modeNames )

    input 'logInfo', 'bool', title: 'Show Info Logs?',  required: false, defaultValue: true
    input 'logWarn', 'bool', title: 'Show Warning Logs?', required: false, defaultValue: true
    input 'logDebug', 'bool', title: 'Show Debug Logs?', description: 'Only leave on when required', required: false, defaultValue: true
    input 'logTrace', 'bool', title: 'Show Detailed Logs?', description: 'Only leave on when required', required: false, defaultValue: true
}

@Field static final String drvThis = 'QolSys IQ Alarm Panel'
@Field static final String drvDoorWindow = 'QolSys IQ Door/Window Sensor'
@Field static final String drvSmoke = 'QolSys IQ Smoke/Heat Detector'
@Field static final String drvMotion = 'QolSys IQ Motion Sensor'
@Field static final String drvWater = 'QolSys IQ Water Sensor'
@Field static final String drvCO = 'QolSys IQ Carbon Monoxide Detector'
@Field static final String drvGlass = 'QolSys IQ Glass Break Sensor'
@Field static final String drvPendant = 'QolSys IQ Auxiliary Pendant'

@Field static String partialMessage = ''

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
    state.clear()
    state.version = version()
    state.reconnectDelay = 2

    if (!panelip) {
        logError 'IP Address of alarm panel not configured'
        return
    }

    if (!accessToken) {
        logWarn( 'Alarm panel access token not configured.  You will not be able to arm or disarm the system from Hubitat.', true )
        return
    }

    try {
        if (getChildDevices().size() == 0) {
            refresh()
        }

        interfaces.rawSocket.close();
        partialMessage = '';
        interfaces.rawSocket.connect("${panelip}", 12345, 'byteInterface': false, 'secureSocket': true, 'ignoreSSLIssues': true, 'convertReceivedDataToString': true);
    }
    catch (e) {
        logError( "initialize error: ${e.message}" )
    }
}

def refresh() {
    logTrace('refresh()')
    def msg = '{ "nonce": "", "action": "INFO", "info_type": "SUMMARY", "version": 0, "source": "C4", "token": "' + accessToken + '"}'
    sendCommand(msg)
}

def armStay( String partition_id, String bypass, BigDecimal user_code = 0 ) {
    logTrace( "armStay partition ${partition_id}" )

    if (checkSecureArming(partition_id, user_code) && checkAccessToken() && checkPartition(partition_id)) {
        def msg = _createArmCommand( partition_id, "ARM_STAY", user_code, 0, bypass);
        sendCommand(msg);
    }
}

def armAway( String partition_id, String bypass, BigDecimal delay, BigDecimal user_code = 0 ) {
    logTrace( "armAway partition ${partition_id}" )

    if (checkSecureArming(partition_id, user_code) && checkAccessToken() && checkPartition(partition_id)) {
        def msg = _createArmCommand( partition_id, "ARM_AWAY", user_code, delay, bypass);
        sendCommand(msg);
    }
}

def disarm( String partition_id, BigDecimal user_code ) {
    logTrace( "disarm partition ${partition_id} usercode ${user_code}" )

    if (!user_code) {
        logError( 'Alarm panel user code not specified.  You cannot disarm the system without a user code.', true )
        return
    }

    if (checkAccessToken() && checkPartition(partition_id)) {
        def msg = _createArmCommand( partition_id, "DISARM", user_code);
        sendCommand(msg);
    }
}

private boolean checkAccessToken() {
    if (!accessToken) {
        logError( 'Alarm panel access token not configured.  You can not arm or disarm the system from Hubitat until the access token is configured.', true )
        return false;
    }
    
    return true;
}

private boolean checkSecureArming(String partition_id, BigDecimal user_code = 0) {
     if (getDataValue( "Secure_Arm_Partition_${partition_id}" ) && user_code == 0) {
        logError( "Secure arming enabled for partition ${partition_id}.  User code must be specified in order to arm panel.", true )
        return false;
    }
    
    return true;
}

private boolean checkPartition(String partition_id) {
    if (partition_id.toInteger() >= getDataValue( 'Partitions' ).toInteger()) {
        logError( "Partition ${partition_id} is not enabled in the alarm panel.", true )
        return false;
    }
    
    return true;
}

private String _createArmCommand( String partition_id, String arming_type, BigDecimal user_code, BigDecimal delay = 0, String bypass = "" ) {
    def command = '{ "version": 1, "source": "C4", "action": "ARMING", "nonce": "", "version_key": 1, "token": "' + accessToken + '", "partition_id":' + partition_id.toString() + '", "arming_type": "' + arming_type + '", ' ;
    
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
    if (message != "receive error: Read timed out") {
        // Timeouts don't seem to hurt anything and just clutter up the logs
        // The alarm panel may not send any data for several minutes, especially
        // if nothing is happening (doors opening/closing, etc.)
        logError( "socketStatus: ${message}")
    }
}

def parse(message) {
    if (message.length() > 0) {

        logDebug("parse() received ${message.length()} bytes : '${message}'")

        if (message.length() >= 3 && message.substring(0,3) == 'ACK') {
            // This is sent by the panel when a command has been received
            // There is nothing to do in response to this
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
                            case 'DISARM':
                                setHEMode( payload.arming_type, DisarmMode )
                                break

                            case 'ARM_STAY':
                                setHEMode( payload.arming_type, ArmStayMode )
                                break

                            case 'ARM_AWAY':
                                setHEMode( payload.arming_type, ArmAwayMode )
                                break
                        }
                        break

                    case'ALARM':
                        logInfo("Alarm event received: ${payload.arming_type} ${payload.alarm_type}")

                        def alarmType = 'ALARM_'

                        // payload.alarm_type can be empty (for intrusion alarm), "AUXILIARY", "FIRE", "POLICE", maybe others...
                        // If it's empty, append "INTRUSION"

                        if (payload.alarm_type == '') {
                            alarmType += 'INTRUSION'
                        }
                        else {
                            alarmType += payload.alarm_type
                        }

                        processEvent( "Alarm_Mode_Partition_${payload.partition_id}", alarmType )
                        break

                    default:
                        logError("Unhandled message: ${message}")
                }

                partialMessage = ''
            }
            catch (ex) {
                // can't parse into json, probably a partial message that fills the socket buffer
                logDebug("storing partial message '${message}'")
                partialMessage = message
            }
        }
    }
}

private setHEMode(event, mode) {
    if (mode != null) {
        if (location.getModes().find { e -> e.name == mode } ) {
            location.setMode(mode)
            logInfo( "Set mode to ${mode}")
        }
        else {
            logError( "${event} attempting to set non-existing mode ${mode}; please update configuration for this device");
        }
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

            def partition = it
            def zoneList = it.zone_list

            zoneList.each {
                if ( it.type == 'Door_Window' ) {
                    createChildDevice( drvDoorWindow, it, partition, partitions )
                }
                else if ( it.type == 'GlassBreak' | it.type == 'Panel Glass Break' ) {
                    createChildDevice( drvGlass, it, partition, partitions )
                }
                else if ( it.type == 'SmokeDetector' | it.type == 'Smoke_M' | it.type == 'Heat' ) {
                    createChildDevice( drvSmoke, it, partition, partitions )
                }
                else if ( it.type == 'CODetector' ) {
                    createChildDevice( drvCO, it, partition, partitions )
                }
                else if ( it.type == 'Motion' | it.type == 'Panel Motion' ) {
                    createChildDevice( drvMotion, it, partition, partitions )
                }
                else if ( it.type == 'Water' ) {
                    createChildDevice( drvWater, it, partition, partitions )
                }
                else if ( it.type == 'Auxiliary Pendant' ) {
                    createChildDevice( drvPendant, it, partition, partitions )
                }
                else {
                    logError("Unhandled device type ${it.type}")
                }
            }

            def partitionId = it.partition_id

            logInfo( 'Searching for orphaned devices...' )

            getChildDevices().each {
                def zoneid = it.id

                if (it.partition_id == partitionId && zoneList?.find { it.id = zoneid } == null ) {
                    logInfo( "Deleting orphaned device ${it.deviceNetworkId}" )
                    deleteChildDevice(it.deviceNetworkId)
                }   
            }
        }
        catch (e) {
            logError('Error in processSummary()', e)
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
        currentchild.updateDataValue('id', zone.id.toString())
        currentchild.updateDataValue('type', zone.type)
        currentchild.updateDataValue('name', zone.name)
        currentchild.updateDataValue('group', zone.group)
        currentchild.updateDataValue('zone_id', zone.zone_id.toString())
        currentchild.updateDataValue('zone_physical_type', zone.zone_physical_type.toString())
        currentchild.updateDataValue('zone_alarm_type', zone.zone_alarm_type.toString())
        currentchild.updateDataValue('zone_type', zone.zone_type.toString())
        currentchild.updateDataValue('partition_id', zone.partition_id.toString() + " (${partition.name})")
        currentchild.ProcessZoneUpdate(zone)
    }
    catch (e) {
        logError("Error creating or updating child device '${dni}':", e)
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
        logError("child device ${dni} not found!  Refreshing device list", e)
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
        logError("child device ${dni} not found!  Refreshing device list", e)
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

void logError(String msg, ex = null) {
    log.error "${drvThis}: ${msg}"
    try {
        if (ex) log.error getExceptionMessageWithLine(ex)
    } catch (e) {
    }
}
