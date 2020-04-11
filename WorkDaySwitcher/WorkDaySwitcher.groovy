definition(
    name: "WorkDaySwitcher",
    namespace: "reineosp",
    author: "Reine Edvardsson",
    description: "If workday or not",
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
            input "bankdaySwitch", "capability.switch", title: "Select switch for bankday", multiple: false, required: true
            input "vacationSwitch", "capability.switch", title: "Select switch for vacation mode", multiple: false, required: true
			input "workdaySwitch", "capability.switch", title: "Select switch to activate/deactivate", multiple: false, required: true
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
    subscribe(settings.bankdaySwitch, "switch", eventHandler)
	subscribe(settings.vacationSwitch, "switch", eventHandler)
    eventHandler(null)
}

def eventHandler( evt ){
    def bdSwitch=bankdaySwitch.currentValue("switch")
	def vmSwitch=vacationSwitch.currentValue("switch")
    log.debug "bankdaySwitch is ${bdSwitch}"
	log.debug "vacationSwitch is ${vmSwitch}"
    if(bankdaySwitch.currentValue("switch")=="on" && vacationSwitch.currentValue("switch")=="off"){
        workdaySwitch.on()
    } else {
        workdaySwitch.off()
    }
}
