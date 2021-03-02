package util

import io.ktor.config.ApplicationConfig

operator fun ApplicationConfig.get(propertyName: String) = property(propertyName).getString()
