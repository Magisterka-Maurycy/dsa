package org.maurycy.framework.dsa.exception.mapper

import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.maurycy.framework.dsa.exception.TooManyFilesSentException

@Provider
class TooManyFilesSentExceptionMapper : ExceptionMapper<TooManyFilesSentException> {
    override fun toResponse(exception: TooManyFilesSentException): Response {
        return Response.notAcceptable(emptyList()).build()
    }
}