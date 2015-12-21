def myVersion() { "v0.1.2-Alpha+003" }
/**
 *  Copyright 2015 SmartThings
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
 *	SmartPower Outlet (CentraLite)
 *
 *	Author: SmartThings
 *	Date: 2015-08-23
 *
 *  Modified: Terry R. Gauchat, @CosmicPuppy
 *  Date: 2015-12-18
 *  	- Attempting to mutate this for single switch window blind control.
 * 	 	- Overwrite on/off to mean up/down (i.e., blinds open/closed).
 *		- NB: Using "on" for blinds up/open because on means let there be light!
 *
 *  Version: 0.1.2-Alpha+003
 */
metadata {
	// Automatically generated. Make future change here.
	definition (name: "Custom Smart Blinds", namespace: "cosmicpuppy", author: "Terry Gauchat") {
		capability "Actuator"
		capability "Switch"
		capability "Power Meter"
		capability "Configuration"
		capability "Refresh"
		capability "Sensor"

		// indicates that device keeps track of heartbeat (in state.heartbeat)
		attribute "heartbeat", "string"

		// Internal attribute for holding actual status of power outlet (on,off).
		attribute "outlet", "string"
        
        // Temporary attribute to track version number during development.
        attribute "version", "string"

		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0B04,0B05", outClusters: "0019", manufacturer: "CentraLite",  model: "3200", deviceJoinName: "Outlet"
		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0B04,0B05", outClusters: "0019", manufacturer: "CentraLite",  model: "3200-Sgb", deviceJoinName: "Outlet"
		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0B04,0B05", outClusters: "0019", manufacturer: "CentraLite",  model: "4257050-RZHAC", deviceJoinName: "Outlet"
		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0B04,0B05", outClusters: "0019"
	}

	// simulator metadata
	simulator {
		// status messages
		status "on": "on/off: 1"
		status "off": "on/off: 0"

		// reply messages
		reply "zcl on-off on": "on/off: 1"
		reply "zcl on-off off": "on/off: 0"
	}

	preferences {
		section {
			image(name: 'educationalcontent', multiple: true, images: [
				"http://cdn.device-gse.smartthings.com/Outlet/US/OutletUS1.jpg",
				"http://cdn.device-gse.smartthings.com/Outlet/US/OutletUS2.jpg"
				])
		}
	}

	// UI tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState: "turningOff"
				attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
				attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState: "turningOff"
				attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
			}
			tileAttribute ("power", key: "SECONDARY_CONTROL") {
				attributeState "power", label:'${currentValue} W'
			}
		}

		standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		valueTile("outlet", "device.outlet", decoration: "flat", width: 2, height: 2) {
			state "outlet", label:'Outlet State: ${currentValue}'
		}

		valueTile("version", "device.version", decoration: "flat", width: 2, height: 2) {
			state "version", label:'Version: ${currentValue}'
		}

		main "switch"
		details(["switch","refresh","outlet","version"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "description is $description"

	// save heartbeat (i.e. last time we got a message from device)
	state.heartbeat = Calendar.getInstance().getTimeInMillis()

	def finalResult = zigbee.getKnownDescription(description)

	//TODO: Remove this after getKnownDescription can parse it automatically
	if (!finalResult && description!="updated")
		finalResult = getPowerDescription(zigbee.parseDescriptionAsMap(description))

	if (finalResult) {
		log.info finalResult
		if (finalResult.type == "update") {
			log.info "$device updates: ${finalResult.value}"
		}
		else if (finalResult.type == "power") {
			def powerValue = (finalResult.value as Integer)/10
			sendEvent(name: "power", value: powerValue)
			/*
				Dividing by 10 as the Divisor is 10000 and unit is kW for the device. AttrId: 0302 and 0300. Simplifying to 10

				power level is an integer. The exact power level with correct units needs to be handled in the device type
				to account for the different Divisor value (AttrId: 0302) and POWER Unit (AttrId: 0300). CLUSTER for simple metering is 0702
			*/
		}
		else {
        	log.trace "Unprocessed type: finalResult.type, value: ${finalResult.type}, ${finalResult.value}"
            if (finalResult?.type == "switch") {
            	finalResult.type = "outlet"
            }
            log.trace "Processed type: finalResult.type, value: ${finalResult.type}, ${finalResult.value}"
			sendEvent(name: finalResult.type, value: finalResult.value)
		}
	}
	else {
		log.warn "DID NOT PARSE MESSAGE for description : $description"
		log.debug zigbee.parseDescriptionAsMap(description)
	}
}


/* New definition of on/off = up/down = open/closed Window Blinds */
/* Since we don't know the starting State, for sanity assume NULL = off = down = closed. */
def on() {
    if( state?.blindsPosition == NULL ) {
    	state.blindsPosition = "down"
    }
	/* If blinds not already up/on, then flip outlet off and back on. */
	if( state?.blindsPosition != "up" ) {
		log.trace "On() Requested: Blinds not up, so flipping outlet off and on."
		delayBetween([
        	outletOff(),
            sendEvent(name: "outlet", value: "turning off", isStateChange: true ),
        	outletOn(),
            sendEvent(name: "outlet", value: "turning on", isStateChange: true )
    	], 1200)
        state.blindsPosition = "up"
        sendEvent(name: "switch", value: "on", isStateChange: true, descriptionText: "Opening Blinds Up to On");
	} else {
    	/* Assume that the blinds are already up. */
        log.trace "On() Requested: Blinds are already up-on-open. Doing nothing."
    	//state.blindsPosition = "up"
    }
    sendEvent(name: "version", value: myVersion(), isStateChange: true);
}

/* This time we _also_ treat NULL as if already off = down = closed. */
def off() {
	/* If blinds already down down/off, then flip outlet off and back on. */
    if( state?.blindsPosition == NULL ) {
    	state.blindsPosition = "down"
    }
    if( state?.blindsPosition != "down" ) {
    	log.trace "Off() Requested: Blinds not down, so flipping outlet off and on."
    	delayBetween([
        	outletOff(),
            sendEvent(name: "outlet", value: "turning off", isStateChange: true ),
        	outletOn(),
            sendEvent(name: "outlet", value: "turning on", isStateChange: true )
    	], 1200)
        state.blindsPosition = "down"
        sendEvent(name: "switch", value: "off", isStateChange: true, descriptionText: "Lowering Blinds Down to Off");
	} else {
    	/* Assume that the blinds are already down. */
        log.trace "On() Requested: Blinds are already down-off-closed or NULL. Doing nothing."
    	//state.blindsPosition = "down"
	}
    sendEvent(name: "version", value: myVersion(), isStateChange: true);
}


def outletOff() {
	zigbee.off()
}

def outletOn() {
	zigbee.on()
}

def refresh() {
	sendEvent(name: "heartbeat", value: "alive", displayed:false)
	zigbee.onOffRefresh() + zigbee.refreshData("0x0B04", "0x050B")
}

def configure() {
	zigbee.onOffConfig() + powerConfig() + refresh()
}

//power config for devices with min reporting interval as 1 seconds and reporting interval if no activity as 10min (600s)
//min change in value is 01
def powerConfig() {
	[
		"zdo bind 0x${device.deviceNetworkId} 1 ${endpointId} 0x0B04 {${device.zigbeeId}} {}", "delay 200",
		"zcl global send-me-a-report 0x0B04 0x050B 0x29 1 600 {05 00}",				//The send-me-a-report is custom to the attribute type for CentraLite
		"send 0x${device.deviceNetworkId} 1 ${endpointId}", "delay 500"
	]
}

private getEndpointId() {
	new BigInteger(device.endpointId, 16).toString()
}

//TODO: Remove this after getKnownDescription can parse it automatically
def getPowerDescription(descMap) {
	def powerValue = "undefined"
	if (descMap.cluster == "0B04") {
		if (descMap.attrId == "050b") {
			if(descMap.value!="ffff")
				powerValue = zigbee.convertHexToInt(descMap.value)
		}
	}
	else if (descMap.clusterId == "0B04") {
		if(descMap.command=="07"){
			return	[type: "update", value : "power (0B04) capability configured successfully"]
		}
	}

	if (powerValue != "undefined"){
		return	[type: "power", value : powerValue]
	}
	else {
		return [:]
	}
}