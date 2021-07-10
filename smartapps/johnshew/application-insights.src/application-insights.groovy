/**
 *  ApplicationInsights
 *
 *  Copyright 2021 John Shewchuk
 *
 */
definition(
    name: "Application Insights",
    namespace: "JohnShew",
    author: "John Shewchuk",
    description: "Log to Application Insights",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
    )


preferences {

	section ("Azure Monitor Application Insights") {
		input "aiConfig", "text", title: "Instrumentation or Connection String"
	}
    
	section("Devices to log to Azure Application Insights") {
		input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required:false, multiple: true
		input "humidities", "capability.relativeHumidityMeasurement", title: "Humidities", required: false, multiple: true
		input "contacts", "capability.contactSensor", title: "Doors open/close", required: false, multiple: true
		input "accelerations", "capability.accelerationSensor", title: "Accelerations", required: false, multiple: true
		input "motions", "capability.motionSensor", title: "Motions", required: false, multiple: true
		input "presence", "capability.presenceSensor", title: "Presence", required: false, multiple: true
		input "switches", "capability.switch", title: "Switches", required: false, multiple: true
		input "waterSensors", "capability.waterSensor", title: "Water sensors", required: false, multiple: true
		input "batteries", "capability.battery", title: "Batteries", required:false, multiple: true
		input "powers", "capability.powerMeter", title: "Power Meters", required:false, multiple: true
		input "energies", "capability.energyMeter", title: "Energy Meters", required:false, multiple: true
	}
    
    section ("Developer") {
    	input "developerMode", "bool", title: "Turn on logging"
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	state.aiInstrumentationKey = settings.aiConfig;
    state.aiUrl = "https://dc.services.visualstudio.com/v2/track";
    
	subscribe(temperatures, "temperature", handleTemperatureEvent)
	subscribe(waterSensors, "water", handleWaterEvent)
	subscribe(humidities, "humidity", handleHumidityEvent)
	subscribe(contacts, "contact", handleContactEvent)
	subscribe(accelerations, "acceleration", handleAccelerationEvent)
	subscribe(motions, "motion", handleMotionEvent)
	subscribe(presence, "presence", handlePresenceEvent)
	subscribe(switches, "switch", handleSwitchEvent)
	subscribe(batteries, "battery", handleBatteryEvent)
	subscribe(powers, "power", handlePowerEvent)
	subscribe(energies, "energy", handleEnergyEvent)
    
}

def handleTemperatureEvent(evt) {
	sendValue(evt) { it.toString() }
}

def handleWaterEvent(evt) {
	sendValue(evt) { it == "wet" ? "true" : "false" }
}

def handleHumidityEvent(evt) {
	sendValue(evt) { it.toString() }
}

def handleContactEvent(evt) {
	sendValue(evt) { it == "open" ? "true" : "false" }
}

def handleAccelerationEvent(evt) {
	sendValue(evt) { it == "active" ? "true" : "false" }
}

def handleMotionEvent(evt) {
	sendValue(evt) { it == "active" ? "true" : "false" }
}

def handlePresenceEvent(evt) {
	sendValue(evt) { it == "present" ? "true" : "false" }
}

def handleSwitchEvent(evt) {
	sendValue(evt) { it == "on" ? "true" : "false" }
}

def handleBatteryEvent(evt) {
	log.debug "Azure Application Insights got battery event ${evt}"
	sendValue(evt) { it.toString() }
}

def handlePowerEvent(evt) {
	sendValue(evt) { it.toString() }
}

def handleEnergyEvent(evt) {
	sendValue(evt) { it.toString() }
}

private sendValue(evt, Closure convert) {
	def eventName = evt.name
    def operationId = evt.deviceId
	def deviceName = evt.displayName.trim()
	def value = convert(evt.value)
    def eventDate = evt.isoDate 
    
	log.debug "Logging to Azure Application Insights ${deviceName} ${eventName} = ${value} at ${eventDate} via ${state.aiInstrumentationKey}"

    def params = [
    	uri: state.aiUrl,
        body: [
            time: eventDate, 
            iKey: state.aiInstrumentationKey,
            name:"Microsoft.ApplicationInsights." + state.aiInstrumentationKey + ".Event", 
            tags:[ 
                "ai.session.id": location.id,
            	"ai.device.id": evt.deviceId,
            	"ai.device.type":"Smartthing",
                "ai.internal.sdkVersion":"Smartthings.0.1",
                "ai.user.id": location.id,
                "ai.operation.id": operationId,
                "ai.operation.name": eventName
                ],
            data: [
                baseType:"EventData",
                    baseData: [
                        ver:2,
                        name: eventName,
                        properties: [ Device: deviceName, Location: location.name ],
                        measurements: [ Value: value.toFloat() ]
                    ]                
                ]
            ]
        ]

    try {
        httpPostJson(params) { response ->
            log.debug "Response code: ${response.status} data: ${response.data}  contentType: ${response.contentType}"
            if (response.status != 200 ) {
				log.debug "Logging failed, status = ${response.status}"
			}
            response.headers.each {
                // log.debug "${it.name} : ${it.value}"
            }
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }
}