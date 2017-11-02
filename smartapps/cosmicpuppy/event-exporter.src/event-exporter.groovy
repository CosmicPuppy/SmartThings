def appVersion()  {"1.0.0"}     // Major.Minor.Hotfix (Official releases and pre-releases).
/**
 *  Event Exporter
 *
 *  Visit Home Page for more information:
 *
 *  Software is provided without warranty and the software author/license owner cannot be held liable for damages.
 *
 *  Copyright © 2017 Terry Gauchat
 */

//include 'asynchttp_v1'

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


preferences(oauthPage: "deviceAuthorization") {
	page(name: "deviceAuthorization", install: true, uninstall: true) {
		
		section( "SmartApp can only access the specific Things that you authorize here \n" +
				"\n[Version ${appVersion()}] \n"
			 ) {
			input "sensors", "capability.sensor", title:"1) Select any or all of your Things:", multiple: true, required: false
			input "actuators", "capability.actuator", title:"2) Add any or all Things missing from the previous list (duplicates OK):", multiple: true, required: false
			input "switches", "capability.switch", title:"3) Add any or all Things missing from the previous lists (duplicates OK):", multiple: true, required: false
			input "batteries", "capability.battery", title:"4) If desired Things are not available in any above lists, try this list:", multiple: true, required: false
			input "temperatures", "capability.temperatureMeasurement", title:"5) Or this final list:", multiple: true, required: false
		}
		
		if (state) {
			section( "ActionTiles Stream Status" ) {
				if (isLocationStreaming()) {
					paragraph "$location.name is currently streaming event data to ActionTiles."
				} else {
					paragraph "$location.name is currently not streaming any event data to ActionTiles."
				}
			}
		}
		remove("Uninstall", "Are you sure you want to uninstall ActionTiles?")
	}
}

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
	[status: "ok"]
}


def uninstalled() {
	log.info "Uninstalling ActionTiles"
	revokeAccessToken()
}	


mappings {
	path("/history") {action: [GET: "history"]}
	path("/revokeAccessToken") {action: [GET: "doRevokeAccessToken"]}
	path("/createAccessToken") {action: [GET: "doCreateAccessToken"]}
}

def indexPage() {["ok"]}


def head() {
"""
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=0"/>
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="apple-mobile-web-app-status-bar-style" content="black" />
<link rel="icon" sizes="192x192" href="${assetPath()}/icon.png">
<link rel="apple-touch-icon" href="${assetPath()}/icon.png">
<meta name="mobile-web-app-capable" content="yes">
<title>$app.label Dashboard</title>

<link rel="stylesheet" href="https://code.jquery.com/mobile/1.4.4/jquery.mobile-1.4.4.min.css" />
<link href="https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css" rel="stylesheet">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/weather-icons/1.3.2/css/weather-icons.min.css" />
<link href="${assetPath()}/style.${appVersion()}.min.css?u=0" rel="stylesheet">
<link href='https://fonts.googleapis.com/css?family=Mallanna' rel='stylesheet' type='text/css'>

<script>
window.location.hash = "";
var stateID = ${getStateID()};
var stateTS = ${getStateTS()};
var tileSize = ${getTSize()};
var readOnlyMode = ${defaultPrefs("readOnlyMode")};
var icons = ${getTileIcons().encodeAsJSON()};
var appVersion = "${appVersion()}-${appStream()}[${appInstance()}]";
var minTemp = ${getMinTemp()};
var maxTemp = ${getMaxTemp()};
var theme = "${defaultPrefs("theme")}";
var pollingRate = ${defaultPrefs("pollingRate") < 30 ? 30 : defaultPrefs("pollingRate")};
var appUrl = "${generateURLWithTokenOrRedirect("app")}";
var hasWeather = ${weather ? true : false};
var smvRate = ${defaultPrefs("smvRate")} * 1000;
var clockFormat = "${defaultPrefs("clockFormat")}";
var access_token = ${params.access_token ? "'$params.access_token'" : null};
</script>

<script src="https://code.jquery.com/jquery-2.1.1.min.js" type="text/javascript"></script>
<script src="https://code.jquery.com/mobile/1.4.4/jquery.mobile-1.4.4.min.js" type="text/javascript"></script>
<script src="${assetPath()}/script.${appVersion()}.min.js?u=0" type="text/javascript"></script>

<style>
.tile {width: ${getTSize()}px; height: ${getTSize()}px;}
.w2 {width: ${getTSize() * 2}px;}
.w3 {width: ${getTSize() * 3}px;}
.h2 {height: ${getTSize() * 2}px;}
.h3 {height: ${getTSize() * 3}px;}
${!defaultPrefs("dropShadow") ? ".icon, .icon * {text-shadow: none;} .ui-slider-handle.ui-btn.ui-shadow {box-shadow: none; -webkit-box-shadow: none; -moz-box-shadow: none;}" : ""}
body {font-size: ${getFSize()}%;}
${defaultPrefs("readOnlyMode") ? """.tile, .music i {cursor: default} .clock, .refresh{cursor: pointer}""" : ""}
${getThemeLightIcon().css}
</style>
"""
}                                                              

def footer() {
"""<script>
\$(function() {
  var wall = new freewall(".tiles");
  wall.fitWidth();
  
  wall.reset({
		draggable: false,
		animate: true,
		selector: '.tile',
		gutterX:cellGutter,
		gutterY:cellGutter,
		cellW:cellSize,
		cellH:cellSize,
		fixSize:null,
		onResize: function() {
			wall.fitWidth();
			wall.refresh();
		}
	});
	wall.fitWidth();
	// for scroll bar appear;
	\$(window).trigger("resize");
});
</script>"""
}

