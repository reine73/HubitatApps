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
    debug "Installed with settings: ${settings}"
    unsubscribe()
    initialize()
}

def uninstalled() {
    debug "Uninstalled"
    unsubscribe()
}

def updated() {
    debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
	setDefaults()
    subscribe(settings.activationSwitch, "switch", eventHandler)
    
}

def eventHandler(evt)
{
    if (pauseApp){
        info("App is paused")
        return
    }
    if (activationSwitch.currentValue("switch")=="on"){
        
        for(colorTemperatureDevice in colorTemperatureDevices) {
            colorTemperatureDevice.on()    
        }
        for(dimmableDevice in dimmableDevices) {
            dimmableDevice.on()
        }
        runInMillis(500,updateLights,null)
    }
    else{
        def CT=getLowestCTlevel()
        def lvl=getLowestDimlevel()
        setLevels(lvl,CT)
        runInMillis(2000,turnOffDevices,null)
    }
}


def updateLights(){
    if (pauseApp){
        info("App is paused")
        return
    }
    def currentTime = now()
    def partOfDay=getPartOfDay(currentTime)
    debug ("Part of day= ${partOfDay}")
    def sunrise=new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", sunriseOverride).time
    def sunset=new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", sunsetOverride).time
    def dimstart=new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", dimdownStart).time
    def dimend=new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", dimdownEnd).time
    
    debug ("sunrise time ${sunrise}")
    debug ("Current time ${currentTime}")
    debug ("sunset time ${sunset}")
    
    def fraction
    //Defaults to partOfDay==0, dimlevel as start of day
    def dimLevel=DimStartValue
    def colourTemp=CTStartValue
    
    if (exceptionEnable && exceptionMode()){
        debug ("In exceptionmode")
        colourTemp=exceptionCTLevel
        dimLevel=exceptionLevel
    }
    else if (partOfDay==1){
        fraction=getFraction(sunrise,currentTime,sunset)
        colourTemp=getIntermidiateValue(CTStartValue,CTMaxValue,CTEndValue,fraction)
        dimLevel=getIntermidiateValue(DimStartValue,DimMaxValue,DimEndValue,fraction)
    }
    else if (partOfDay==2){    
        dimLevel=DimEndValue
        colourTemp=CTEndValue
    }
    else if (partOfDay==3){
        if (dimdownEnable && !ignoreMode()){
            fraction=getFraction(dimstart,currentTime,dimend)
            colourTemp=getLinearIntermidiateValue(CTEndValue,DimdownCTLevel,fraction)
            dimLevel=getLinearIntermidiateValue(DimEndValue,DimdownLevel,fraction)
        }else{
            dimLevel=DimEndValue
            colourTemp=CTEndValue
        }
    }
    else if (partOfDay==4){
        dimLevel=(dimdownEnable)?DimdownLevel:DimEndValue
        colourTemp=(dimdownEnable)?DimdownCTLevel:CTEndValue
    }

    debug ("DayPart is ${fraction}")
    debug ("CT is ${colourTemp}")
    debug ("Dimlevel is ${dimLevel}")
    setLevels(dimLevel,colourTemp)
}

def turnOffDevices(data)
{
    for(colorTemperatureDevice in colorTemperatureDevices) {
        colorTemperatureDevice.off() 
        debug ("Turning off ${colorTemperatureDevice.label}")
    }
    for(dimmableDevice in dimmableDevices) {
        dimmableDevice.off()
        debug ("Turning off ${dimmableDevice.label}")

    }
}

