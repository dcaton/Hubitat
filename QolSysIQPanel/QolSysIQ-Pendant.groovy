/*
*  QolSys IQ Alarm Panel Auxiliary Pendant Driver
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
    definition(name: "QolSys IQ Auxiliary Pendant", namespace: "dcaton-qolsysiqpanel", author: "Don Caton", component: true, importUrl: "https://raw.githubusercontent.com/dcaton/Hubitat/main/QolSysIQPanel/QolSysIQ-Pendant.groovy") {
        
        capability "PushableButton"
    }
}

void updated() {
    state.numberOfButtons = 1;
}

void installed() {
    parent.logInfo "QolSys IQ Auxiliary Pendant ${device.deviceNetworkId} installed..."
	updated()
}

void parse(String description) { log.warn "parse(String description) not implemented" }

void push(button) { log.warn "push() not applicable for this device; device is read-only"}

def ProcessZoneActive(zone){
    def pushed = 0;
    
    switch (zone.status) {
        case "Open":
            pushed = 1;
            break;
        case "Closed":
        default:
            pushed = 0;
            break;
    }
        
    sendEvent( name: "pushed", value: pushed )
}

def ProcessZoneUpdate(zone){
    // tamper - ENUM ["clear", "detected"]
    ProcessZoneActive(zone);
    
/*    
    def tamper;
    
    switch (zone.state) {
        case 0:
            status = "clear";
            break;
        default:
            status = "unknown (${zone.state})"
    }
        
    sendEvent( name: "tamper", value: tamper )
*/
}
