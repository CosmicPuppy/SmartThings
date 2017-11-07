def appVersion() {"1.2.1"}     // Major.Minor.Hotfix (Official releases and pre-releases).

/* ID: jgstexport.sa */
def dataServerUrl() { appSettings.apiPath }
def dataAuthToken() { appSettings.apiKey }


/**
 *  Event Exporter for Jose Garcia
 *
 *  Software is provided without warranty and the software author/license owner cannot be held liable for damages.
 *
 *  Copyright © 2017 Terry Gauchat. Use and modification rights granted to Jose Garcia.
 */

include 'asynchttp_v1'
import java.text.DateFormat
import java.text.SimpleDateFormat

definition(
    name: "Event Exporter",
    namespace: "tgauchat",
    author: "Terry Gauchat",
    description: "Event Exporter for Jose Garcia",
    category: "SmartThings Labs",
	iconUrl: "https://x.com/icons/saX1.png",
	iconX2Url: "https://x.com/icons/saX2.png",
	iconX3Url: "https://x.com/icons/saX3.png",
    oauth: true)

{
	appSetting "apiPath"
	appSetting "apiKey"
}

preferences {
	page name: "mainPage"
    
    page(name: "controlThings", title: "Things", install: false) {
		section("Logs of Things...") {
			input "switches", "capability.switch", title: "Switches...", multiple: true, required: false, description : " "
			input "momentaries", "capability.momentary", title: "Momentary Switches...", multiple: true, required: false, description : " "
            input "presence", "capability.presenceSensor", title: "Presence Sensors...", multiple: true, required: false, description : " "
            input "contacts", "capability.contactSensor", title: "Contact Sensors...", multiple: true, required: false, description : " "
            input "motion", "capability.motionSensor", title: "Motion Sensors...", multiple: true, required: false, description : " "
            input "temperature", "capability.temperatureMeasurement", title: "Temperature...", multiple: true, required: false, description : " "
            input "humidity", "capability.relativeHumidityMeasurement", title: "Hygrometer...", multiple: true, required: false, description : " "
            input "water", "capability.waterSensor", title: "Water Sensors...", multiple: true, required: false, description : " "
            input "battery", "capability.battery", title: "Battery Status...", multiple: true, required: false, description : " "
            input "energy", "capability.energyMeter", title: "Energy Meters...", multiple: true, required: false, description : " "
            input "power", "capability.powerMeter", title: "Power Meters...", multiple: true, required: false, description : " "
            input "acceleration", "capability.accelerationSensor", title: "Vibration Sensors...", multiple: true, required: false, description : " "
            input "luminosity", "capability.illuminanceMeasurement", title: "Luminosity Sensors...", multiple: true, required: false, description : " "
            input "weather", "device.smartweatherStationTile", title: "Weather...", multiple: true, required: false, description : " "
			input "locks", "capability.lock", title: "Locks...", multiple: true, required: false, description : " "
			input "music", "capability.musicPlayer", title: "Music Players...", multiple: true, required: false, description : " "
			input "camera", "capability.imageCapture", title: "Cameras (Image Capture)...", multiple: true, required: false, description : " "
			input "thermostatsHeat", "capability.thermostat", title: "Heating Thermostats...", multiple: true, required: false, description : " "
			input "thermostatsCool", "capability.thermostat", title: "Cooling Thermostats...", multiple: true, required: false, description : " "
        }
	}

  
    page name: "prefs"
}

def mainPage() {
	dynamicPage(name: "mainPage", install: true, uninstall: true) {
		section() {
			label title: "Label", required: false, defaultValue: "Event Exporter"
		}

		section("Options") {
        	input "days", "number", title: "Number of Days to export:", description: "Days Count", defaultValue: 7, required: true
        	input "phone", "phone", title: "Send completed Job ID Text Message to:", description: "Phone Number", required: true
    	}
	
		section() {
			href "controlThings", title:"Things", description : " "
		}

		section() {
			href "prefs", title: "Preferences", description : " "
		}
	}
}

/* TODO: Current unused. */
def prefs() {
	dynamicPage(name: "prefs", title: "Preferences", install: false) {
		section("Debugging Options") {
			input "logLevel", title: "Log Level", "enum", multiple: false, required: true, defaultValue: "Debug", options: ["Error", "Warn", "Info", "Debug", "Trace"]
		}
	}
}


