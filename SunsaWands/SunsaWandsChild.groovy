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
*  SOFTWARE.def version() {"v0.1.0"}
*
*/

def version() {"v1.0.0"}

metadata {
    definition(name: "Sunsa Wand", namespace: "dcaton-sunsawands", author: "Don Caton", component: true, importUrl: "https://raw.githubusercontent.com/dcaton/Hubitat-Sunsa-Wands/main/SunsaWandChild.groovy") {
        capability "WindowBlind"
        capability "Battery"
        capability "TemperatureMeasurement"
    }
}

preferences {
    input(name: "closeDirection", type: "enum", options: ["Up", "Down"], title: "Close Direction", description: "", required: true, defaultValue: "Up")
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
     setTiltLevel(settings.closeDirection == "Down" ? 100 : -100);
}

void setPosition(position) { log.warn "setPosition() not applicable to this device"; }

void startPositionChange(direction) { log.warn "startPositionChange() not applicable to this device"; }

void stopPositionChange() { log.warn "stopPositionChange() not applicable to this device"; }

void setTiltLevel(tilt) {
     parent.setTiltLevel(getDataValue("idDevice"), tilt);
}

def void _InitStates(tilt, battery, temperature) {
    state.battery = battery;
    sendEvent( name: "battery", value: battery, isStateChanged: true );
    state.temperature = temperature;
    sendEvent( name: "temperature", value: temperature, isStateChanged: true );
    _UpdateTiltLevel(tilt);
}

def void _UpdateTiltLevel(tilt) {
    // windowShade - ENUM ["opening", "partially open", "closed", "open", "closing", "unknown"]
    if (state.tilt != tilt ) {
        state.tilt = tilt;
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
        
        state.windowShade = shadeval;
        sendEvent( name: "windowShade", value: shadeval, isStateChanged: true );
    }
}