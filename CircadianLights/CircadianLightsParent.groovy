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
*  		Circadian Daylight                                                                            *
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
    name:"Circadian lights",
    namespace: "reineosp",
    author: "Reine Edvardsson",
    description: "Parent app for Circadian daylight app",
    category: "Lights",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
    )

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
} 

def installed() {
    
    log.debug "Installed with settings:"
    initialize()
}

def uninstalled() {
	unschedule()
}

def updated() {
    log.debug "Updated"
    updateLights()
    initialize()
}

def initialize() {
    log.info "There are ${childApps.size()} child apps"
    childApps.each {child ->
        log.info "Child app: ${child.label}"
    }
    unschedule()
    schedule("0 0/5 * * * ? *",updateLights)
}

def updateLights(){
    childApps.each {child ->
        log.info "Update lighs on ${child.label}"
        child.updateLights()
    }
}

def mainPage() {
    dynamicPage(name: "mainPage") {
    	installCheck()
		if(state.appInstalled == 'COMPLETE'){
			section(getFormat("title", "${app.label}")) {
				paragraph "<div>Circadian lights</div>"
				paragraph getFormat("line")
			}
			section("Instructions:", hideable: true, hidden: true) {
				paragraph "<b>Notes:</b>"
				paragraph "- Add lights to control during the day<br>"
			}
  			section(getFormat("header-black", " Child Apps")) {
				app(name: "anyOpenApp", appName: "Circadian Child", namespace: "reineosp", title: "<b>Add a new 'Circadian' child</b>", multiple: true)
  			}
 			section(getFormat("header-black", " General")) {
       				label title: "Enter a name for parent app (optional)", required: false
 			}
			display()
		}
	}
}

def installCheck(){         
	state.appInstalled = app.getInstallationState() 
	if(state.appInstalled != 'COMPLETE'){
		section{paragraph "Please hit 'Done' to install '${app.label}' parent app "}
  	}
  	else{
    	log.info "Parent Installed OK"
  	}
}

def getFormat(type, myText=""){
	if(type == "header-black") return "<div style='color:#000000;font-weight: bold;background-color:#E9E9E9;border: 0px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
	if(type == "header-darkcyan") return "<div style='color:#ffffff;font-weight: bold;background-color:#000000;border: 0px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
	if(type == "line") return "\n<hr style='background-color:#000000; height: 1px; border: 0;'></hr>"
	if(type == "title") return "<h2 style='color:#000000;font-weight: bold;font-style: italic'>${myText}</h2>"
}

def display(){
	section() {
		paragraph getFormat("line")
		paragraph "<div style='text-align:center'>Circadian Lights - App Version: 0.1</div>"
	}       
}  