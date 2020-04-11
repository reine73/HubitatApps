/**
*	Hubitat Alternative Circadian Daylight 0.1
*
*	Author:
*		Reine Edvardsson
*
*	Documentation:  <add URL>
*
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*                                                                                                     *
*	Ideas taken from from:                                                                            *
*  		Hubitat Circadian Daylight                                                                    *
*		https://raw.githubusercontent.com/adamkempenich/hubitat/master/Apps/CircadianDaylight.groovy  *
*                                                                                                     *
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*
*  Changelog:
*
* 	To-Do:
*
*/

definition(
    name:"Circadian Child",
    namespace: "reineosp",
	parent: "reineosp:Circadian lights",
    author: "Reine Edvardsson",
    description: "Child app for Circadian daylight",
    category: "Lights",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences {
    page(name: "pageConfig")
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    initialize()
}

def initialize() {
	setDefaults()
}

def updateLights(){
    def currentTime = now()
    
    def sunrise=new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", sunriseOverride).time
    def sunset=new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", sunsetOverride).time
    debug ("sunrise time ${sunrise}")
    debug ("Current time ${currentTime}")
    debug ("sunset time ${sunset}")
    
    def dayPart=getPercentageOfDay(sunrise,currentTime,sunset)

    def colourTemp=getIntermidiateValue(CTStartValue,CTMaxValue,CTEndValue,dayPart)
    def dimLevel=getIntermidiateValue(DimStartValue,DimMaxValue,DimEndValue,dayPart)
    debug ("DayPart is ${dayPart}")
    debug ("CT is ${colourTemp}")
    debug ("Dimlevel is ${dimLevel}")
    //debug ("The percentage of the day is ${dayPart}")
    for(colorTemperatureDevice in colorTemperatureDevices) {
        if(colorTemperatureDevice.currentValue("switch") == "on") {
            colorTemperatureDevice.setColorTemperature(colourTemp)
            debug ("${colorTemperatureDevice.label} is set to ${colourTemp}K")
        }
    }
    for(colorTemperatureDevice in colorTemperatureDevices) {
        if(colorTemperatureDevice.currentValue("switch") == "on") {
            colorTemperatureDevice.setLevel(dimLevel)
            debug ("${colorTemperatureDevice.label} is set to ${dimLevel}%")
        }
    }
    
    for(dimmableDevice in dimmableDevices) {
        if(dimmableDevice.currentValue("switch") == "on") {
            dimmableDevice.setLevel(dimLevel)
            debug ("${dimmableDevice.label} is set to ${dimLevel}%")
        }
    }
}

def getPercentageOfDay(start, now, end){
    if (start <= now && now <= end){
        return (now-start)/(end-start)
    } else if (now < start ){
        return -1
    }
    else{
        return 2
    }
}

def getIntermidiateValue(start,max,end,dayPart)
{
    def level=0.0
    if (dayPart < 0){  // before start time, set start value
        level= start
    }
    else if (dayPart<=0.5){
        level=(max-start)*Math.sin(dayPart*Math.PI)+start
    }
    else if (dayPart<=1){
        level=(max-end)*Math.sin(dayPart*Math.PI)+end
    }
    else{  // after end time, set end value
        level=end
    }
    return level.round(0)
}

/////////////////////// Pages and plumming stuff  ///////////////////////////
def pageConfig() {
    dynamicPage(name: "pageConfig", title: "<h2 style='color:#00CED1;font-weight: bold'>Circadian lights</h2>", nextPage: null, install: true, uninstall: true, refreshInterval:0) {	
	display()
    
	section("Instructions:", hideable: true, hidden: true) {
		paragraph "<b>Notes:</b>"
		paragraph "- Select master and slave dimmers you want to keep in sync<br>- The slave(s) will follow the master."
	}
		
	section(getFormat("header-black", " Select lights")) {
		input "colorTemperatureDevices", "capability.colorTemperature", title: "Which Color Temperature capable devices?", multiple:true, required: false
		//input "colorDevices", "capability.colorControl", title: "Which Color-Changing devices?", multiple:true, required: false
        input "dimmableDevices", "capability.switchLevel", title: "Which dimmable devices?", multiple:true, required: false
	}
	section(getFormat("header-black", " Sunrise and sunset")) {
		input "sunriseOverride", "time", title: "Sunrise Override", required: true, hideWhenEmpty: false
		input "sunsetOverride", "time", title: "Sunset Override", required: true, hideWhenEmpty: false
	}
	section(getFormat("header-black", " Colour temps")) {
		input "CTStartValue", "number", title: "Start Colour Temperature", required: true, hideWhenEmpty: false
		input "CTMaxValue", "number", title: "Max Colour Temperature", required: true, hideWhenEmpty: false
		input "CTEndValue", "number", title: "End Colour Temperature", required: true, hideWhenEmpty: false
	}
	section(getFormat("header-black", " Dim levels")) {
		input "DimStartValue", "number", title: "Start Dim Level", required: true, hideWhenEmpty: false
		input "DimMaxValue", "number", title: "Max Dim Level", required: true, hideWhenEmpty: false
		input "DimEndValue", "number", title: "End Dim Level", required: true, hideWhenEmpty: false
	}
	section(getFormat("header-black", " General")) {label title: "Enter a name for this child app", required: true}
	section() {
		input(name: "logEnable", type: "bool", defaultValue: "true", title: "Enable Debug Logging", description: "Enable extra logging for debugging.")
   	}
	
	display2()
	}
}

def debug(txt){
    try {
		if (settings.logEnable) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
    	log.error("${app.label} - LOGDEBUG unable to output requested data!")
    }
}

def setDefaults(){
	if(logEnable == null){logEnable = false}
}

def display() {
	section() {
		paragraph getFormat("line")
		input "pause1", "bool", title: "Pause This App", required: true, submitOnChange: true, defaultValue: false
	}
}

def display2() {
	section() {
		paragraph getFormat("line")
		paragraph "<div style='text-align:center'>Circadian Child - App Version: 0.1.0</div>"
	}
}

def getFormat(type, myText=""){
	if(type == "header-black") return "<div style='color:#000000;font-weight: bold;background-color:#E9E9E9;border: 0px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
	if(type == "header-darkcyan") return "<div style='color:#ffffff;font-weight: bold;background-color:#000000;border: 0px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
	if(type == "line") return "\n<hr style='background-color:#000000; height: 1px; border: 0;'></hr>"
	if(type == "title") return "<h2 style='color:#000000;font-weight: bold;font-style: italic'>${myText}</h2>"
}