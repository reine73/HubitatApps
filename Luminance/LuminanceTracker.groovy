definition(
    name: "LuminanceTracker",
    namespace: "reineosp",
    author: "Reine Edvardsson",
    description: "Tracking luminance and sets/unsets a switch based on levels",
    category: "Convenience",
    
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "" )

preferences{ 
    page( name: "appSetup")
}

def appSetup(){ 
    dynamicPage(name: "appSetup", title: "Luminance tracker settings", nextPage: null, install: true, uninstall: true, refreshInterval: 0) {
        
        section("Settings"){
            input "luminanceSensor", "capability.illuminanceMeasurement", title: "Select luminance sensor", multiple: false, required: true
            input "luminanceSwitch", "capability.switch", title: "Select switch to activate/deactivate", multiple: false, required: true
			input "lowerLimit", "number", title: "Lower threshold", required: true, hideWhenEmpty: false
			input "upperLimit", "number", title: "Upper threshold", required: true, hideWhenEmpty: false
        }
    }
}

def installed() {
    log.debug "Installed application"
    unsubscribe()
    initialize()
}

def updated() {
    log.debug "Updated application"
    unsubscribe()
    initialize()
}

def uninstalled() {
    log.debug "Uninstalled application"
    unsubscribe()
}

def initialize(){
    log.info("Initializing with settings: ${settings}")
    subscribe(settings.luminanceSensor, "illuminance", eventHandler)
    eventHandler(null)
}

def eventHandler( evt ){
    def crntLux = luminanceSensor.currentValue("illuminance").toInteger()
    log.debug "Illuminance read at level ${crntLux}"
    if(crntLux <= lowerLimit){
        luminanceSwitch.on()
    } else if(crntLux >= upperLimit){
        luminanceSwitch.off()
    }
}
