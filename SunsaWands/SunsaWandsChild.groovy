/*
*  Unofficial Sunsa Wand Integration for Hubitat
*
*  Sunsa Wand Child Driver
*
*  Copyright 2021 Don Caton <dcaton1220@gmail.com>
*
*  This is a component driver of the Sunsa Wands driver.
*  Devices using this driver are automatically created as needed.
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
*  SOFTWARE.
*
*/

def version() {"v1.0.6"}

metadata {
    definition(name: "Sunsa Wand", namespace: "dcaton.sunsawands", author: "Don Caton", component: true, importUrl: "https://raw.githubusercontent.com/dcaton/Hubitat/main/SunsaWands/SunsaWandsChild.groovy") {
        capability "WindowBlind"
        capability "Battery"
        capability "TemperatureMeasurement"
        capability "IlluminanceMeasurement"
    }
}

preferences {
    input(name: "closeDirection", type: "enum", options: ["Up", "Down", "Left", "Right"], title: "Close Direction", description: "", required: true, defaultValue: "Up")
}

void installed() {
    parent.logInfo "Sunsa Wand ${device.deviceNetworkId} installed..."
    updated()
}

void uninstalled() {
}

void updated() {
    state.version = version()
}

void parse(String description) { log.warn "parse(String description) not implemented" }

void open() {
     setTiltLevel(0);
}

void close() {
     setTiltLevel(settings.closeDirection in ["Down", "Right"] ? 100 : -100);
}

void setPosition(position) { log.warn "setPosition() not applicable to this device"; }

void startPositionChange(direction) { log.warn "startPositionChange() not applicable to this device"; }

void stopPositionChange() { log.warn "stopPositionChange() not applicable to this device"; }

void setTiltLevel(tilt) {
     parent.setTiltLevel(getDataValue("idDevice"), tilt);
}

def void _InitStates(Data) {
    sendEvent( name: "battery", value: Data.batteryPercentage, isStateChanged: true );
    
    // light sensor not yet supported in current API
    //sendEvent( name: "illuminance", value: ?, isStateChanged: true );
    
    // temp not yet supported in current API
    //sendEvent( name: "temperature", value: ?, isStateChanged: true );
    
    _UpdateTiltLevel(Data.Position);
}

def void _UpdateTiltLevel(tilt) {

    // API doesn't support starting and stopping wand, so "opening" and "closing" values are never set
    
    // windowShade - ENUM ["opening", "partially open", "closed", "open", "closing", "unknown"]
    
    if (state.tilt != tilt ) {
        sendEvent( name: "tilt", value: tilt, isStateChanged: true );
        
        def shadeval;
        switch (tilt) {
            case 0:
                shadeval = "open";
                break;
            case 100:
            case -100:
                shadeval = "closed";
                break;
            default:
                shadeval = "partially open";
                break;
        }
        
        sendEvent( name: "windowShade", value: shadeval, isStateChanged: true );
    }
}