def defaultPrefs(name) {
	if (settings?.containsKey(name)) {
		if (settings[name] == "false") return false
		return settings[name]
	}
	
	def defaults = [
    	days			: 7,
		logLevel		: "Debug"
	]
	
	defaults[name]
}

//----------------------------------------------------
def logLevels() {["Error", "Warn", "Info", "Debug", "Trace"]}
def log_info(obj)  {if (logLevels().indexOf(defaultPrefs("logLevel")) >= logLevels().indexOf("Info")) { log.info obj }}
def log_error(obj) {if (logLevels().indexOf(defaultPrefs("logLevel")) >= logLevels().indexOf("Error")) { log.error obj }}
def log_debug(obj) {if (logLevels().indexOf(defaultPrefs("logLevel")) >= logLevels().indexOf("Debug")) { log.debug obj }}
def log_trace(obj) {if (logLevels().indexOf(defaultPrefs("logLevel")) >= logLevels().indexOf("Trace")) { log.trace obj }}
//-----------------------------------------------------


/* =============================================================================================================*/

def getSanitizedFBUri() {
	def uri = dataServerUrl().trim()
	
	while (uri[-1] == '/') {
		uri = uri[0..-2]
	}
	
	uri
}


def escapeUTF8(string) {
    try {
        return string.collectReplacements{it >= 128 ? "\\u" + String.format("%04X", (int) it) : null}
    } catch (e) {
        log.error "error escapeUTF8 $e"
    }
   
    return string
}


/* sendToFB using asynchttp_v1.patch() */
def sendToFB(data) {
	def uri = "${getSanitizedFBUri()}/JOB-${state.exportId}.json"
    log_trace "Data: ${data}"
	
	def map = [
		uri: "$uri",
		query: [
			print:	"silent",
			auth:	dataAuthToken()
		],
		body: data
	]

	try {
		log_trace "Sending data to FireBase"
		//httpPostJson(map) {}
		asynchttp_v1.patch(null, map)
	} catch (e) {
		log.error ("error writing to database. $e")
	}
} /* sendToFB NEW */


/* sendToFB OLD */
def OLDsendToFB(data) {
	def uri = "${getSanitizedFBUri()}/JOB-${state.exportId}/${getTimestamp()}.json?print=silent&auth=${dataAuthToken()}"
	
	def map = [
		uri: "$uri",
		//body: ["x": "y"],
		body: data,
		//headers : ["x-http-method-override" : "POST"] // Use this to auto generate sequence keys
		//headers : ["x-http-method-override" : "PUT"] // Use this with manually generated sequence keys
	]

	try {
		log_trace "sending $data to FireBase:"
		httpPutJson(map) {}
	} catch (e) {
		log.error ("error writing to database. $e")
	}
		
} /* sendToFB OLD */

//-----------------------------------------------------


def installed() {
	log.info "Installing the SmartApp"
	initialize()
}

def updated() {
	log.info "Updating the SmartApp"
	initialize()
}

def initialize() {
	log.info "Initializing the SmartApp"
	subscribe(app, onAppTouch)
	[status: "ok"]
}

def uninstalled() {
	log.info "Uninstalling the SmartApp"
	revokeAccessToken()
}	

def onAppTouch(evt) {
	log.debug "appTouch with Event: $evt"
	history();
}


def handleError(e, method) {
	def error = [message: "\"Unexpected error. \"",
		error: "\"$e\"", method: "\"${method}\""]
	log.error(error)
	error
}

def getEventsOfDevice(device,start,end) {
	
	log_trace "getEventsOfDevice: ${device}; Start: ${start}; End: ${end}"
	
	/* Alternative: http://docs.smartthings.com/en/latest/ref-docs/device-ref.html#eventssince */
    //OLD: def result = device.eventsBetween(then, today, [max: 200])?.findAll{"$it.source" == "DEVICE"}?.collect{[

	def result = device.eventsBetween(start, end)?.collect{
        String source = it.source; // Source seems to be a map or enum with extraneous properties. Strip to just String.
        [ "${formatDateForKey(it.date)}" :
            [
                "date": formatDate(it.date),
                "deviceID": it.deviceId,
                "name": it.name,
                "displayName": it.displayName,
                "description": it.description,
                "descriptionText": it.descriptionText,
                "unit": it.unit,
                "source": source,
                "value": it.value,
                "isDigital": it.isDigital(),
                "isPhysical": it.isPhysical(),
                "isStateChange": it.isStateChange()
            ]
        ]
	}

/* TODO: sentToFB in bulk should work; but this creates unintended array index branches. Defer. */
/*
	def jsonOutput = new groovy.json.JsonOutput()
	//def myData = jsonOutput.toJson(result)
    def myData = result
    log_trace myData
    
    //log_trace "Events of Device: ${result}"
    //result.each { log_info "EVENT: ${it}" }
	sendToFB( myData )
*/

	def myData = [:]
	result.each {
		myData = it
		log_trace "Sending: ${myData}"
		sendToFB( myData )
	}

    result
}


