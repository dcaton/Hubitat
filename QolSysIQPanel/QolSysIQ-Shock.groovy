/* groovylint-disable LineLength */
/*
*  QolSys IQ Alarm Panel Virtual Shock Sensor Driver
*
*  Copyright 2024 Don Caton <dcaton1220@gmail.com>
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
*   2024-01-29: Initial version
*
*/

metadata {
    definition(name: 'QolSys IQ Shock Sensor', namespace: 'dcaton-qolsysiqpanel', author: 'Don Caton', component: true, importUrl: 'https://raw.githubusercontent.com/dcaton/Hubitat/main/QolSysIQPanel/QolSysIQ-Shock.groovy') {
        capability 'ShockSensor'
        capability 'TamperAlert'
    }
}

void updated() {
}

void installed() {
    parent.logInfo "QolSys IQ Shock Sensor ${device.deviceNetworkId} installed..."
    updated()
}

void parse(String description) { log.warn 'parse(String description) not implemented' }

void ProcessZoneActive(zone) {
    if ( state.contact != zone.status ) {
        state.contact = zone.status
        sendEvent( name: 'shock', value: zone.status.toLowerCase(), isStateChanged: true )
    }
}

void ProcessZoneUpdate(zone) {
    // contact - ENUM ["clear", "detected"]
    // tamper - ENUM ["clear", "detected"]
    if ( state.contact != zone.status ) {
        state.contact = zone.status
        sendEvent( name: 'shock', value: zone.status.toLowerCase(), isStateChanged: true )
    }
}
