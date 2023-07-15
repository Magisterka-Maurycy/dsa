package org.maurycy.framework.dsa.model

import org.jboss.resteasy.reactive.RestForm
import org.jboss.resteasy.reactive.multipart.FileUpload

class FormData {
    @RestForm("files")
    var files: List<FileUpload> = emptyList()
    @RestForm("tags")
    var tags: List<String> = emptyList()
}