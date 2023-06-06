package org.maurycy.framework.dsa

import io.quarkus.arc.profile.UnlessBuildProfile
import io.quarkus.security.spi.runtime.AuthorizationController
import jakarta.annotation.Priority

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Alternative
import jakarta.interceptor.Interceptor
import org.eclipse.microprofile.config.inject.ConfigProperty


@Alternative
@Priority(Interceptor.Priority.LIBRARY_AFTER)
@ApplicationScoped
// tests have their own mechanism for disabling authorization
@UnlessBuildProfile("test")
class DisabledAuthController : AuthorizationController() {
    @ConfigProperty(name = "disable.authorization", defaultValue = "false")
    var disableAuthorization = false
    override fun isAuthorizationEnabled(): Boolean {
        return !disableAuthorization
    }
}