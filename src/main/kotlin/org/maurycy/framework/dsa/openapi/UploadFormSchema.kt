package org.maurycy.framework.dsa.openapi

// Class that will be used to define the request body, and with that
// it will allow uploading of "N" files
class UploadFormSchema {
    var files: List<UploadItemSchema>? = null
}