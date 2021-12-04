/*
*  QolSys IQ Alarm Panel Virtual Alarm Contact Driver
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
*   2021-12-03: Initial version
*
*   Notes:
*   ======
*   This driver represents a "takeover module", also called a "hardwire translator".  This device
*   adds hardwired zones to the IQ2 panel, allowing things like hard-wired smoke and CO detectors
*   and existing alarm sensors from an older non-wireless panel (hence the name "takeover module").
*
*   This driver supports the PG9WLSHW8 PowerG expansion module, and no doubt other modules as well
*   that identify themselves as a takeover module.  While the IQ2 panel reports open/closed status
*   for the PG9WLSHW8, it is not clear exactly what "open" and "closed" represents.
*/

metadata {
    definition(name: "QolSys IQ Takeover Module", namespace: "dcaton-qolsysiqpanel", author: "Don Caton", component: true, importUrl: "https://raw.githubusercontent.com/dcaton/Hubitat/main/QolSysIQPanel/QolSysIQ-Contact.groovy") {
        
        capability "ContactSensor"
    }
}

void updated() {
}

void installed() {
    parent.logInfo "QolSys IQ Takeover Module ${device.deviceNetworkId} installed..."
    updated()
}

void parse(String description) { log.warn "parse(String description) not implemented" }

def ProcessZoneActive(zone){
    if( state.contact != zone.status ){
        state.contact = zone.status;
        sendEvent( name: "contact", value: zone.status.toLowerCase(), isStateChanged: true )
    }
}

def ProcessZoneUpdate(zone){
    // contact - ENUM ["closed", "open"]
    // tamper - ENUM ["clear", "detected"]    
    if( state.contact != zone.status ){
        state.contact = zone.status;
        sendEvent( name: "contact", value: zone.status.toLowerCase(), isStateChanged: true )
    }
}
