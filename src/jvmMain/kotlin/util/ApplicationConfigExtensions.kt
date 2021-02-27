package util

import io.ktor.config.*

operator fun ApplicationConfig.get(propertyName: String) = property(propertyName).getString()