/*
*  Unofficial Sunsa Wands Driver for Hubitat
*
*  Copyright 2021 Don Caton <dcaton1220@gmail.com>
*
*  Please see the documentation at https://app.swaggerhub.com/apis-docs/Sunsa/Sunsa/1.0.4 
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

def version() {"v1.0.6"}

import hubitat.helper.InterfaceUtils
import groovy.json.JsonSlurper
import groovy.transform.Field
import java.net.*;

metadata {
    definition (name: "Sunsa Wands API", namespace: "dcaton.sunsawands", author: "Don Caton", importUrl: "https://raw.githubusercontent.com/dcaton/Hubitat/main/SunsaWands/SunsaWandsAPI.groovy") {
        capability "Initialize"
        capability "Refresh"
        capability "WindowBlind"
        capability "WindowShade"
    }
}

preferences {
    input("userid", "number", title: "User Id", description: "", required: true)
    input("apikey", "string", title: "Api Key", description: "", required: true)
    
    input "logInfo", "bool", title: "Show Info Logs?",  required: false, defaultValue: true
    input "logWarn", "bool", title: "Show Warning Logs?", required: false, defaultValue: true
    input "logDebug", "bool", title: "Show Debug Logs?", description: "Only leave on when required", required: false, defaultValue: true
    input "logTrace", "bool", title: "Show Detailed Logs?", description: "Only leave on when required", required: false, defaultValue: true
}

@Field static final String drvThis = "Sunsa Wands"

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
    refresh();
    
}

def refresh() {
    if (!userid) {
        logError "User Id not configured"
        return
    }
    
    if (!apikey) {
        logError( "Api Key not configured" );
        return
    }
    
    try {
        def Params
        Params = [ uri: "https://sunsahomes.com/api/public/${userid}/devices?publicApiKey=${apikey}", contentType: "application/json" ]
        asynchttpGet( "devicesResponse", Params )    } 
    catch(e) {
        logError( "error getting devices: ${e.message}" );
    }
}

void open() {
    getChildDevices().each { it.open(); }
}

void close() {
    getChildDevices().each { it.close(); }
}

void setPosition(position) { log.warn "setPosition() not applicable to this device"; }

void startPositionChange(direction) { log.warn "startPositionChange() not applicable to this device"; }

void stopPositionChange() { log.warn "stopPositionChange() not applicable to this device"; }

void setTiltLevel(tilt) {
    getChildDevices().each { it.setTiltLevel(tilt); }
}

private def devicesResponse( resp, data ){
    switch( resp.getStatus() ){
        case 200:
            device.deviceNetworkId = "sunsa-${userid}"
            logInfo( "response: ${ resp.data }" )
            if( resp.data != null ){
                Data = parseJson( resp.data );
                logInfo( "json data: ${ Data }" );
                
                for( int i = 0; i < Data.devices.size(); i++ ){
                    CreateChildDevice(Data.devices[i]);
                }
                
                logInfo( "Searching for orphaned devices..." );
                
                getChildDevices().each {                
                    def childDeviceId = it.getDataValue("idDevice");
                    if (Data.devices.find() { it.idDevice = childDeviceId } == null ) {
                        logInfo( "Deleting orphaned device ${it.deviceNetworkId} ${it.idDevice}" );
                        deleteChildDevice(it.deviceNetworkId);    
                    }
                }
                
            } else {
                logError( "No data returned by Sunsa" )
            }
            break
        case 400:
             logError( "Invalid id" )
             break 
        case 401:
             logError( "HTTP 401 error connecting to Sunsa API. Check your userid.");
             break 
        case 404:
            logError( "User not found" )
            break   
        default:
            logError( "Error connecting to Sunsa api: ${ resp.getStatus() }" )
            break
    }
}

private def CreateChildDevice(Data){
    log.debug "json data: ${ Data }"
    
    def dni = "${device.deviceNetworkId}-${Data.idDevice}"; 
    try {
        log.debug "Trying to create child device ${Data.name} dni: ${dni} if it doesn't already exist";
        def currentchild = getChildDevices()?.find { it.deviceNetworkId == dni };
        if (currentchild == null) {
            log.debug "Creating ${Data.name} child for ${dni}"
            currentchild = addChildDevice("dcaton.sunsawands", "Sunsa Wand", dni, [name: "${Data.name}", label: "${Data.name}", isComponent: true])
        }
        currentchild.updateDataValue("idDevice", Data.idDevice.toString());
        currentchild.updateDataValue("blindType", Data.blindType.text);
        currentchild.updateDataValue("blindTypeValue", Data.blindType.value.toString());
        currentchild.updateDataValue("defaultSmartHomeDirection", Data.defaultSmartHomeDirection.text);
        currentchild.updateDataValue("defaultSmartHomeDirectionValue", Data.defaultSmartHomeDirection.value.toString());
        currentchild._InitStates( Data );
    }
    catch (e) {
        logError("Error creating child device ${dni}", e);
    }
}

//
// Internal stuff
//

def setTiltLevel(idDevice, tilt) {
    try {
        def Params
        Params = [ uri: "https://sunsahomes.com/api/public/${userid}/devices/${idDevice}?publicApiKey=${apikey}", contentType: "application/json", body: "{\"Position\": ${tilt}}" ]
        logTrace(Params.uri);
        asynchttpPut( "setTiltLevelResponse", Params );
    } 
    catch(e) {
        logError( "error getting devices: ${e.message}" );
    }
    
}

private def setTiltLevelResponse( resp, data ){
    switch( resp.getStatus() ){
        case 200:
            logInfo( "response: ${ resp.data }" )
            if( resp.data != null ) {
                Data = parseJson( resp.data );
                logInfo( "json data: ${ Data }" );
                
                //def dni = Data.device.idDevivce.toString();
                //dni = "${device.deviceNetworkId}-${dni}"
                
                def dni = "${device.deviceNetworkId}-${Data.device.idDevice}";
                
                logInfo(dni);
                
                try {
                    def currentchild = getChildDevices()?.find { it.deviceNetworkId == dni };
                    currentchild._UpdateTiltLevel(Data.device.Position);
                }
                catch (e) {
                    logError("child device ${dni} not found!  Refreshing device list", e);
                    refresh();
                }
                
            } 
            else {
                logError( "No data returned by Sunsa" )
            }
            break
        case 400:
             logError( "Invalid id" )
             break 
        case 404:
            logError( "User not found" )
            break   
        default:
                logError( "Error connecting to Sunsa api: ${ resp.getStatus() }" )
            break
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