def appVersion()  {"1.0.5"}     // Major.Minor.Hotfix (Official releases and pre-releases).

/* ID: jgstexport.sa */
def dataServerUrl() {"https://jgstexport.firebaseio.com/"}
def dataAuthToken() {"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjg2NTUwOTg2NzgzMiwidiI6MCwiZCI6eyJ1aWQiOiJqZ3N0ZXhwb3J0LnNhIn0sImlhdCI6MTUwOTk1NDIzMn0.eLTgQ70PwJmHHmwyXm-HD6hjbH0FsfqXaR0xJ1P0pHE"}


/**
 *  Event Exporter
 *
 *  Visit Home Page for more information:
 *
 *  Software is provided without warranty and the software author/license owner cannot be held liable for damages.
 *
 *  Copyright © 2017 Terry Gauchat
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



preferences {
	page name: "mainPage"
    
    page(name: "controlThings", title: "Things", install: false) {
		section("Control lights...") {
			input "switches", "capability.switch", title: "Switches...", multiple: true, required: false, description : " "
			input "dimmers", "capability.switchLevel", title: "Dimmable Switches...", multiple: true, required: false, description : " "
			input "momentaries", "capability.momentary", title: "Momentary Switches...", multiple: true, required: false, description : " "
		}
		
		section("Control thermostats...") {
			input "thermostatsHeat", "capability.thermostat", title: "Heating Thermostats...", multiple: true, required: false, description : " "
			input "thermostatsCool", "capability.thermostat", title: "Cooling Thermostats...", multiple: true, required: false, description : " "
		}
		
		section("Control things...") {
			input "locks", "capability.lock", title: "Locks...", multiple: true, required: false, description : " "
			input "music", "capability.musicPlayer", title: "Music Players...", multiple: true, required: false, description : " "
			input "camera", "capability.imageCapture", title: "Cameras (Image Capture)...", multiple: true, required: false, description : " "
		}
		
		section("View state of things...") {
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
        }
		
	}
    
    page name: "prefs"
}

def mainPage() {
	dynamicPage(name: "mainPage", install: true, uninstall: true) {
		section() {
			label title: "Label", required: false, defaultValue: "Event Exporter"
		}
	
		section() {
			href "controlThings", title:"Things", description : " "
		}

		section() {
			href "prefs", title: "Preferences", description : " "
		}
	}
}


def prefs() {
	dynamicPage(name: "prefs", title: "Preferences", install: false) {
		section() {
			input "roundNumbers", title: "Round Off Decimals", "bool", required: true, defaultValue:true
		}
		
		section("Debugging Options") {
			input "logLevel", title: "Log Level", "enum", multiple: false, required: true, defaultValue: "Debug", options: ["Error", "Warn", "Info", "Debug", "Trace"]
		}
	}
}

/*
def defaultPrefs(name) {
	if (settings?.containsKey(name)) {
		if (settings[name] == "false") return false
		return settings[name]
	}
	
	def defaults = [
		showSHM			: false,
		showMode		: true,
		showHelloHome 	: true,
		showRefresh 	: true,
		showHistory 	: true,
		showClock 		: "Small Digital",
		clockFormat		: "12 Hour Clock",
		theme		 	: "default",
		tileSize 		: "Medium",
		videoTileSize	: "Small",
		fontSize	 	: "Normal",
		roundNumbers 	: true,
		themeLightType 	: "Default",
		pollingRate		: 30,
		smvRate			: 5,
		logLevel		: "Debug"
	]
	
	defaults[name]
}
*/

//----------------------------------------------------

def logLevels() {["Error", "Warn", "Info", "Debug", "Trace"]}
def log_info(obj)  {if (9 >= logLevels().indexOf("Info")) { log.info obj }}
def log_error(obj) {if (9 >= logLevels().indexOf("Error")) { log.error obj }}
def log_debug(obj) {if (0 >= logLevels().indexOf("Debug")) { log.debug obj }}
def log_trace(obj) {if (9 >= logLevels().indexOf("Trace")) { log.trace obj }}

