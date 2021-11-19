/*
*  Unofficial QolSys IQ2+ Alarm Panel Integration for Hubitat
*
*  QolSys IQ2+ Alarm Panel Virtual Alarm Contact Driver
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
*/

def version() {"v0.1.0"}

metadata {
    definition(name: "QolSys IQ2+ Door/Window Sensor", namespace: "dcaton-qolsysiq2", author: "Don Caton", component: true, importUrl: "") {
        
        capability "ContactSensor"
        capability "TamperAlert"
    }
}

void updated() {
    state.version = version()
}

void installed() {
    parent.logInfo "QolSys IQ2+ Door/Window Sensor ${device.deviceNetworkId} installed..."
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
