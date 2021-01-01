package fi.samzone.utils

static String read(String propertyName) {
	propertyName = propertyName.trim()
	Properties properties = new Properties()
	File prodPropertiesFile = new File('prod.properties')
	File devPropertiesFile = new File('dev.properties')
	File defaultPropertiesFile = new File('default.properties')
	File propertiesFile = prodPropertiesFile.exists() ? prodPropertiesFile :
			(devPropertiesFile.exists() ? devPropertiesFile : defaultPropertiesFile)
	propertiesFile.withInputStream { properties.load(it) }
	String propertyValue = properties.getProperty(propertyName)
	if (propertyValue == null)
		throw new IllegalArgumentException("Property \"${propertyName}\" does not exists in configuration file ${propertiesFile.name}.")
	return propertyValue.trim()
}

static Integer readInt(String propertyName) {
	return ConfigurationProperties.read(propertyName) as Integer
}

static Boolean readBoolean(String propertyName) {
	return ConfigurationProperties.read(propertyName).toLowerCase().trim() in ['true', '1']
}