def renderEvent(data) {
	//return "$data.deviceType, $data.name, $data.value, $data.displayName, $data.value, ${data.unit ?: ""}, ${formatDate(data.date)}"
	return "$data"
}


def filterEventsPerCapability(events, deviceType) {
	def acceptableEventsPerCapability = [
		switch          : ["switch"],
		dimmer          : ["switch", "level"],
		momentary       : ["switch"],
		thermostatHeat  : ["temperature", "heatingSetpoint", "thermostatFanMode", "thermostatOperatingState",],
		thermostatCool  : ["temperature", "coolingSetpoint", "thermostatFanMode", "thermostatOperatingState",],
		lock            : ["lock"],
		music           : ["status", "level", "trackDescription", "mute"],
		camera          : [],
		presence        : ["presence"],
		contact         : ["contact"],
		motion          : ["motion"],
		temperature     : ["temperature"],
		humidity        : ["humidity"],
		water           : ["water"],
		battery         : ["battery"],
		energy          : ["energy"],
		power           : ["power"],
		acceleration    : ["acceleration"],
		luminosity      : ["illuminance"],
		weather         : ["temperature", "weather"],
	]
	
	if (events) events*.deviceType = deviceType
	def result = events?.findAll{it.name in acceptableEventsPerCapability[deviceType]}
	result
}

/*
 * TODO: Should count Events to report in log and text.
 * TODO: Should reduce Firebase calls by batching sendToFBs
 */
def getAllDeviceEvents(data) {
	int ago = data.ago
	//log_trace "start getAllDeviceEvents: $data"
	
	def todayUnix = data.todayUnix
	def today = new Date(todayUnix)
	def start = new Date()
	def end = new Date()
	
	end = today - ago
	start = end - 1

	def eventsPerCapability = [
		switch          : switches              ?.collect{getEventsOfDevice(it,start,end)},
		momentary       : momentaries           ?.collect{getEventsOfDevice(it,start,end)},
		thermostatHeat  : thermostatsHeat       ?.collect{getEventsOfDevice(it,start,end)},
		thermostatCool  : thermostatsCool       ?.collect{getEventsOfDevice(it,start,end)},
		lock            : locks                 ?.collect{getEventsOfDevice(it,start,end)},
		music           : music                 ?.collect{getEventsOfDevice(it,start,end)},
		camera          : camera                ?.collect{getEventsOfDevice(it,start,end)},
		presence        : presence              ?.collect{getEventsOfDevice(it,start,end)},
		contact         : contacts              ?.collect{getEventsOfDevice(it,start,end)},
		motion          : motion                ?.collect{getEventsOfDevice(it,start,end)},
		temperature     : temperature           ?.collect{getEventsOfDevice(it,start,end)},
		humidity        : humidity              ?.collect{getEventsOfDevice(it,start,end)},
		water           : water                 ?.collect{getEventsOfDevice(it,start,end)},
		battery         : battery               ?.collect{getEventsOfDevice(it,start,end)},
		energy          : energy                ?.collect{getEventsOfDevice(it,start,end)},
		power           : power                 ?.collect{getEventsOfDevice(it,start,end)},
		acceleration    : acceleration          ?.collect{getEventsOfDevice(it,start,end)},
		luminosity      : luminosity            ?.collect{getEventsOfDevice(it,start,end)},
		weather         : weather               ?.collect{getEventsOfDevice(it,start,end)},
	]

	ago = ago + 1;
	if ( ago < days ) {
		log_trace "Calling getAll: ago = ${ago}."
		runIn(2, "getAllDeviceEvents", [data: [ago: ago, todayUnix: todayUnix]])
        log_info "[${app.label}] Spawned Event Fetch #${ago+1} for Job ID: JOB-${state.exportId}"
	} else {
		log_trace "No more calls: ago = ${ago}."
        log_info "[${app.label}] Ended Job ID: JOB-${state.exportId}"
        if( phone ) {
        	sendSms( phone, "[${app.label}] Ended Job ID: JOB-${state.exportId}" )
        }
	}
	
    log_trace "Events: ${eventsPerCapability}"
	return eventsPerCapability
}


