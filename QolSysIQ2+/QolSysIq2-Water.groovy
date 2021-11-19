/*
*  Unofficial QolSys IQ2+ Alarm Panel Integration for Hubitat
*
*  QolSys IQ2+ Alarm Panel Virtual Water Sensor Driver
*
*  Copyright 2021 Don Caton <dcaton1220@gmail.com>
*
*  This is a component driver of the QolSys IQ2+ Alarm Panel Virtual Alarm
*  Panel Driver.  Devices using this driver are automatically created as needed.
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
*
*   Change Log:
*   2021-03-xx: Initial version
*
*/

def version() {"v0.1.0"}

metadata {
    definition(name: "QolSys IQ2+ Water Sensor", namespace: "dcaton-qolsysiq2", author: "Don Caton", component: true, importUrl: "") {
        
        capability "WaterSensor"
        capability "TamperAlert"
    }
}

void updated() {
    state.version = version()
}

void installed() {
    parent.logInfo "QolSys IQ2+ Water Sensor ${device.deviceNetworkId} installed..."
	updated()
}

void parse(String description) { log.warn "parse(String description) not implemented" }

def ProcessZoneActive(zone){
    def water;
    
    switch (zone.status) {
        case "Open":
            water = "wet";
            break;
        case "Closed":
            water = "dry";
            break;
        default:
            water = "unknown (${zone.status})"
    }
        
    if( state.water != water ){
        state.water = water;
        sendEvent( name: "water", value: water, isStateChanged: true )
    }
}

def ProcessZoneUpdate(zone){
    // water - ENUM ["wet", "dry"]
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
