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

@Path("v2/store")

class StoreResourceV2(
    private val storeService: StoreService,
) {
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user", "admin")
    @Path("{bucket}")
    suspend fun uploadFile(@PathParam("bucket") aBucket: String, aFormData: FormData, @Context uriInfo: UriInfo): RestResponse<String> {
        val answer = storeService.storeFiles(aBucket= aBucket, aFormData = aFormData)
        return RestResponse.ResponseBuilder
            .created<String>(uriInfo.absolutePathBuilder.path(answer).build()).build()
    }

    @GET
    @Path("{bucket}/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    @RolesAllowed("user", "admin")
    fun downloadFile(@PathParam("bucket") aBucket: String, @PathParam("name") aName: String): Response {
        val e = storeService.findFile(aBucket= aBucket,aFileName = aName)
        return Response.ok(
            e.readAllBytes()
        )
            .header("Content-Disposition", "attachment;filename=$aName")
            .header("Content-Type", e.headers()["Content-Type"])
            .build()

    }

    @GET
    @RolesAllowed("user", "admin")
    @Path("{bucket}")
    fun getQuery(@PathParam("bucket") aBucket: String, @RestQuery search: String?, @RestQuery tag: String?): List<String> {
        if (search != null && tag != null) {
            //TODO: currently there is no full text search with tag compatibility
            throw NotAcceptableException()
        }
        if (search != null) {
            return storeService.searchFull(aBucket = aBucket, aInput = search)
        }
        if (tag != null) {
            return storeService.searchByTag(aBucket = aBucket, aTag = tag)
        }
        return storeService.searchAll(aBucket = aBucket)
    }

    @DELETE
    @Path("{bucket}/{name}")
    @RolesAllowed("user", "admin")
    fun deleteFile(@PathParam("bucket") aBucket: String, @PathParam("name") aName: String): Response {
        storeService.deleteFile(aBucket = aBucket, aFileName = aName)
        return Response.noContent().build()
    }

}