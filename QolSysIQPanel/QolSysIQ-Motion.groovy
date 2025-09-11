/*
*  QolSys IQ Alarm Panel Virtual Motion Sensor Driver
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
    definition(name: "QolSys IQ Motion Sensor", namespace: "dcaton-qolsysiqpanel", author: "Don Caton", component: true, importUrl: "https://raw.githubusercontent.com/dcaton/Hubitat/main/QolSysIQPanel/QolSysIQ-Motion.groovy") {
        
        capability "MotionSensor"
        // capability "TamperAlert"
    }
}

void updated() {
}

void installed() {
    parent.logInfo "QolSys IQ Motion Detector ${device.deviceNetworkId} installed..."
	updated()
}

void parse(String description) { log.warn "parse(String description) not implemented" }

def ProcessZoneActive(zone){
    def motion;
    
    switch (zone.status) {
        case "Open":
            motion = "active";
            break;
        case "Closed":
            motion = "inactive";
            break;
        default:
            motion = "unknown (${zone.status})"
    }
        
    sendEvent( name: "motion", value: motion )
}

def ProcessZoneUpdate(zone){
    // motion - ENUM ["inactive", "active"]
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
