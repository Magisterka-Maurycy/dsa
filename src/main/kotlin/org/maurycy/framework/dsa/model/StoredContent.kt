package org.maurycy.framework.dsa.model

class StoredContent {
    lateinit var metaData: Map<String, List<String>>
    lateinit var id: String
    lateinit var bucket: String
    lateinit var name: String
    lateinit var etag: String
    lateinit var content: String
    lateinit var tags: List<String>
}