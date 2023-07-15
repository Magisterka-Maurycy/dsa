package org.maurycy.framework.dsa.resource

import io.smallrye.common.annotation.Blocking
import jakarta.annotation.security.RolesAllowed
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotAcceptableException
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import org.jboss.resteasy.reactive.RestQuery
import org.jboss.resteasy.reactive.RestResponse
import org.maurycy.framework.dsa.model.FormData
import org.maurycy.framework.dsa.service.StoreService

@Path("store")
class StoreResource(
    private val storeService: StoreService,
) {
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user", "admin")
    suspend fun uploadFile(aFormData: FormData, @Context uriInfo: UriInfo): RestResponse<String> {
        val answer = storeService.storeFiles(aFormData = aFormData)
        return RestResponse.ResponseBuilder
            .created<String>(uriInfo.absolutePathBuilder.path(answer).build()).build()
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    @RolesAllowed("user", "admin")
    fun downloadFile(@PathParam("name") aName: String): Response {
        val e = storeService.findFile(aFileName = aName)
        return Response.ok(
            e.readAllBytes()
        )
            .header("Content-Disposition", "attachment;filename=$aName")
            .header("Content-Type", e.headers()["Content-Type"])
            .build()

    }

    @GET
    @RolesAllowed("user", "admin")
    fun getQuery(@RestQuery search: String?, @RestQuery tag: String?): List<String> {
        if (search != null && tag != null) {
            //TODO: currently there is no full text search with tag compatibility
            throw NotAcceptableException()
        }
        if (search != null) {
            return storeService.searchFull(search)
        }
        if (tag != null) {
            return storeService.searchByTag(tag)
        }
        return storeService.searchAll()
    }

    @DELETE
    @Path("{name}")
    @RolesAllowed("user", "admin")
    fun deleteFile(@PathParam("name") aName: String): Response {
        storeService.deleteFile(aFileName = aName)
        return Response.noContent().build()
    }

}