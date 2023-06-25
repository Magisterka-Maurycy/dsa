package org.maurycy.framework.dsa.exception.mapper

import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.maurycy.framework.dsa.exception.NoFileSentException

@Provider
class NoFileSentExceptionMapper : ExceptionMapper<NoFileSentException> {
    override fun toResponse(exception: NoFileSentException): Response {
        return Response.notAcceptable(emptyList()).build()
    }
}