def headHistory() {
"""
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=0"/>
<title>$app.label Event History</title>
<link rel="stylesheet" href="https://code.jquery.com/mobile/1.4.4/jquery.mobile-1.4.4.min.css" />
<link href="https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css" rel="stylesheet">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/weather-icons/1.3.2/css/weather-icons.min.css" />
<link href="${assetPath()}/style.${appVersion()}.min.css?u=0" rel="stylesheet">
<link href='https://fonts.googleapis.com/css?family=Mallanna' rel='stylesheet' type='text/css'>
<script src="https://code.jquery.com/jquery-2.1.1.min.js" type="text/javascript"></script>
<script src="https://code.jquery.com/mobile/1.4.4/jquery.mobile-1.4.4.min.js" type="text/javascript"></script>
<script src="${assetPath()}/jquery.ui.touch-punch.min.js" type="text/javascript"></script>
<style>
${getThemeLightIcon().css}
</style>
"""
}

def getTSize() {
	if (defaultPrefs("tileSize") == "Medium") return 120
	else if (defaultPrefs("tileSize") == "Large") return 150
	105
}

def getFSize() {
	if (defaultPrefs("fontSize") == "Smaller") return 90
	if (defaultPrefs("fontSize") == "Larger") return 120
	if (defaultPrefs("fontSize") == "Largest") return 150
	100
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
	def tf = new java.text.SimpleDateFormat("h:mm:ss a, dd MMMMM")
    if (location?.timeZone) tf.setTimeZone(location.timeZone)
    return tf.format(date)
}

def getDOW() {
	def tf = new java.text.SimpleDateFormat("EEEE")
    if (location?.timeZone) tf.setTimeZone(location.timeZone)
    "${tf.format(new Date())}"
}

def renderModeTile(data) {
"""<div class="mode tile w2 menu ${data.isStandardMode ? data.mode : ""}" data-mode="$data.mode" data-popup="mode-popup">
	<div class="title">Mode</div>
	<div data-role="popup" id="mode-popup" data-overlay-theme="b">
		<ul data-role="listview" data-inset="true" style="min-width:210px;">
			${data.modes.collect{"""<li data-icon="false">$it</li>"""}.join("\n")}
		</ul>
    </div>
	<div class="icon Home"><i class="fa fa-home"></i></div>
	<div class="icon Night"><i class="fa fa-moon-o"></i></div>
	<div class="icon Away"><i class="fa fa-sign-out"></i></div>
	<div class="icon small text mode-name" id="mode-name">$data.mode</div>
</div>"""
}

