package org.maurycy.framework.dsa.openapi

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(type = SchemaType.STRING, format = "binary")
interface UploadItemSchema