def setLevels(dimLevel,CTLevel)
{
    for(colorTemperatureDevice in colorTemperatureDevices) {
        if(colorTemperatureDevice.currentValue("switch") == "on") {
            colorTemperatureDevice.setColorTemperature(CTLevel)
            debug ("${colorTemperatureDevice.label} is set to ${CTLevel}K")
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

def getLowestCTlevel(){
    def level=CTStartValue
    if (level>CTEndValue){
        level=CTEndValue
    }
    if (dimdownEnable && DimdownCTLevel!=null && level>DimdownCTLevel){
        level=DimdownCTLevel
    }
    if (exceptionEnable && exceptionCTLevel!=null && level>exceptionCTLevel){
        level=exceptionCTLevel
    }
    return level
}

def getLowestDimlevel(){
    def level=DimStartValue
    if (level>DimEndValue){
        level=DimEndValue
    }
    if (dimdownEnable && level>DimdownLevel){
        level=DimdownLevel
    }
    if (exceptionEnable && exceptionLevel!=null && level>exceptionLevel){
        level=exceptionLevel
    }
    return level
}

def getFraction(start, now, end){
    if (start <= now && now <= end){
        return (now-start)/(end-start)
    } else if (now < start ){
        return -1
    }
    else{
        return 2
    }
}

def getLinearIntermidiateValue(high,low,fraction)
{
    return (high-low)*(1-fraction)+low
}

def getIntermidiateValue(start,max,end,fraction)
{
    def level=0.0
    if (fraction < 0){  // before start time, set start value
        level= start
    }
    else if (fraction<=0.5){
        level=(max-start)*Math.sin(fraction*Math.PI)+start
        level=level.round(0)
    }
    else if (fraction<=1){
        level=(max-end)*Math.sin(fraction*Math.PI)+end
        level=level.round(0)
    }
    else{  // after end time, set end value
        level=end
    }
    return level
}

def getPartOfDay(currentTime)
{
    def part=0
    def sunrise=new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", sunriseOverride).time
    def sunset=new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", sunsetOverride).time
    def dimstart=new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", dimdownStart).time
    def dimend=new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", dimdownEnd).time
    
    if (sunrise <= currentTime && currentTime <= sunset){
        part=1
    }
    else if (sunset <= currentTime && currentTime <= dimstart){
        part=2
    }
    else if (dimstart <= currentTime && currentTime <= dimend){
        part=3
    }
    else if (dimend<= currentTime){
        part=4
    }
    return part
}

def ignoreMode() {
	def result = !ignoreModes || ignoreModes.contains(location.mode)
    debug ("Ignore mode: ${result}")
    return result
}

def exceptionMode() {
	def result = !exceptionModes || exceptionModes.contains(location.mode)
    debug ("Exception mode: ${result}")
    return result
}

/////////////////////// Pages and plumming stuff  ///////////////////////////
def pageConfig() {
    dynamicPage(name: "pageConfig", title: "<h2 style='color:#00CED1;font-weight: bold'>Circadian lights</h2>", nextPage: null, install: true, uninstall: true, refreshInterval:0) {	
	//display()
    
	//section("Instructions:", hideable: true, hidden: true) {
	//	paragraph "<b>Notes:</b>"
	//	paragraph "- Select master and slave dimmers you want to keep in sync<br>- The slave(s) will follow the master."
	//}
    section(getFormat("header-black", "On/off switch")) {
        input "activationSwitch", "capability.switch", title: "Select switch to turn on/off", multiple: false, required: true
    }
	section(getFormat("header-black", " Select lights")) {
		input "colorTemperatureDevices", "capability.colorTemperature", title: "Which Color Temperature capable devices?", multiple:true, required: false
		//input "colorDevices", "capability.colorControl", title: "Which Color-Changing devices?", multiple:true, required: false
        input "dimmableDevices", "capability.switchLevel", title: "Which dimmable devices?", multiple:true, required: false
	}
    section(getFormat("header-black", " Sunrise settings")) {
        input "sunriseOverride", "time", title: "Sunrise time", required: true, hideWhenEmpty: false
        input "DimStartValue", "number", title: "Start Dim Level", required: true, hideWhenEmpty: false
        input "CTStartValue", "number", title: "Start Colour Temperature", required: true, hideWhenEmpty: false
    }
    section(getFormat("header-black", " Midday settings")) {
        input "DimMaxValue", "number", title: "Max Dim Level", required: true, hideWhenEmpty: false
        input "CTMaxValue", "number", title: "Max Colour Temperature", required: true, hideWhenEmpty: false
    }
    section(getFormat("header-black", " Sunset settings")) {
        input "sunsetOverride", "time", title: "Sunset time", required: true, hideWhenEmpty: false
        input "DimEndValue", "number", title: "End Dim Level", required: true, hideWhenEmpty: false
        input "CTEndValue", "number", title: "End Colour Temperature", required: true, hideWhenEmpty: false
    }
    section(getFormat("header-black", " Dimdown settings")) {
        input "dimdownEnable", "bool", title: "Enable dim down", required: true, submitOnChange: true, defaultValue: false
        input "dimdownStart", "time", title: "Dimdown start time", required: false, hideWhenEmpty: false
        input "dimdownEnd", "time", title: "Dimdown end time", required: false, hideWhenEmpty: false
        input "DimdownLevel", "number", title: "End Dim Level", required: false, hideWhenEmpty: false
        input "DimdownCTLevel", "number", title: "End Colour Temperature", required: false, hideWhenEmpty: false
        input( name	: "ignoreModes" ,type	: "mode" ,title	: "Ignore dimdown for modes",multiple	: true,required	: false )
    }
    section(getFormat("header-black", " Exception modes")) {
        input "exceptionEnable", "bool", title: "Enable exception", required: true, submitOnChange: true, defaultValue: false
        input "exceptionLevel", "number", title: "Dim Level", required: false, hideWhenEmpty: false
        input "exceptionCTLevel", "number", title: "Colour Temperature", required: false, hideWhenEmpty: false
        input( name	: "exceptionModes" ,type	: "mode" ,title	: "Exception for modes",multiple	: true,required	: false )
    }

	section(getFormat("header-black", " General")) {label title: "Enter a name for this child app", required: true}
	section() {
		input(name: "logEnable", type: "bool", defaultValue: "true", title: "Enable Debug Logging", description: "Enable extra logging for debugging.")
        input(name: "pauseApp", type: "bool", defaultValue: "false", title: "Pause application", description: "Pause application")
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

def info(txt){
    log.info("${app.label} - ${txt}") 
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