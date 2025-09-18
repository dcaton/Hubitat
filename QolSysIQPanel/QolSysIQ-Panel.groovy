/* groovylint-disable BuilderMethodWithSideEffects, DuplicateListLiteral, DuplicateMapLiteral, DuplicateNumberLiteral, DuplicateStringLiteral, FactoryMethodName, ImplicitClosureParameter, MethodCount, MethodParameterTypeRequired, MethodSize, NestedBlockDepth, PublicMethodsBeforeNonPublicMethods, UnnecessaryGetter */
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
import com.hubitat.app.ChildDeviceWrapper

/* groovylint-disable-next-line CompileStatic */
metadata {
    definition(name: 'QolSys IQ Alarm Panel', namespace: 'dcaton-qolsysiqpanel', author: 'Don Caton', importUrl: 'https://raw.githubusercontent.com/dcaton/Hubitat/main/QolSysIQPanel/QolSysIQ-Panel.groovy') {
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

        attribute 'connected', 'enum', ['connected', 'not connected']
        attribute 'healthStatus', 'enum', ['offline', 'online']
    }
}

preferences {
    input(type: 'string', name: 'panelip', title: 'Alarm Panel IP Address', description: '(IPv4 address in form of 192.168.1.45)', required: true)
    input(type: 'string', name: 'accessToken', title: 'Alarm Panel Access Token', description: 'Token obtained from alarm panel', required: true)
    input(type: 'bool', name: 'AllowArmAndDisarm', title: 'Allow HE to send arming and disarming commands', required: false, defaultValue: false)
    input(type: 'bool', name: 'AllowTriggerAlarm', title: 'Allow HE to trigger an alarm condition', required: false, defaultValue: false)

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
@Field static final String drvShock = 'QolSys IQ Shock Sensor'

@Field static String partialMessage = ''
@Field static Integer socketReadTimeout = 4  // in minutes

@Field static String driverVersion = '1.1.1'

//
// Commands
//

void installed() {
    logTrace('installed()')
    updateDataValue( 'Version', driverVersion )
    updated()
    runIn(1800, logsOff)
}

void uninstalled() {
    logTrace('uninstalled()')
    unschedule()
    interfaces.rawSocket.close()
}

void updated() {
    logTrace('updated()')
    initialize()
    refresh()
}

void configure() {
    logTrace('configure()')
    unschedule()
}

void initialize() {
    try {
        logTrace('initialize()')
        unschedule()
        logDebug('initialize(): Attempting to close socket if it is open...')
        interfaces.rawSocket.close()
        processEvent( 'connected', 'not connected' )
        processEvent( 'healthStatus', 'offline' )
        logDebug('initialize(): Clearing driver state...')
        state.clear()
        partialMessage = ''

        if (!panelip) {
            logError 'initialize(): IP Address of alarm panel not configured'
        }
        else if (!accessToken) {
            logError 'initialize(): Alarm panel access token not configured.'
        }
        else {
            logTrace("initialize(): Attempting to connect to panel at ${panelip}...")
            interfaces.rawSocket.connect([byteInterface: false, secureSocket: true, ignoreSSLIssues: true, convertReceivedDataToString: true, timeout : (socketReadTimeout * 60000), bufferSize: 10240], panelip, 12345 )
            state.lastMessageReceivedAt = now()
            refresh()
        }
    }
    catch (e) {
        logError( "initialize(): ${panelip} initialize error: ${e.message}, trying again in 1 minute..." )
        runIn(60, 'initialize')
    }
    finally {
        logTrace('exit initialize()')
    }
}

void refresh() {
    logTrace('refresh()')
    String msg = '{ "nonce": "", "action": "INFO", "info_type": "SUMMARY", "version": 1, "source": "C4", "token": "' + accessToken + '"}'
    sendCommand(msg)
}

void armStay( String partitionId, String bypass, BigDecimal userCode = 0 ) {
    logTrace( "armStay partition ${partitionId}" )

    if (checkAllowArming() && checkSecureArming(partitionId, userCode) && checkPartition(partitionId)) {
        String msg = createArmCommand( partitionId, 'ARM_STAY', userCode, 0, bypass)
        processEvent( "Error_Partition_${partitionId}", '(No error)' )
        sendCommand(msg)
    }
}

void armAway( String partitionId, String bypass, BigDecimal delay, BigDecimal userCode = 0 ) {
    logTrace( "armAway partition ${partitionId}" )

    if (checkAllowArming() && checkSecureArming(partitionId, user_code) && checkPartition(partitionId)) {
        String msg = createArmCommand( partitionId, 'ARM_AWAY', userCode, delay, bypass)
        processEvent( "Error_Partition_${partitionId}", '(No error)' )
        sendCommand(msg)
    }
}

void disarm( String partitionId, BigDecimal userCode ) {
    logTrace( "disarm partition ${partitionId}" )

    if (!userCode) {
        logError( 'Alarm panel user code not specified.  You cannot disarm the system without a user code.')
    }
    else if (checkAllowArming() && checkPartition(partitionId)) {
        String msg = createArmCommand( partitionId, 'DISARM', userCode)
        processEvent( "Error_Partition_${partitionId}", '(No error)' )
        sendCommand(msg)
    }
}

void alarm( String partitionId, String alarmType ) {
    logTrace( "initiate alarm ${alarmType}, partition ${partitionId}" )

    if (!(Boolean)settings.AllowTriggerAlarm) {
        logError( 'AllowTriggerAlarm setting must be enabled in order to initiate an alarm.')
    }
    else if (checkPartition(partitionId)) {
        String msg = '{ "nonce": "", "action": "ALARM", "alarm_type": "' + alarmType + '", "version": 1, "source": "C4", "token": "' + accessToken + '", "partition_id":' + partitionId.toString() + ' }'
        logTrace( "initiate alarm: ${msg}" )
        processEvent( "Error_Partition_${partitionId}", '(No error)' )
        sendCommand(msg)
    }
}

private boolean checkAllowArming() {
    Boolean allow = (Boolean)settings.AllowArmAndDisarm
    if (!allow) {
        logError( 'AllowArmAndDisarm setting must be enabled in order to use arm and disarm commands.')
    }
    return allow
}

private boolean checkSecureArming(String partitionId, BigDecimal userCode = 0) {
    if (getDataValue( "Secure_Arm_Partition_${partitionId}" ) && userCode == 0) {
        logError( "Secure arming enabled for partition ${partitionId}.  User code must be specified in order to arm panel.")
        return false
    }

    return true
}

private boolean checkPartition(String partitionId) {
    if (partitionId.toInteger() >= getDataValue( 'Partitions' ).toInteger()) {
        logError( "Partition ${partitionId} is not enabled in the alarm panel.")
        return false
    }

    return true
}

/* groovylint-disable-next-line FactoryMethodName */
private String createArmCommand( String partitionId, String armingType, BigDecimal userCode, BigDecimal delay = 0, String bypass = '' ) {
    String command = '{ "version": 1, "source": "C4", "action": "ARMING", "nonce": "", "token": "' + accessToken + '", "partition_id":' + partitionId + '", "arming_type": "' + armingType + '", '

    if (userCode) {
        command += '"usercode": "' + userCode + '"'
    }

    if (arming_type == 'ARM_STAY' || arming_type == 'ARM_AWAY' ) {
        command += ', "delay": ' + delay + ', "bypass": ' + (bypass == 'Yes' ? 'true' : 'false')
    }

    command += '}'

    return command
}

//
// Socket stuff
//

void socketStatus(String message) {
    if (message == 'receive error: String index out of range: -1') {
        // This is some error condition that repeats every 15ms.
        // Probably a bug in the rawsocket code.  Close the connection to prevent
        // the log being flooded with error messages.
        // Note: this may no longer be needed
        processEvent( 'connected', 'not connected' )
        processEvent( 'healthStatus', 'offline' )
        interfaces.rawSocket.close()
        logError( "socketStatus: ${message}")
        logError( 'Closing connection to alarm panel' )
        initialize()
    }
    else if (message == 'receive error: Read timed out') {
        logInfo("no messages received in ${(now() - state.lastMessageReceivedAt) / 60000} minutes, sending INFO command to panel to test connection...")
        refresh()
        runIn(10, 'connectionCheck')
    }
    else {
        logError( "socketStatus: ${message}, running initialize() in 1 minute...")
        processEvent( 'connected', 'not connected' )
        processEvent( 'healthStatus', 'offline' )
        runIn(60, 'initialize')
    }
}

void parse(String message) {
    logTrace('parse()')
    processEvent( 'connected', 'connected' )
    processEvent( 'healthStatus', 'online' )

    try {
        state.lastMessageReceived = new Date(now()).toString()
        state.lastMessageReceivedAt = now()

        if (message.length() > 0) {
            logDebug("parse() received ${message.length()} bytes : '${message}'")

            if (message.length() >= 3 && message.substring(0, 3) == 'ACK') {
                // This is sent by the panel when a command has been received
                partialMessage = ''
                logDebug("'ACK' received")
            }
            else {
                if (partialMessage != null && partialMessage.length() > 0) {
                    /* groovylint-disable-next-line ParameterReassignment */
                    message = partialMessage + message
                }

                try {
                    Object payload = new JsonSlurper().parseText(message)
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
                                    break

                                case 'ENTRY_DELAY':
                                    processEvent( "Entry_Delay_Partition_${payload.partition_id}", payload.delay )
                                    break

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
                                    break
                            }
                            break

                        case'ALARM':
                            logInfo("Alarm event received: ${payload.alarm_type}")

                            // payload.alarm_type can be "POLICE", "FIRE" OR "AUXILIARY"
                            // If it's empty, append "INTRUSION"

                            // Note there is no distinction made between an audible and silent alarm,
                            // or an alarm initiated by a sensor vs an alarm initiated on the keypad

                            processEvent( "Alarm_Mode_Partition_${payload.partition_id}", 'ALARM_' + payload.alarm_type )
                            break

                        case 'ERROR':
                            logInfo("Error received: '${payload.error_type}' '${payload.description}'")
                            processEvent( "Error_Partition_${payload.partition_id}", "${payload.error_type}: ${payload.description}" )
                            break

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
    catch (e) {
        logError("exception in parse(): ${e}")
    }
    finally {
        logTrace('exit parse()')
    }
}

/* groovylint-disable-next-line UnusedPrivateMethod */
private void connectionCheck() {
    long now = now()

    if ( now - state.lastMessageReceivedAt > 10000) {
        logWarn("Connection check: no messages received in ${(now - state.lastMessageReceivedAt) / 60000} minutes, reconnecting...")
        initialize()
    }
    else {
        logDebug('connectionCheck ok')
    }
}

//
// Internal stuff
//

private void sendCommand(String s) {
    logDebug("sendCommand ${s}")
    interfaces.rawSocket.sendMessage(s)
}

private void processSummary(Object payload) {
    logTrace 'processSummary'

    updateDataValue( 'Partitions', payload.partition_list.size().toString() )

    boolean partitions = payload.partition_list.size() > 1

    payload.partition_list.each {
        try {
            processEvent( "Alarm_Mode_Partition_${it.partition_id}", it.status )
            updateDataValue( "Secure_Arm_Partition_${it.partition_id}", it.secure_arm.toString() )
            updateDataValue( "Partition ${it.partition_id} Name", it.name )
            state.unrecognizedDevices = 'false'

            Map partition = it
            List zoneList = it.zone_list

            zoneList.each {
                switch (it.type) {
                    case [ 'Door_Window', 'Tilt', 'Doorbell' ]:
                        createOrUpdateChildDevice( drvDoorWindow, it, partition, partitions )
                        break

                    case [ 'GlassBreak', 'Panel Glass Break' ]:
                        createOrUpdateChildDevice( drvGlass, it, partition, partitions )
                        break

                    case [ 'SmokeDetector', 'Smoke_M', 'Heat' ]:
                        createOrUpdateChildDevice( drvSmoke, it, partition, partitions )
                        break

                    case 'CODetector':
                        createOrUpdateChildDevice( drvCO, it, partition, partitions )
                        break

                    case [ 'Motion', 'Panel Motion' ]:
                        createOrUpdateChildDevice( drvMotion, it, partition, partitions )
                        break

                    case 'Water':
                        createOrUpdateChildDevice( drvWater, it, partition, partitions )
                        break

                    case 'Auxiliary Pendant':
                        createOrUpdateChildDevice( drvPendant, it, partition, partitions )
                        break

                    case 'TakeoverModule':
                        createOrUpdateChildDevice( drvTakeover, it, partition, partitions )
                        break

                    case 'Shock':
                        createOrUpdateChildDevice( drvShock, it, partition, partitions )
                        break

                    case [ 'Siren', 'Keypad', 'Bluetooth' ]:
                        // ignore types that don't make sense as a child device
                        break

                    default:
                        logError("Unhandled device type ${it.type}")
                        state.unrecognizedDevices = 'true'
                }
            }

            int partitionId = it.partition_id

            logDebug( "Searching for orphaned devices in partition ${partitionId}..." )

            getChildDevices().each {
                String deviceToCheckzoneid = it.getDataValue('zone_id')
                String deviceToCheckpartitionid = it.getDataValue('partition_id').substring(0, 1)
                Object matchingDeviceInUpdatedZoneList = zoneList.find { it.zone_id.toString() == deviceToCheckzoneid && it.partition_id.toString() == deviceToCheckpartitionid }

                if ( matchingDeviceInUpdatedZoneList == null ) {
                    logInfo( "Deleting orphaned device ${it.deviceNetworkId}" )
                    deleteChildDevice(it.deviceNetworkId)
                }
            }
        }
        catch (e) {
            logError("Error in processSummary(): ${e.message}")
        }
    }
}

private void createOrUpdateChildDevice(String deviceName, Map zone, Map partition, boolean partitions) {
    String dni = "${device.deviceNetworkId}-z${zone.zone_id}"
    String name = (partitions ? partition.name + ': ' : '') + zone.name
    try {
        ChildDeviceWrapper currentchild = getChildDevices()?.find { it.deviceNetworkId == dni }
        if (currentchild == null) {
            logDebug "Creating child device '${deviceName}' dni: '${dni}'"
            currentchild = addChildDevice('dcaton-qolsysiqpanel', deviceName, dni, [name: deviceName /* "${zone.name}" */, label: name, isComponent: true])
        }
        else {
            logDebug "Updating existing child device '${deviceName}' dni: '${dni}'"
        }
        currentchild.label = name
        currentchild.with {
            updateDataValue('id', zone.id.toString())
            updateDataValue('type', zone.type)
            updateDataValue('group', zone.group)
            updateDataValue('zone_id', zone.zone_id.toString())
            updateDataValue('zone_physical_type', zone.zone_physical_type.toString())
            updateDataValue('zone_alarm_type', zone.zone_alarm_type.toString())
            updateDataValue('zone_type', zone.zone_type.toString())
            updateDataValue('partition_id', zone.partition_id.toString())
            updateDataValue('partition_name', partition.name)
            ProcessZoneUpdate(zone)
        }
    }
    catch (e) {
        logError("Error creating or updating child device '${dni}': ${e.message}")
    }
}

private void processZoneActive(Map zone) {
    logTrace 'processZoneActive'
    String dni = "${device.deviceNetworkId}-z${zone.zone_id}"
    try {
        ChildDeviceWrapper currentchild = getChildDevices()?.find { it.deviceNetworkId == dni };
        currentchild.ProcessZoneActive(zone)
    }
    catch (e) {
        logError("child device ${dni} not found!  Refreshing device list, ${e.message}")
        refresh()
    }
}

private void processZoneUpdate(Map zone) {
    logTrace 'processZoneUpdate'
    String dni = "${device.deviceNetworkId}-z${zone.zone_id}"
    try {
        ChildDeviceWrapper currentchild = getChildDevices()?.find { it.deviceNetworkId == dni };
        currentchild.ProcessZoneUpdate(zone)
    }
    catch (e) {
        logError("child device ${dni} not found!  Refreshing device list, ${e.message}")
        refresh()
    }
}

/* groovylint-disable-next-line NoDef */
private void processEvent( String variable, def value ) {
    sendEvent( name: "${ variable }", value: value )
}

//
// Logging helpers
//

void logsOff() {
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