//-----------------------------------------------------
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


/* sendToFB NEW */
def sendToFB(data) {
	def uri = "${getSanitizedFBUri()}/JOB-${state.exportId}/${getTimestamp()}.json"
	
	def map = [
		uri: "$uri",
		query: [
			print:	"silent",
			auth:	dataAuthToken()
		],
		//body: ["x": "y"],
		body: data,
		//headers : ["x-http-method-override" : "POST"] // Use this to auto generate sequence keys
		headers : ["x-http-method-override" : "PUT"] // Use this with manually generated sequence keys
	]

	try {
		log.debug "sending data to FireBase"
		//httpPutJson(map) {}
		asynchttp_v1.put(null, map)
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
		headers : ["x-http-method-override" : "PUT"] // Use this with manually generated sequence keys
	]

	try {
		log.debug "sending $data to FireBase:"
		httpPostJson(map) {}
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
    //def result = device.eventsBetween(then, today, [max: 200])?.findAll{"$it.source" == "DEVICE"}?.collect{[

	/*
	def result = device.eventsBetween(start, end)?.collect{[
		"time" : timestamp,
        "date": it.date,
		"deviceID": it.deviceId,
		"name": it.name,
        "displayName": it.displayName
		//description: it.description,
		//descriptionText: it.descriptionText,
		//unit: it.unit,
		//source: it.source,
		//value: it.value,
		//isDigital: it.isDigital(),
		//isPhysical: it.isPhysical(),
		///isStateChange: it.isStateChange()
	]}
	*/
	
	def jsonOutput = new groovy.json.JsonOutput()
	def result = device.eventsBetween(start, end)?.collect{[
        "date": formatDate(it.date),
		"deviceID": it.deviceId,
		"name": it.name,
        "displayName": it.displayName,
		"description": it.description,
		"descriptionText": it.descriptionText,
		"unit": it.unit,
		"source": it.source,
		"value": it.value,
		"isDigital": it.isDigital(),
		"isPhysical": it.isPhysical(),
		"isStateChange": it.isStateChange()
	]}

	def myData = []
    //log_trace "Events of Device: ${result}"
    //result.each { log_info "EVENT: ${it}" }
	result.each {
		myData = it
		//log_trace "Sending: ${myData}"
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
		dimmer          : dimmers               ?.collect{getEventsOfDevice(it,start,end)},
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

	/*
	def filteredEvents = [:]
	
	eventsPerCapability.each {deviceType, events ->
		filteredEvents[deviceType] = filterEventsPerCapability(events?.flatten(), deviceType)
        //log_trace "Event: ${filterEventsPerCapability(events?.flatten(), deviceType)}"
	}

	def result = filteredEvents.values()?.flatten()?.findAll{it}?.sort{"$it.date.time" + "$it.deviceType"}.reverse()
	*/
	
	ago = ago + 1;
	// TODO: should be 7
	if ( ago < 1 ) {
		log_trace "Calling getAll: ago = ${ago}."
		runIn(2, "getAllDeviceEvents", [data: [ago: ago, todayUnix: todayUnix]])
	} else {
		log_trace "No more calls: ago = ${ago}."
	}
	
	result 
}





mappings {
	path("/history") {action: [GET: "history"]}
	path("/revokeAccessToken") {action: [GET: "doRevokeAccessToken"]}
	path("/createAccessToken") {action: [GET: "doCreateAccessToken"]}
}

def indexPage() {["ok"]}


def headHistory() {
"""
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=0"/>
<title>$app.label Event History</title>
<link rel="stylesheet" href="https://code.jquery.com/mobile/1.4.4/jquery.mobile-1.4.4.min.css" />
<link href="https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css" rel="stylesheet">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/weather-icons/1.3.2/css/weather-icons.min.css" />

<link href='https://fonts.googleapis.com/css?family=Mallanna' rel='stylesheet' type='text/css'>
<script src="https://code.jquery.com/jquery-2.1.1.min.js" type="text/javascript"></script>
<script src="https://code.jquery.com/mobile/1.4.4/jquery.mobile-1.4.4.min.js" type="text/javascript"></script>

<style>

</style>
"""
}


def history() {
	log_debug "History..."

	// render contentType: "text/html", data: """<!DOCTYPE html><html><head>${headHistory()} \n<style>""</style></head><body>${historyNav()}<ul class="history-list list">\n${getAllDeviceEvents()?.collect{renderEvent(it)}.join("\n")}</ul></body></html>"""
	//log.debug "${getAllDeviceEvents()?.collect{renderEvent(it)}.join("\n")}"
	
	/* TODO: Could be an input parameter. Assume starting immediately today and going back 7 days. */
    def ago = 0
	def todayUnix = new Date().getTime();
	state.exportId = getExportId();
	
	log_trace "Formatted date is ${state.exportId}"
	/* Sample format: def data = ["time" : timestamp, "server": server, id : ["instanceID" : ago ] ] */
	
	/* Sample Event record under a timeStamp.
	def timeStamp = new Date().getTime();
	def data = ["${timeStamp}" : ["date":"2017-11-05T17:28:34+0000",
		"deviceID":"eedb429d-69c1-441f-a69f-a8865df889fa","name":"lock","displayName":"Lock Back Door"]
	]
	sendToFB(data)
	return
	*/
	
    log_debug "calling getAll"
    runIn(2, "getAllDeviceEvents", [data: [ago: ago, todayUnix: todayUnix]])
}




def getDeviceData(device, type) {[tile: "device",  active: isActive(device, type), type: type, device: device.id, name: device.displayName, value: getDeviceValue(device, type), level: getDeviceLevel(device, type), isValue: isValue(device, type)]}

def getDeviceFieldMap() {[lock: "lock", "switch": "switch", dimmer: "switch", contact: "contact", presence: "presence", temperature: "temperature", humidity: "humidity", luminosity: "illuminance", motion: "motion", acceleration: "acceleration", water: "water", power: "power", energy: "energy", battery: "battery"]}

//def getActiveDeviceMap() {[lock: "unlocked", themeLight: "on", light: "on", "switch": "on", dimmer: "on", dimmerLight: "on", contact: "open", presence: "present", motion: "active", acceleration: "active", water: "wet"]}

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
	//def tf = new java.text.SimpleDateFormat("h:mm:ss a, dd MMMMM")
    //if (location?.timeZone) tf.setTimeZone(location.timeZone)
	def tf = new java.text.SimpleDateFormat("YYYY-MM-dd hh:mm:ss")
    return tf.format(date)
}

def getDOW() {
	def tf = new java.text.SimpleDateFormat("EEEE")
    if (location?.timeZone) tf.setTimeZone(location.timeZone)
    "${tf.format(new Date())}"
}


def roundNumber(num) {
	try {
	if (num == 0 || num == 0.0) return 0;
	if (!num) return "n/a "
	if (!defaultPrefs("roundNumbers") || !"$num".isNumber()) return num
	else {
    	try {
            return "$num".toDouble().round()
        } catch (e) {return num}
    } 
	} catch (e) {
		handleError(e, "roundNumber $num");
	}
}


/*
def getEventIcon(event) {
	if (event.name == "level" && (event.deviceType == "dimmerLight" || event.deviceType == "dimmer")) return (getTileIcons()["light"]).on
	def eventValues = getTileIcons()[event.deviceType]

	if (!eventValues) return getTileIcons()["?"]
	
	if (eventValues instanceof String) return eventValues
	
	eventValues[event.value] ?: getTileIcons()["?"]
}
*/




def allDeviceData() {
	try {
	log_trace "allDeviceData start"
	def refresh = [tile: "refresh", ts: getTS(), name: "Refresh", type: "refresh"]
	if (defaultPrefs("disableDashboard")) return [refresh]
	
	def data = []
	
	data.addAll(state.tileSeparators ?: [])
	
	if (defaultPrefs("showSHM")) {
		def status = location.currentState("alarmSystemStatus")?.value
		def message = [off : "Disarmed", away: "Armed (away)", stay: "Armed (stay)"][status] ?: status
		data << [tile: "alarm", type: "alarm", active: status == "away" || status == "stay" ? "active" : "inactive", name: "Smart Home Monitor", status: status, message: message]
	}
	
	log_trace "allDeviceData collected SHM"
	
	if (defaultPrefs("showClock") == "Small Analog") data << [tile: "clock", size: 1, style: "a", date: getDate(), dow: getDOW(), name: "Clock", type: "clock"]
	else if (defaultPrefs("showClock") == "Large Analog") data << [tile: "clock", size: 2, style: "a", date: getDate(), dow: getDOW(), name: "Clock", type: "clock"]
    	else if (defaultPrefs("showClock") == "Small Digital") data << [tile: "clock", size: 1, style: "d", date: getDate(), dow: getDOW(), name: "Clock", type: "clock"]
	else if (defaultPrefs("showClock") == "Large Digital") data << [tile: "clock", size: 2, style: "d", date: getDate(), dow: getDOW(), name: "Clock", type: "clock"]
	
	if (defaultPrefs("showMode") && location.modes) data << [tile: "mode", mode: "$location.mode", isStandardMode: ("$location.mode" == "Home" || "$location.mode" == "Away" || "$location.mode" == "Night"), modes: location?.modes?.name?.sort(), name: "Mode", type: "mode"]
	
	log_trace "allDeviceData collected modes"

	if (defaultPrefs("showHelloHome")) {
		def phrases = []
		try {
			phrases = location?.helloHome?.getPhrases() ? location?.helloHome?.getPhrases()*.label?.sort() : []
		} catch (e) {
			log_error "Unable to fetch Routines: Invalid result from getPhrases(). Please contact SmartThings Support support@SmartThings.com and ask them to check if your Location/Routines data is maybe corrupted or missing."
		}
		if (phrases) data << [tile: "helloHome", phrases: phrases, name: "Routines", type: "hello-home"]
	}
	
	log_trace "allDeviceData collected routines"
	
	weather?.each{data << getWeatherData(it)}
	
	log_trace "allDeviceData collected weather"
	
	locks?.each{data << getDeviceData(it, "lock")}
	log_trace "allDeviceData collected locks"
	thermostatsHeat?.each{data << getThermostatData(it, "thermostatHeat")}
	thermostatsCool?.each{data << getThermostatData(it, "thermostatCool")}
	log_trace "allDeviceData collected thermostats"
	music?.each{data << getMusicPlayerData(it)}
	log_trace "allDeviceData collected MusicPlayerData"
	switches?.each{data << getDeviceData(it, "switch")}
	log_trace "allDeviceData collected switches"
	dimmers?.each{data << getDeviceData(it, "dimmer")}
	log_trace "allDeviceData collected dimmers"
	dimmerLights?.each{data << getDeviceData(it, "dimmerLight")}
	log_trace "allDeviceData collected dimmerLights"
	momentaries?.each{data << getDeviceData(it, "momentary")}
	log_trace "allDeviceData collected momentaries"
	contacts?.each{data << getDeviceData(it, "contact")}
	log_trace "allDeviceData collected contacts"
	presence?.each{data << getDeviceData(it, "presence")}
	log_trace "allDeviceData collected presence"
	motion?.each{data << getDeviceData(it, "motion")}
	log_trace "allDeviceData collected motion"
	acceleration?.each{data << getDeviceData(it, "acceleration")}
	log_trace "allDeviceData collected acceleration"
	camera?.each{data << getDeviceData(it, "camera")}
	log_trace "allDeviceData collected camera"
	
	(1..10).each{if (settings["dropcamStreamUrl$it"]?.toLowerCase()?.startsWith("http")) {data << [tile: "video", device: "$it", link: settings["dropcamStreamUrl$it"], name: settings["dropcamStreamT$it"] ?: "", i: it, type: "dropcam"]}}
	(1..10).each{if (settings["mjpegStreamUrl$it"]?.toLowerCase()?.startsWith("http")) {data << [tile: "video", device: "$it", link: settings["mjpegStreamUrl$it"], name: settings["mjpegStreamTitile$it"] ?: "", i: it, type: "mjpeg"]}}
	(1..10).each{if (settings["smvStreamUrl$it"]?.toLowerCase()?.startsWith("http")) {data << [tile: "video", device: "$it", link: settings["smvStreamUrl$it"], name: settings["smvStreamTitile$it"] ?: "", i: it, type: "smv"]}}
	
	log_trace "allDeviceData collected video streams"
	
	temperature?.each{data << getDeviceData(it, "temperature")}
	log_trace "allDeviceData collected temperature"
	humidity?.each{data << getDeviceData(it, "humidity")}
	log_trace "allDeviceData collected humidity"
	luminosity?.each{data << getDeviceData(it, "luminosity")}
	log_trace "allDeviceData collected luminosity"
	water?.each{data << getDeviceData(it, "water")}
	log_trace "allDeviceData collected water"
	energy?.each{data << getDeviceData(it, "energy")}
	log_trace "allDeviceData collected energy"
	power?.each{data << getDeviceData(it, "power")}
	log_trace "allDeviceData collected power"
	battery?.each{data << getDeviceData(it, "battery")}
	log_trace "allDeviceData collected battery"
	
	(1..10).each{if (settings["linkUrl$it"]) {data << [tile: "link", device: "$it", link: settings["linkUrl$it"], name: settings["linkTitle$it"] ?: "Link $it", i: it, type: "link"]}}
	(1..10).each{if (settings["dashboardUrl$it"]) {data << [tile: "dashboard", device: "$it", link: settings["dashboardUrl$it"], name: settings["dashboardTitle$it"] ?: "Dashboard $it", i: it, type: "dashboard"]}}
	log_trace "allDeviceData collected links"
	
	if (defaultPrefs("showRefresh")) data << refresh
	if (defaultPrefs("showHistory")) data << [tile: "history", name: "Event History", type: "history"]
	
	log_trace "allDeviceData end"
	state.tileCount = data.size()
	
	def toolsTile = [tile: "tools", name: "Tools", type: "tools"]
	
	log_trace "tile count: $tileCount"
	log_trace "Sort Order: ${state?.sortOrder}"

	/* If toolsTile not in sortOrder, add it to the end of data. */
	if (!state?.sortOrder?.containsKey("tools-null")) {
		data.sort{state?.sortOrder?."$it.type-$it.device"}
		data << toolsTile
	} else {
		data << toolsTile
		data.sort{state?.sortOrder?."$it.type-$it.device"}
	}
	
	} catch (e) {
		handleError(e, "allDeviceData")
	}
}


def html() {
	try {
		checkin()
		
		render contentType: "text/html", data: "<!DOCTYPE html><html><head>${head()} \n<style>""</style></head><body>\n${footer()}</body></html>"
	} catch (e) {
		return handleError(e, "html")
	}
}



def generateURLWithTokenOrRedirect(path) {
	log_trace "running generateURLWithTokenOrRedirect( " + path + " )"
	if (params.access_token) {
		return apiServerUrl("api/smartapps/installations/$app.id/$path") + "?access_token=$params.access_token"
	} else {
		return smartAppUrlWithWebLogin(path)
	}
}


/*
def getListIcon(type) {
	def icons = [
		lock: getTileIcons().lock.locked,
		switch: getTileIcons().switch.on,
		light: getTileIcons().light.on,
		dimmer: getTileIcons().dimmer.on,
		momentary: getTileIcons().momentary,
		contact: getTileIcons().contact.open,
		presence: getTileIcons().presence.present,
		motion: getTileIcons().motion.active,
		acceleration: getTileIcons().acceleration.active,
		water: getTileIcons().water.wet,
		alarm: getTileIcons().alarm.armed,
		tileSeparator: "&nbsp;",
	]
	
	icons[type] ?: getTileIcons()[type]
}
*/


def list() {render contentType: "text/html", data: """
	<!DOCTYPE html>
	<html>
	<head>
		<meta charset="UTF-8" />
		<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=0"/>
		<title>$app.label Device Order</title>

		<link rel="stylesheet" href="https://code.jquery.com/mobile/1.4.4/jquery.mobile-1.4.4.min.css" />
		<link href="https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css" rel="stylesheet">
		<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/weather-icons/1.3.2/css/weather-icons.min.css" />
		<link href="${assetPath()}/style.${appVersion()}.min.css?u=0" rel="stylesheet">
		<link href='https://fonts.googleapis.com/css?family=Mallanna' rel='stylesheet' type='text/css'>

		<script src="https://code.jquery.com/jquery-2.1.1.min.js" type="text/javascript"></script>
		
		<script src="https://code.jquery.com/ui/1.11.2/jquery-ui.min.js" type="text/javascript"></script>
		<script src="${assetPath()}/jquery.ui.touch-punch.min.js" type="text/javascript"></script>

		<script>
			\$(function() {
				\$( ".list" ).sortable({
					stop: function( event, ui ) {changeOrder();},
                    axis: "y"
				});
				\$( ".list" ).disableSelection();
				
				\$( ".st-blank-tile" ).click(function(e){
					var element = \$(this).parent();
					e.stopImmediatePropagation();
					e.preventDefault();
					var id = \$(this).parent().data("device");
					var request = {id: id};
					var access_token = getUrlParameter("access_token");
					if (access_token) request["access_token"] = access_token;
					\$.get("deleteBlank", request).done(function(data){
						element.remove();
					}).fail(function() {alert("error, please refresh")});
				});
			});

			function changeOrder() {
				var l = "";
				\$( ".list li" ).each(function(index) {
					l = l + \$(this).data("type") + "-" + \$(this).data("device") + "|~|";
				});
				var access_token = getUrlParameter("access_token");
				var request = {list: l};
				if (access_token) request["access_token"] = access_token;
				
				\$.get("position", request).done(function(data) {
					if (data.status == "ok") {}
				}).fail(function() {alert("error, please refresh")});
			}
			
			function getUrlParameter(sParam)
			{
				var sPageURL = window.location.search.substring(1);
				var sURLVariables = sPageURL.split('&');
				for (var i = 0; i < sURLVariables.length; i++) 
				{
					var sParameterName = sURLVariables[i].split('=');
					if (sParameterName[0] == sParam) 
					{
						return sParameterName[1];
					}
				}
			}
			
			function addBlankTile() {
				var request = {};
				var access_token = getUrlParameter("access_token");
				if (access_token) request["access_token"] = access_token;
				\$.get("addBlankTile", request).done(function(data){
					\$("#tileList").append("<li class='item tile tileSeparator' data-type='tileSeparator' data-device='" + data.i + "' id='tileSeparator|" + data.i + "'>Blank Tile <i class='active fa fa-fw fa-close st-blank-tile' style='float: right; cursor: pointer; padding: 4px 5px 10px 30px;'></i></li>");
					changeOrder();
					
					\$('html, body').animate({
					   scrollTop: \$('.footer').offset().top
					}, 'slow');

				}).fail(function() {alert("error, please refresh")});
			}
		</script>
		<style>
		${getThemeLightIcon().css}
		</style>
	</head>
	<body class='theme-${defaultPrefs("theme")}'>
		<div style="" class="historyNav">
			<i class="fa fa-fw fa-arrow-left" onclick="location.replace('${generateURLWithTokenOrRedirect("back")}')"></i>
			<i class="fa fa-fw fa-refresh" onclick="this.className = this.className + ' fa-spin'; location.reload();"></i>
			<i class="fa fa-fw fa-square" onclick="addBlankTile()"></i>
			<i class="fa fa-fw fa-question" onclick="window.open('${helpURL()}/order');"></i>
		</div>
		<ul class="list" style="margin-top:50px" id="tileList">\n${allDeviceData()?.collect{renderListItem(it)}.join("\n")}</ul>
		<div class="footer">&nbsp;</div>
	</body>
	</html>
"""
}


/* =========== */
/* End of File */
/* =========== */
