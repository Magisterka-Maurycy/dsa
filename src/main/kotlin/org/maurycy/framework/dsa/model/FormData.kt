package org.maurycy.framework.dsa.model

import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.jboss.resteasy.reactive.RestForm
import org.jboss.resteasy.reactive.multipart.FileUpload
import org.maurycy.framework.dsa.openapi.UploadFormSchema


// We instruct OpenAPI to use the schema provided by the 'UploadFormSchema'
// class implementation and thus define a valid OpenAPI schema for the Swagger
// UI
@Schema(implementation = UploadFormSchema::class)
class FormData {
    @RestForm("files")
    var files: List<FileUpload>? = null
}