def renderHelloHomeTile(data) {
"""
<div class="hello-home tile menu" data-rel="popup" data-popup="hello-home-popup">
	<div class="title">Routines</div>
	<div data-role="popup" id="hello-home-popup" data-overlay-theme="b">
		<ul data-role="listview" data-inset="true" style="min-width:210px;">
			${data.phrases.collect{"""<li data-icon="false">$it</li>"""}.join("\n")}
		</ul>
	</div>
</div>
"""
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

def getWeatherData(device) {
	try {
	def data = [tile:"device", active:"inactive", type: "weather", device: device.id, name: device.displayName]
    ["city", "weather", "feelsLike", "temperature", "localSunrise", "localSunset", "percentPrecip", "humidity", "weatherIcon"].each{data["$it"] = device?.currentValue("$it")}
    data.icon = ["chanceflurries":"wi-snow","chancerain":"wi-rain","chancesleet":"wi-rain-mix","chancesnow":"wi-snow","chancetstorms":"wi-storm-showers","clear":"wi-day-sunny","cloudy":"wi-cloudy","flurries":"wi-snow","fog":"wi-fog","hazy":"wi-dust","mostlycloudy":"wi-cloudy","mostlysunny":"wi-day-sunny","partlycloudy":"wi-day-cloudy","partlysunny":"wi-day-cloudy","rain":"wi-rain","sleet":"wi-rain-mix","snow":"wi-snow","sunny":"wi-day-sunny","tstorms":"wi-storm-showers","nt_chanceflurries":"wi-snow","nt_chancerain":"wi-rain","nt_chancesleet":"wi-rain-mix","nt_chancesnow":"wi-snow","nt_chancetstorms":"wi-storm-showers","nt_clear":"wi-stars","nt_cloudy":"wi-cloudy","nt_flurries":"wi-snow","nt_fog":"wi-fog","nt_hazy":"wi-dust","nt_mostlycloudy":"wi-night-cloudy","nt_mostlysunny":"wi-night-cloudy","nt_partlycloudy":"wi-night-cloudy","nt_partlysunny":"wi-night-cloudy","nt_sleet":"wi-rain-mix","nt_rain":"wi-rain","nt_snow":"wi-snow","nt_sunny":"wi-night-clear","nt_tstorms":"wi-storm-showers","wi-horizon":"wi-horizon"][data.weatherIcon]
	data
	} catch (e) {
		handleError(e, "getWeatherData $device");
	}
}

def getThermostatData(device, type) {
	try {
	def deviceData = [:]
	device?.supportedAttributes?.each{
		try {
			deviceData << [("$it" as String): device.currentValue("$it")]
		} catch (e) {}
	}
	def setpoint = roundThermostatSetpoint(type == "thermostatHeat" ? deviceData.heatingSetpoint : deviceData.coolingSetpoint)
	[tile: "device", type: type, device: device.id, name: device.displayName, humidity: deviceData.humidity, temperature: deviceData.temperature, thermostatFanMode: deviceData.thermostatFanMode, thermostatOperatingState: deviceData.thermostatOperatingState, setpoint: setpoint.whole, fraction: setpoint.fraction]
	} catch (e) {
		handleError(e, "getThermostatData $device, $type");
	}
}

def roundThermostatSetpoint(setpoint) {
	try {
	def whole = setpoint
	def fraction = 0
	if (setpoint) {
		if (getTemperatureScale() == "F") {
			whole = Math.round(setpoint)
		} else {
			setpoint = Math.round(setpoint * 2.0) / 2.0
			whole = setpoint as int
			fraction = (setpoint - whole) as int
			
			whole = Math.round(setpoint) // remove this line when ready
		}
	}
	
	[whole: whole, fraction: fraction]
	} catch (e) {
		handleError(e, "roundThermostatSetpoint $setpoint");
	}
}

def renderTile(data) {
	try {
	log_trace "rendering tile $data"
	if (data.type == "thermostatHeat" || data.type == "thermostatCool") {
		return  """<div class="$data.type tile h2 thermostat ${data.setpoint ? "" : "null-setpoint"}" data-type="$data.type" data-scale="${getTemperatureScale()}" data-device="$data.device" data-setpoint="$data.setpoint" data-operating-state="$data.thermostatOperatingState" data-fraction="$data.fraction"><div class="title">$data.name ${getTileIcons()[data.type]}<br/><span class="title2">${data.temperature}&deg;, $data.thermostatOperatingState</span></div><div class="icon setpoint"><span class="whole">$data.setpoint</span><span class="fraction">.$data.fraction</span><span class="degree">&deg;</span></div><div class="icon null-setpoint">--</div><div class="icon up"><i class="fa fa-fw fa-chevron-up"></i></div><div class="icon down"><i class="fa fa-fw fa-chevron-down"></i></div><div class="footer">&#10044; $data.thermostatFanMode ${data.humidity ? ",<i class='fa fa-fw wi wi-sprinkles'></i>" + data.humidity  + "%" : ""}</div></div>"""
	} else if (data.type == "weather"){
		return """<div class="weather tile w2" data-type="weather" data-device="$data.device" data-weather="$data.weatherIcon" data-fixSize="0"><div class="title">$data.city<br/><span class="title2">$data.weather, feels like $data.feelsLike&deg;</span></div><div class="icon"><span class="text">$data.temperature&deg;</span><i class="wi $data.icon"></i></span></div><div class="footer">$data.localSunrise <i class="fa fa-fw wi wi-horizon-alt"></i> $data.localSunset</div><div class="footer right">$data.percentPrecip%<i class="fa fa-fw fa-umbrella"></i><br>$data.humidity%<i class="fa fa-fw wi wi-sprinkles"></i></div></div>"""
	} else if (data.type == "music") {
		return """
		<div class="music tile w2 $data.active ${data.mute ? "muted" : ""}" data-type="music" data-device="$data.device" data-level="$data.level" data-track-description="$data.trackDescription" data-mute="$data.mute">
			<div class="title"><span class="name">$data.name</span><br/><span class='title2 track'>$data.trackDescription</span></div>
			<div class="icon text"><i class="fa fa-fw fa-backward back"></i>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<i class="fa fa-fw fa-pause pause"></i><i class="fa fa-fw fa-play play"></i>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<i class="fa fa-fw fa-forward forward"></i></div>
			<div class="footer"><i class='fa fa-fw fa-volume-down unmuted'></i><i class='fa fa-fw fa-volume-off muted'></i></div>
		</div>
		"""
	} else if (data.tile == "video") {
		if (data.type == "dropcam") {
			def ts = defaultPrefs("videoTileSize") == "Small" ? "h2 w2" : "h2 w3"
			return """<div class="video dropcam tile $ts" data-link-i="$data.i" data-fixSize="0"><div class="title">$data.name</div><div class="video-container"><object width="240" height="164"><param name="movie" value="$data.link"></param><param name="allowFullScreen" value="true"></param><param name="allowscriptaccess" value="always"></param><param name="wmode" value="opaque"></param><embed src="$data.link" type="application/x-shockwave-flash" allowscriptaccess="always" allowfullscreen="true" width="240" height="164" wmode="opaque"></embed></object></div></div>"""
		} else if (data.type == "mjpeg") {
			def ts = defaultPrefs("videoTileSize") == "Small" ? "h1 w2" : "h2 w3"
			return """<div class="video mjpeg tile $ts" data-link-i="$data.i" data-fixSize="0"><div class="title">$data.name</div><div class="video-container"><img src="$data.link"/></div></div>"""
		} else if (data.type == "smv") {
			def ts = defaultPrefs("videoTileSize") == "Small" ? "h1 w2" : "h2 w3"
			return """<div class="video smv tile $ts" data-link-i="$data.i" data-fixSize="0"><div class="title">$data.name</div><div class="video-container"><img src="$data.link" data-src="$data.link"/></div></div>"""
		}
	} else if (data.tile == "device") {
		return """<div class="$data.type tile $data.active" data-active="$data.active" data-type="$data.type" data-device="$data.device" data-value="$data.value" data-level="$data.level" data-is-value="$data.isValue"><div class="title">$data.name</div></div>"""
	} else if (data.tile == "link") {
		return """<div class="link tile" data-link-i="$data.i"><div class="title">$data.name</div><div class="icon"><a href="$data.link" data-ajax="false"><i class="fa fa-th"></i></a></div></div>"""
	} else if (data.tile == "dashboard") {
		return """<div class="dashboard tile" data-link-i="$data.i"><div class="title">$data.name</div><div class="icon"><a href="$data.link" data-ajax="false"><i class="fa fa-link"></i></a></div></div>"""
	} else if (data.tile == "refresh") {
		return """<div class="refresh tile clickable"><div class="title">Refresh</div><div class="footer">Updated $data.ts</div></div>"""
	} else if (data.tile == "history") {
		return """<div class="history tile"><div class="title">Event History</div></div>"""
	} else if (data.tile == "mode") {
		return renderModeTile(data)
	} else if (data.tile == "clock") {
		if (data.style == "a") {
			return """<div id="analog-clock" class="clock tile clickable h$data.size w$data.size"><div class="title">$data.date</div><div class="icon" style="margin-top:-${data.size * 45}px;"><canvas id="clockid" class="CoolClock:st:${45 * data.size}"></canvas></div><div class="footer">$data.dow</div></div>"""
		} else {
			return """<div id="digital-clock" class="clock tile clickable w$data.size"><div class="title">$data.date</div><div class="icon ${data.size == 2 ? "" : "text"}" id="clock">*</div><div class="footer">$data.dow</div></div>"""
		}
	} else if (data.tile == "helloHome") {
		return renderHelloHomeTile(data)
	} else if (data.tile == "alarm") {
		return """<div class="alarm tile $data.active h1 w2" data-active="$data.active" data-type="alarm" data-device="alarm" data-status="$data.status" data-popup="alarm-popup" data-pin-to-arm="$settings.shmPinToArm" data-pin-to-disarm="$settings.shmPinToDisarm">
				<div class="title">Smart Home Monitor</div>
				<div class="footer">$data.message</div>
				<div data-role="popup" id="alarm-popup" data-overlay-theme="b">
					<ul data-role="listview" data-inset="true" style="min-width:210px;">
						<li data-icon="false" data-state="away">Arm (Away)</li>
						<li data-icon="false" data-state="stay">Arm (Stay)</li>
						<li data-icon="false" data-state="off">Disarm</li>
					</ul>
				</div>
				</div>"""
	} else if (data.type == "tileSeparator") {
		return """<div class="$data.type tile" data-fix-Size="null">&nbsp;</div>"""
	} else if (data.type == "tools") {
		return """
			<div class="tools tile" data-type="tools" data-device="tools">
				<div class="title">Tools</div>
				<div class="icon"><i class="fa fa-fw fa-ellipsis-h st-tools"></i></div>
			</div>
			"""
	}
	
	return ""
	}  catch (e) {
		handleError(e, "renderTile $data");
	}
}

def getTileIcons() {
	[
		dimmer : [off : "<i class='inactive fa fa-fw fa-toggle-off st-switch-off'></i>", on : "<i class='active fa fa-fw fa-toggle-on st-switch-on'></i>"],
		dimmerLight : [off : "<i class='inactive fa fa-fw fa-lightbulb-o st-light-off'></i>", on : "<i class='active fa fa-fw fa-lightbulb-o st-light-on'></i>"],
		switch : [off : "<i class='inactive fa fa-fw fa-toggle-off st-switch-off'></i>", on : "<i class='active fa fa-fw fa-fw fa-toggle-on st-switch-on'></i>"],
		light : [off : "<i class='inactive fa fa-fw fa-lightbulb-o st-light'></i>", on : "<i class='active fa fa-fw fa-lightbulb-o st-light-on'></i>"],
		lock : [locked : "<i class='inactive fa fa-fw fa-lock st-lock'></i>", unlocked : "<i class='active fa fa-fw fa-unlock-alt st-unlock'></i>"],
		motion : [active : "<i class='active fa fa-fw fa-exchange st-motion-active'></i>", inactive: "<i class='inactive fa fa-fw fa-exchange st-motion-inactive'></i>"],
		acceleration : [active : "<i class='active fa fa-fw st-acceleration-active'>&#8779</i>", inactive: "<i class='inactive fa fa-fw st-acceleration-inactive'>&#8779</i>"],
		presence : [present : "<i class='active fa fa-fw fa-map-marker st-present'></i>", notPresent: "<i class='inactive fa fa-fw fa-map-marker st-not-present'></i>", "not present": "<i class='inactive fa fa-fw fa-map-marker st-not-present'></i>"],
		contact : [open : "<i class='active r45 fa fa-fw fa-expand st-opened'></i>", closed: "<i class='inactive r45 fa fa-fw fa-compress st-closed'></i>"],
		water : [dry : "<i class='inactive fa fa-fw fa-tint st-dry'></i>", wet: "<i class='active fa fa-fw fa-tint st-wet'></i>"],
		alarm : [armed : "<i class='active fa fa-fw fa-shield st-alarm'></i>", disarmed: "<i class='inactive fa fa-fw fa-shield st-alarm'></i>"],
		momentary : "<i class='fa fa-fw fa-circle-o st-momentary'></i>",
		camera : "<i class='fa fa-fw fa-camera st-camera'></i>",
		refresh : "<i class='fa fa-fw fa-refresh st-refresh'></i>",
        history : "<i class='fa fa-fw fa-history st-history'></i>",		
		humidity : "<i class='fa fa-fw wi wi-sprinkles st-humidity'></i>",
		luminosity : "<i class='fa fa-fw st-luminosity'>&#9728;</i>",
		temperature : "<i class='fa fa-fw wi wi-thermometer st-temperature'></i>",
		energy : "<i class='fa fa-fw wi wi-lightning st-energy'></i>",
		power : "<i class='fa fa-fw fa-bolt st-power'></i>",
		battery : """<i class='fa fa-fw fa-fw batt st-battery'><?xml version="1.0" ?><!DOCTYPE svg  PUBLIC '-//W3C//DTD SVG 1.1//EN'  'http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd'><svg enable-background="new 0 0 96 96" height="96px" id="battery" version="1.1" viewBox="0 0 96 96" width="96px" xml:space="preserve" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><path d="M84,32c0-6.63-5.37-12-12-12H12C5.37,20,0,25.37,0,32v32c0,6.63,5.37,12,12,12h60c6.63,0,12-5.37,12-12  c6.63,0,12-5.37,12-12v-8C96,37.37,90.63,32,84,32z M76,64c0,2.21-1.79,4-4,4H12c-2.21,0-4-1.79-4-4V32c0-2.21,1.79-4,4-4h60  c2.21,0,4,1.79,4,4V64z M88,52c0,2.21-1.79,4-4,4V40c2.21,0,4,1.79,4,4V52z"/></svg></i>""",
        "hello-home" : "<i class='fa fa-fw fa-comment-o st-hello-home'></i>",
        link : "<i class='fa fa-fw fa-link st-link'></i>",
        dashboard : "<i class='fa fa-fw fa-th st-dashboard'></i>",
        thermostatHeat : "<i class='fa fa-fw fa-fire st-heat'></i>",
        thermostatCool : "<i class='fa fa-fw wi wi-snowflake-cold st-cool'></i>",
		themeLight: getThemeLightIcon(),
		clock: """<i class="fa fa-fw fa-clock-o st-clock"></i>""",
		mode: """<i class="fa fa-fw fa-gear st-mode"></i>""",
		weather: """<i class="fa fa-fw wi wi-day-rain-mix st-weather"></i>""",
		music: """<i class="fa fa-fw fa-music st-music"></i>""",
		video: """<i class="fa fa-fw fa-video-camera st-video"></i>""",
		smv: """<i class="fa fa-fw fa-video-camera st-video"></i>""",
		dropcam: """<i class="fa fa-fw fa-video-camera st-video"></i>""",
		mjpeg: """<i class="fa fa-fw fa-video-camera st-video"></i>""",
		"?": """<i class="fa fa-fw fa-question st-unknown"></i>""",
		"tools" : """<i class="fa fa-fw st-tools fa-ellipsis-h"></i>"""
	]
}

def getListIcon(type) {
	def icons = [
		lock: getTileIcons().lock.locked,
		switch: getTileIcons().switch.on,
		light: getTileIcons().light.on,
		themeLight: getTileIcons().themeLight.on,
		dimmer: getTileIcons().dimmer.on,
		dimmerLight: getTileIcons().dimmerLight.on,
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

def getEventIcon(event) {
	if (event.name == "level" && (event.deviceType == "dimmerLight" || event.deviceType == "dimmer")) return (getTileIcons()["light"]).on
	def eventValues = getTileIcons()[event.deviceType]

	if (!eventValues) return getTileIcons()["?"]
	
	if (eventValues instanceof String) return eventValues
	
	eventValues[event.value] ?: getTileIcons()["?"]
}

def getThemeLightIcon() {
	def icons = [
	"Valentine's Day" : [on : "<i class='active fa fa-fw fa-heart st-valentines-on'></i>", off : "<i class='inactive fa fa-fw fa-heart-o st-valentines-off'></i>", css: ".themeLight {background-color: #FF82B2;} /*pink*/ .themeLight.active {background-color: #A90000} .themeLight.active .icon i {color:#EA001F}"],
	"Christmas" : [on: "<i class='active fa fa-fw fa-tree st-christmas-on'></i>", off: "<i class='inactive fa fa-fw fa-tree st-christmas-off'></i>", css: ".themeLight {background-color: #11772D;} /*green*/ .themeLight.active {background-color: #AB0F0B} .themeLight.active .icon i {color:#11772D}"],
    ]
	icons[themeLightType] ?: [off : "<i class='inactive fa fa-fw fa-lightbulb-o st-light-off'></i>", on : "<i class='active fa fa-fw fa-lightbulb-o st-light-on'></i>", css : ""]
}

/* Terry: Cursor type "grab/grabbing" not supported on various browsers, so override CSS here to "pointer". TODO: Write smarter CSS? */
def renderListItem(data) {
	if (data.type == "tileSeparator") {
		return """<li class="item tile $data.type" data-type="$data.type" data-device="$data.device" id="$data.type|$data.device">Blank Tile<i class='active fa fa-fw fa-close st-blank-tile' style='float: right; cursor: pointer; padding: 4px 5px 10px 30px;'></i></li>"""
	} else {
		return """<li class="item tile $data.type" data-type="$data.type" data-device="$data.device" id="$data.type|$data.device" style='cursor: pointer;'>${getListIcon(data.type)}$data.name</li>"""
	}
}

def renderEvent(data) {return """<li class="item tile $data.deviceType" data-name="$data.name" data-value="$data.value"><div class="event-icon">${getEventIcon(data)}</div><div class="event">$data.displayName &nbsp;<i class="fa fa-long-arrow-right"></i> $data.value${data.unit ?: ""}</div><div class="date">${formatDate(data.date)}</div></li>"""}

def getMusicPlayerData(device) {[tile: "device", type: "music", device: device.id, name: device.displayName, status: device.currentValue("status"), level: getDeviceLevel(device, "music"), trackDescription: device.currentValue("trackDescription"), mute: device.currentValue("mute") == "muted", active: device.currentValue("status") == "playing" ? "active" : ""]}

def getDeviceData(device, type) {[tile: "device",  active: isActive(device, type), type: type, device: device.id, name: device.displayName, value: getDeviceValue(device, type), level: getDeviceLevel(device, type), isValue: isValue(device, type)]}

def getDeviceFieldMap() {[lock: "lock", themeLight: "switch", light: "switch", "switch": "switch", dimmer: "switch", dimmerLight: "switch", contact: "contact", presence: "presence", temperature: "temperature", humidity: "humidity", luminosity: "illuminance", motion: "motion", acceleration: "acceleration", water: "water", power: "power", energy: "energy", battery: "battery"]}

def getActiveDeviceMap() {[lock: "unlocked", themeLight: "on", light: "on", "switch": "on", dimmer: "on", dimmerLight: "on", contact: "open", presence: "present", motion: "active", acceleration: "active", water: "wet"]}

def isValue(device, type) {!(["momentary", "camera"] << getActiveDeviceMap().keySet()).flatten().contains(type)}

def isActive(device, type) {
	try {
	log_trace "isActive $device, $type"
	def field = getDeviceFieldMap()[type]
	def value = "n/a"
	try {
		value = device.respondsTo("currentValue") ? device.currentValue(field) : device.value
	} catch (e) {
		log_error "Device $device ($type) does not report $field properly. This is probably due to numerical value returned as text."
	}
	value == getActiveDeviceMap()[type] ? "active" : "inactive"
	} catch (e) {
		handleError(e, "isActive $device, $type");
	}
}

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

def updateStateTS() {
	log_debug "updating TS"
	state.ts = now()
}

def updateStateID() {
	log_debug "updating stateID"
	state.stateID = now()
}

def getStateTS() {state.ts ?: 0}
def getStateID() {state.stateID ?: 0}

def ping() {
	/* Hide access_token even in personal Live Logging. Too exposed. */
	def dparams = params
	dparams.'access_token' = 'XXX-XXX'
	log_debug "params: $dparams"
	def result
	if (params.stateID as Long != getStateID()) result = [status: "reload"]
	else if (params.ts as Long == getStateTS()) result = [status: "noop", updated: getTS(), ts: getStateTS()]
	else result = [status: "update", updated: getTS(), ts: getStateTS(), data: allDeviceData()]
	//log_debug "ping answer: $result"
	result
}

def saveCSS() {
	state.customCSS = params.css
	css()
}

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
	lights?.each{data << getDeviceData(it, "light")}
	log_trace "allDeviceData collected lights"
	themeLights?.each{data << getDeviceData(it, "themeLight")}
	log_trace "allDeviceData collected themeLights"
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

def handleError(e, method) {
	def error = [message: "\"Unexpected error. You may be able to resolve this by opening the SmartApp Preferences for SmartTiles Dashboard named " +
		"'${app.label}' and pressing Done (not Back) to save all screens. Visit http://SmartTiles.click/error and for further support, send complete log lines (xxxx-xxxx HH:MM:SS ...) to SmartTiles Support.\"",
		mailto: "Support@SmartTiles.click", appVSI: "v${appVersion()}-${appStream()}[${appInstance()}]", error: "\"$e\"", method: "\"${method}\""]
	log.error(error)
	error
}

def getEventsOfDevice(device) {
	def today = new Date()
	def then = timeToday(today.format("HH:mm"), TimeZone.getTimeZone('UTC')) - 1
	device.eventsBetween(then, today, [max: 200])?.findAll{"$it.source" == "DEVICE"}?.collect{[description: it.description, descriptionText: it.descriptionText, displayName: it.displayName, date: it.date, name: it.name, unit: it.unit, source: it.source, value: it.value]}
}

def filterEventsPerCapability(events, deviceType) {
	log_trace "start filterEventsPerCapability"
	def acceptableEventsPerCapability = [
		light           : ["switch"],
		dimmerLight     : ["switch", "level"],
		switch          : ["switch"],
		dimmer          : ["switch", "level"],
		momentary       : ["switch"],
		themeLight      : ["switch"],
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
	log_trace "end filterEventsPerCapability"
	result
}

def getAllDeviceEvents() {
	log_trace "start getAllDeviceEvents"

	def eventsPerCapability = [
		light           : lights                ?.collect{getEventsOfDevice(it)},
		dimmerLight     : dimmerLights          ?.collect{getEventsOfDevice(it)},
		switch          : switches              ?.collect{getEventsOfDevice(it)},
		dimmer          : dimmers               ?.collect{getEventsOfDevice(it)},
		momentary       : momentaries           ?.collect{getEventsOfDevice(it)},
		themeLight      : themeLights           ?.collect{getEventsOfDevice(it)},
		thermostatHeat  : thermostatsHeat       ?.collect{getEventsOfDevice(it)},
		thermostatCool  : thermostatsCool       ?.collect{getEventsOfDevice(it)},
		lock            : locks                 ?.collect{getEventsOfDevice(it)},
		music           : music                 ?.collect{getEventsOfDevice(it)},
		camera          : camera                ?.collect{getEventsOfDevice(it)},
		presence        : presence              ?.collect{getEventsOfDevice(it)},
		contact         : contacts              ?.collect{getEventsOfDevice(it)},
		motion          : motion                ?.collect{getEventsOfDevice(it)},
		temperature     : temperature           ?.collect{getEventsOfDevice(it)},
		humidity        : humidity              ?.collect{getEventsOfDevice(it)},
		water           : water                 ?.collect{getEventsOfDevice(it)},
		battery         : battery               ?.collect{getEventsOfDevice(it)},
		energy          : energy                ?.collect{getEventsOfDevice(it)},
		power           : power                 ?.collect{getEventsOfDevice(it)},
		acceleration    : acceleration          ?.collect{getEventsOfDevice(it)},
		luminosity      : luminosity            ?.collect{getEventsOfDevice(it)},
		weather         : weather               ?.collect{getEventsOfDevice(it)},
	]
	
	def filteredEvents = [:]
	
	eventsPerCapability.each {deviceType, events ->
		filteredEvents[deviceType] = filterEventsPerCapability(events?.flatten(), deviceType)
	}
	def result = filteredEvents.values()?.flatten()?.findAll{it}?.sort{"$it.date.time" + "$it.deviceType"}.reverse()
	
	log_trace "end getAllDeviceEvents"
	
	result 
}

def logLevels() {["Error", "Warn", "Info", "Debug", "Trace"]}

def log_info(obj) {if (logLevels().indexOf(defaultPrefs("logLevel")) >= logLevels().indexOf("Info")) { log.info obj }}
def log_error(obj) {if (logLevels().indexOf(defaultPrefs("logLevel")) >= logLevels().indexOf("Error")) { log.error obj }}
def log_debug(obj) {if (logLevels().indexOf(defaultPrefs("logLevel")) >= logLevels().indexOf("Debug")) { log.debug obj }}
def log_trace(obj) {if (logLevels().indexOf(defaultPrefs("logLevel")) >= logLevels().indexOf("Trace")) { log.trace obj }}

def checkin() {
	try {
		state.appVersion = extendedAppVersion();
		
		def hash = new BigInteger(1,java.security.MessageDigest.getInstance("MD5").digest("${location.id}".getBytes())).toString(16).padLeft(32,"0")
		def timestamp = [".sv": "timestamp"]
        state.refreshCount = (state.refreshCount ?: 0) + 1
		
		/*
		 * Tag Blue instances by prefixing with 10.
		 *    Could prefix with Blue, but nice to stay mostly numeric (since instance normally "0".."5") -> Blue = "100".."105".
		 * TODO: Lots of alternative cleaner ways to format this data. Perhaps improve in future version.
		 */
		def instanceID = appInstance()
		if( appEdition() == "BLUE" ) {
			instanceID = "10" + "${instanceID}"
		}
		
		/*
		 * Record if accessToken is currently defined.
		 */
		def at = false
		if (state.accessToken) {
			at = true
		}
		
		def map = [
			uri: appSettings.apiPath + "/${hash}.json?print=silent&auth=${appSettings.apiKey}", 
			body: ["time" : timestamp, server: getApiServerUrl(), ("${instanceID}") : ["instanceID" : instanceID, "instanceTime" : timestamp,
				"instanceVersion" : 'v' + appVersion() + '-' + appStream(),
				"refreshCount" : state?.refreshCount, tileCount: state.tileCount,
				"launchCount" : state?.launchCount,
				"directCount" : state?.directCount,
				"cssCount" : state?.cssCount,
				"toolsCount" : state?.toolsCount,
				"commandCount" : state?.commandCount,
				"historyCount" : state?.historyCount,
				"at" : at]],
			headers : ["x-http-method-override" : "PATCH"]
		]

		httpPostJson(map) {}
	} catch (e) {
		handleError(e, "checkin")
	}
}

/* 
 * Terry: For tracking purposes, note if /launch endpoint is being used.
 * TODO: This or similar may be be used in v5.8.x for actions related to force access_token wiping.
 * 	     It may be sufficient or easier to just use a one time flag;
 *       though comparing refreshCount to launchCount may be interesting.
 */
def htmlViaLaunch() {
	state.launchCount = (state.launchCount ?: 0) + 1
	log_trace "launchCount: ${state.launchCount}"
	html()
}

def htmlViaDirect() {
	state.directCount = (state.directCount ?: 0) + 1
	log_trace "directCount: ${state.directCount}"
	html()
}

def html() {
	try {
		checkin()
		
		render contentType: "text/html", data: "<!DOCTYPE html><html><head>${head()}${customCSS()} \n<style>${state.customCSS ?: ""}</style></head><body class='theme-${defaultPrefs("theme")}'>\n${renderTiles()}\n${renderWTFCloud()}${footer()}</body></html>"
	} catch (e) {
		return handleError(e, "html")
	}
}

def tools() {
	state.toolsCount = (state.toolsCount ?: 0) + 1
	def logoutItem = """<a href="${apiServerUrl("logout")}" onclick="return logout()"><li class="item tile st-tools-sign-out"><i class="fa fa-fw fa-sign-out"></i>Logout</li></a>"""
	def revokeTokenItem = """<a href="revokeAccessToken" onclick="return revokeAccessToken()"><li class="item tile st-tools-revoke-token"><i class="fa fa-fw fa-key"></i>Revoke Access Token</li></a>"""
	
	render contentType: "text/html", data: """
	<!DOCTYPE html>
	<html>
	<head>
		<meta charset="UTF-8" />
		
		<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=0"/>
		<meta name="apple-mobile-web-app-capable" content="yes" />
		<meta name="apple-mobile-web-app-status-bar-style" content="black" />
		<link rel="icon" sizes="192x192" href="${assetPath()}/icon.png">
		<link rel="apple-touch-icon" href="${assetPath()}/icon.png">
		<meta name="mobile-web-app-capable" content="yes">
		
		<link rel="stylesheet" href="https://code.jquery.com/mobile/1.4.4/jquery.mobile-1.4.4.min.css" />

		<link href="${assetPath()}/style.${appVersion()}.min.css?u=0" rel="stylesheet">
		<link href='https://fonts.googleapis.com/css?family=Mallanna' rel='stylesheet' type='text/css'>
		<link href="https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css" rel="stylesheet">
		
		<title>$app.label Tools</title>
		<script src="https://code.jquery.com/jquery-2.1.1.min.js" type="text/javascript"></script>
		<script src="https://code.jquery.com/mobile/1.4.4/jquery.mobile-1.4.4.min.js" type="text/javascript"></script>
		<script>
			//TODO: These two methods are probably not really needed. Could rely on direct href.
			function app() {
				location.replace('${smartAppUrlWithWebLogin("app")}'); 
				return false;
			}
			function logout() {
				\$.get('${apiServerUrl("logout")}').always(function() {
					app();
				})
				return false;
			}
			
			function revokeAccessToken() {
				var access_token = getUrlParameter("access_token");
				var request = {};
				if (access_token) request["access_token"] = access_token;
				
				\$.get("revokeAccessToken").always(function() {
					location.replace('${launcherPath()}'); 
				})
				return false;
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
			
		</script>
	</head>
	<body>
		<ul class="list toolsList">
			${state.accessToken ? revokeTokenItem : ""}
			${params.access_token ? "" : logoutItem}
			<a href="${launcherPath()}"><li class="item tile st-tools-launcher"><i class="fa fa-fw fa-rocket"></i>SmartTiles \"$app.label\" Launcher (for bookmarking)</li></a>
		</ul>
	</body>
	"""
}


def generateURLWithTokenOrRedirect(path) {
	log_trace "running generateURLWithTokenOrRedirect( " + path + " )"
	if (params.access_token) {
		return apiServerUrl("api/smartapps/installations/$app.id/$path") + "?access_token=$params.access_token"
	} else {
		return smartAppUrlWithWebLogin(path)
	}
}

def renderTiles() {"""<div class="tiles">\n${allDeviceData()?.collect{renderTile(it)}.join("\n")}</div>"""}

def renderWTFCloud() {"""<div data-role="popup" id="wtfcloud-popup" data-overlay-theme="b" class="wtfcloud"><div class="icon cloud" onclick="clearWTFCloud()"><i class="fa fa-cloud"></i></div><div class="icon message" onclick="clearWTFCloud()"><i class="fa fa-question"></i><i class="fa fa-exclamation"></i><i class='fa fa-refresh'></i></div></div>"""}

def launcherPath() {
	def shardURL = getApiServerUrl()
	def shardID = "NA01"
	if (shardURL =~ /na02/) {
		shardID = "NA02"
	} else if (shardURL =~ /eu01/) {
		shardID = "EU01"
	}
	"${launcherURL()}/?app=$app.id&shard=$shardID&label=${app.label}"
}

def link() {
	render contentType: "text/html", data: """<!DOCTYPE html><html><head><meta charset="UTF-8"/><meta name="viewport" content="user-scalable=no, initial-scale=1, maximum-scale=1, minimum-scale=1, width=device-width, height=device-height, target-densitydpi=device-dpi" /></head><body style="margin: 2px;"><div style="padding:10px">\"$app.label\" Dashboard Launcher URL:</div><textarea onclick="this.focus();this.select()" readonly="readonly" style="font-size:larger; width: 90%; height: 6em">${launcherPath()}</textarea><div style="padding:10px">Copy the URL above to any modern browser to view \"${app.label}\" Dashboard.<p>(Use SmartApp configuration to change Label \"${app.label}\".)</p><p>Visit <a href='${helpURL()}/url' target='_blank'>URL Help</a> page.</p></div></body></html>"""
}

def directLink() {
render contentType: "text/html", data: """<!DOCTYPE html><html><head><meta charset="UTF-8"/><meta name="viewport" content="user-scalable=no, initial-scale=1, maximum-scale=1, minimum-scale=1, width=device-width, height=device-height, target-densitydpi=device-dpi" /></head><body style="margin: 2px;"><div style="padding:10px">\"$app.label\" Direct Dashboard URL:</div><textarea onclick="this.focus();this.select()" readonly="readonly" style="font-size:larger; width: 90%; height: 6em">${smartAppUrlWithWebLogin("direct")}</textarea><div style="padding:10px">Copy the URL above to one of the <strong>Link to Other Dashboard</strong> slots in the dashboard configuration that you are linking <strong><em>from</em></strong>.</p><p>Visit <a href='${helpURL()}/url' target='_blank'>URL Help</a> page.</p></div></body></html>"""
}

def css() {
	state.cssCount = (state.cssCount ?: 0) + 1
	def action = css + (params.access_token ? "?access_token=$params.access_token" : "")
	render contentType: "text/html", data: """
	<!DOCTYPE html><html><head><meta charset="UTF-8"/><meta name="viewport" content="user-scalable=no, initial-scale=1, maximum-scale=1, minimum-scale=1, width=device-width, height=device-height, target-densitydpi=device-dpi" /></head>
	<body style="margin: 0; overflow-x: hidden;">
		<form action="$action" method="post"><textarea rows="8" cols="30" style="font-size:12pt; width:98%; padding:1px; margin:1px;" name="css">${state.customCSS ?: "/*enter custom css here*/"}</textarea><br/><input type="submit" value="Save" style="margin-left:10px"></form>
		<div style="padding:5px">Enter custom CSS and tap "Save".<br/><strong>Please note that invalid CSS may break the dashboard. Use at your discretion.</strong>
		<p>Visit our <a href='${helpURL()}/css' target='_blank'>Customizing CSS Help</a> pages.</p>
		<br/><a href="${generateURLWithTokenOrRedirect("tools")}">&lt; BACK</a>
		</div></body></html>
	"""
}


// <script src="https://code.jquery.com/mobile/1.4.4/jquery.mobile-1.4.4.min.js" type="text/javascript"></script>

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

def historyNav() {
"""
<div style="" class="historyNav">
<i class="fa fa-fw fa-arrow-left" onclick="location.replace('${generateURLWithTokenOrRedirect("back")}')"></i>
<i class="fa fa-fw fa-refresh" onclick="this.className = this.className + ' fa-spin'; location.reload();"></i>
<i class="fa fa-fw fa-chevron-up" onclick="window.scrollTo(0, 0);"></i>
</div>
"""
}

def history() {
	if (!defaultPrefs("showHistory") || defaultPrefs("disableDashboard")) return ["history disabled"]
	state.historyCount = (state.historyCount ?: 0) + 1
	render contentType: "text/html", data: """<!DOCTYPE html><html><head>${headHistory()}${customCSS()} \n<style>${state.customCSS ?: ""}</style></head><body class='theme-${defaultPrefs("theme")}'>${historyNav()}<ul class="history-list list">\n${
		getAllDeviceEvents()?.collect{renderEvent(it)}.join("\n")}</ul></body></html>"""
}



/* =========== */
/* End of File */
/* =========== */