def history() {
	log_debug "History..."

	//log.debug "${getAllDeviceEvents()?.collect{renderEvent(it)}.join("\n")}"
	
	/* TODO: Start point could be an input parameter. Assume starting immediately today. */
    def ago = 0
	def todayUnix = new Date().getTime();
	state.exportId = getExportId();
    log.info  "[${app.label}] Starting Job ID: JOB-${state.exportId}"
	
    log_debug "Calling runIn getAllDeviceEvents in 2 seconds."
    runIn(2, "getAllDeviceEvents", [data: [ago: ago, todayUnix: todayUnix]])
    log_info "[${app.label}] Spawned Event Fetch #${ago+1} for Job ID: JOB-${state.exportId}"
}


def getDeviceData(device, type) {[tile: "device",  active: isActive(device, type), type: type, device: device.id, name: device.displayName, value: getDeviceValue(device, type), level: getDeviceLevel(device, type), isValue: isValue(device, type)]}

def getDeviceFieldMap() {[lock: "lock", "switch": "switch", dimmer: "switch", contact: "contact", presence: "presence", temperature: "temperature", humidity: "humidity", luminosity: "illuminance", motion: "motion", acceleration: "acceleration", water: "water", power: "power", energy: "energy", battery: "battery"]}

def isValue(device, type) {!(["momentary", "camera"] << getActiveDeviceMap().keySet()).flatten().contains(type)}

def getDeviceValue(device, type) {
	try {
	log_trace "getDeviceValue $device, $type"
	def unitMap = [temperature: "°", humidity: "%", luminosity: "lx", battery: "%", power: "W", energy: "kWh"]
	def field = getDeviceFieldMap()[type]
	def value = "n/a"
	try {
		value = device.respondsTo("currentValue") ? device.currentValue(field) : device.value
	} catch (e) {
		log_error "Device $device ($type) does not report $field properly. This is probably due to numerical value returned as text."
	}
	if (!isValue(device, type)) return value
	else return "${roundNumber(value)}${unitMap[type] ?: ""}"
	} catch (e) {
		handleError(e, "getDeviceValue $device, $type");
	}
}

def getDeviceLevel(device, type) {
	log_trace "getDeviceLevel $device, $type"
	if (type == "dimmer" || type == "dimmerLight" || type == "music") return "${(device.currentValue("level") ?: 0) / 10.0}".toDouble().round() ?: 1
}

def handler(e) {
	try {
		log_debug "event from: $e.displayName, value: $e.value, source: $e.source, description: $e.description"
		updateStateTS()
	} catch (ee) {
		handleError(ee, "handler")
	}
}

def getExportId() {
	def ei = new java.text.SimpleDateFormat("yyyyMMdd-hhmmss")
    //if (location?.timeZone) tf.setTimeZone(location.timeZone)
    "${ei.format(new Date())}"
}

def getTimestamp() {
	def ei = new java.text.SimpleDateFormat("yyyyMMdd-hhmmss_SSS")
    //if (location?.timeZone) tf.setTimeZone(location.timeZone)
    "${ei.format(new Date())}"
}

def getTS() {
	def tf = new java.text.SimpleDateFormat("h:mm a")
    if (location?.timeZone) tf.setTimeZone(location.timeZone)
    "${tf.format(new Date())}"
}

def getDate() {
	def tf = new java.text.SimpleDateFormat("MMMMM d")
    if (location?.timeZone) tf.setTimeZone(location.timeZone)
    "${tf.format(new Date())}"
}

def formatDate(date) {
	def tf = new java.text.SimpleDateFormat("YYYY-MM-dd hh:mm:ss.SSS")
    //if (location?.timeZone) tf.setTimeZone(location.timeZone)
	return tf.format(date)
}

def formatDateForKey(date) {
	def ei = new java.text.SimpleDateFormat("yyyyMMdd-hhmmss_SSS")
    //if (location?.timeZone) tf.setTimeZone(location.timeZone)
    return "${ei.format(date)}"
}

def getDOW() {
	def tf = new java.text.SimpleDateFormat("EEEE")
    if (location?.timeZone) tf.setTimeZone(location.timeZone)
    "${tf.format(new Date())}"
}


/* =========== */
/* End of File */
/* =========== */
