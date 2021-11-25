/*
*  QolSys IQ Alarm Panel Virtual Carbon Monoxide Detector Driver
*
*  Copyright 2021 Don Caton <dcaton1220@gmail.com>
*
*  This is a component driver of the QolSys IQ Alarm Panel Virtual Alarm
*  Panel Driver.  Devices using this driver are automatically created as needed.
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

metadata {
    definition(name: "QolSys IQ Carbon Monoxide Detector", namespace: "dcaton-qolsysiqpanel", author: "Don Caton", component: true, importUrl: "https://raw.githubusercontent.com/dcaton/Hubitat/main/QolSysIQPanel/QolSysIQ-CO.groovy") {
        
        capability "CarbonMonoxideDetector"
        capability "TamperAlert"
    }
}

void updated() {
}

void installed() {
    parent.logInfo "QolSys IQ Carbon Monoxide Detector ${device.deviceNetworkId} installed..."
    updated()
}

void parse(String description) { log.warn "parse(String description) not implemented" }

def ProcessZoneActive(zone){
    def status;
    
    switch (zone.status) {
        case "Closed":
            status = "clear";
            break;
        case "Open":
            status = "detected";
            break;
        default:
            status = "unknown (${zone.status})"
    }
        
    if( state.carbonMonoxide != status ){
        state.carbonMonoxide = status;
        sendEvent( name: "carbonMonoxide", value: status, isStateChanged: true )
    }
}

def ProcessZoneUpdate(zone){
    // carbonMonoxide - ENUM ["clear", "tested", "detected"]
    // tamper - ENUM ["clear", "detected"]
    ProcessZoneActive(zone);
    def tamper;
    
    switch (zone.state) {
        case 0:
            status = "clear";
            break;
        default:
            status = "unknown (${zone.state})"
    }
        
    if( state.tamper != tamper ){
        state.tamper = tamper;
        sendEvent( name: "tamper", value: tamper, isStateChanged: true )
    }
}
