package org.maurycy.framework.dsa.service


import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.GetObjectResponse
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.quarkus.logging.Log
import io.quarkus.tika.TikaContent
import io.quarkus.tika.TikaMetadata
import io.quarkus.tika.TikaParser
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import jakarta.enterprise.context.ApplicationScoped
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import org.apache.http.util.EntityUtils
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.elasticsearch.client.Request
import org.elasticsearch.client.Response
import org.elasticsearch.client.ResponseException
import org.elasticsearch.client.RestClient
import org.jboss.resteasy.reactive.multipart.FileUpload
import org.maurycy.framework.dsa.exception.NoFileSentException
import org.maurycy.framework.dsa.exception.TooManyFilesSentException
import org.maurycy.framework.dsa.model.FormData
import org.maurycy.framework.dsa.model.StoredContent


@ApplicationScoped
class StoreService(
    private val minio: MinioClient,
    private val elasticSearchClient: RestClient,
    private val tikaParser: TikaParser,
    @ConfigProperty(name = "default.bucket.name", defaultValue = "test")
    private val bucketName: String
) {

    fun storeFiles(aFormData: FormData) = storeFiles(bucketName, aFormData)
    fun storeFiles(aBucket: String, aFormData: FormData): String {
        createBucket(aBucket)
        val files = aFormData.files
        if (files.size > 1) {
            throw TooManyFilesSentException()
        }
        if (files.isEmpty()) {
            throw NoFileSentException()
        }
        return storeFile(aBucket, files[0], aFormData.tags)
    }

    fun findFile(aFileName: String) = findFile(bucketName, aFileName)
    fun findFile(aBucket: String, aFileName: String): GetObjectResponse {
        return minio.getObject(
            GetObjectArgs.builder()
                .bucket(aBucket)
                .`object`(aFileName)
                .build()
        )
    }

    private fun createBucket(aBucket: String) {
        if (minio.bucketExists(BucketExistsArgs.builder().bucket(aBucket).build())) {
            return
        }
        minio.makeBucket(MakeBucketArgs.builder().bucket(aBucket).build())
    }

    private fun storeFile(aBucket: String, aFileUpload: FileUpload, aTags: List<String>): String {
        val inputStream = FileInputStream(aFileUpload.filePath().toString())
        val baos = ByteArrayOutputStream()
        inputStream.transferTo(baos)
        inputStream.close()
        val cloneForMinio: InputStream = ByteArrayInputStream(baos.toByteArray())
        val cloneForIndexing: InputStream = ByteArrayInputStream(baos.toByteArray())

        val response = minio.putObject(
            PutObjectArgs.builder()
                .bucket(aBucket)
                .stream(cloneForMinio, aFileUpload.size(), -1)
                .contentType(aFileUpload.contentType())
                .`object`(aFileUpload.fileName())
                .build()
        )
        val id = response.bucket() + "-" + response.`object`()

        val request = Request(
            "PUT",
            "/minio$aBucket/_doc/$id"
        )
        val storedContent = StoredContent()
        val parsed = parseInputStream(cloneForIndexing)
        storedContent.id = id
        storedContent.content = parsed.text
        storedContent.metaData = metaDataParse(parsed.metadata)
        storedContent.name = response.`object`()
        storedContent.bucket = response.bucket()
        storedContent.etag = response.etag()
        storedContent.tags = aTags
        request.setJsonEntity(JsonObject.mapFrom(storedContent).toString())
        elasticSearchClient.performRequest(request)
        cloneForMinio.close()
        cloneForIndexing.close()
        return aFileUpload.fileName()
    }

    private fun metaDataParse(tikaMetadata: TikaMetadata): Map<String, List<String>> {
        val map = mutableMapOf<String, List<String>>()
        tikaMetadata.names.forEach {
            val res = tikaMetadata.getValues(it)
            map[it] = res
            Log.info("$it :$res")
        }
        return map
    }

    private val listOfIndex = listOf("content", "bucket", "etag", "name", "metaData", "tags")
    fun searchFull(aInput: String) = searchFull(bucketName, aInput)
    fun searchFull(aBucket: String, aInput: String): List<String> {
        return search(aBucket, listOfIndex, aInput)
    }

    private fun search(aBucket: String, aTerms: List<String>, aMatch: String): List<String> {
        return search(aBucket, aTerms, aMatch, false)
    }

    @Throws(IOException::class)
    private fun search(aBucket: String, aTerms: List<String>, aMatch: String, aStrong: Boolean): List<String> {
        val request = Request(
            "GET",
            "/minio$aBucket/_search"
        )
        val terms = JsonArray()
        aTerms.forEach {
            terms.add(it)
        }

        /**
         * Query based on:  <a href="URL#https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html">link</a>
         **/
        val queryString = JsonObject().put("fields", terms)
        if (aStrong) {
            queryString.put("query", aMatch)
        } else {
            queryString.put("query", "*$aMatch*")
        }
        val matchJson = JsonObject().put("query_string", queryString)
        val queryJson = JsonObject().put("query", matchJson)
        Log.info("query json: ${queryJson.encodePrettily()}")
        request.setJsonEntity(queryJson.encode())
        val response: Response
        try {
            response = elasticSearchClient.performRequest(request)
        } catch (
            responseException: ResponseException
        ) {
            // in case of ResponseException it will return empty list it can happen when no files where uploaded and
            // index does not exist yet in elasticsearch cluster
            return emptyList()
        }

        val responseBody: String = EntityUtils.toString(response.entity)
        val json = JsonObject(responseBody)
        val hits: JsonArray = json.getJsonObject("hits").getJsonArray("hits")
        val results: MutableList<StoredContent> = ArrayList(hits.size())
        for (i in 0 until hits.size()) {
            val hit: JsonObject = hits.getJsonObject(i)
            val storedContent: StoredContent = hit.getJsonObject("_source").mapTo(StoredContent::class.java)
            results.add(storedContent)
        }

        return results.map {
            it.name
        }.stream().toList().toSet().toList()
    }

    private fun parseInputStream(aInputStream: InputStream): TikaContent {
        return tikaParser.parse(aInputStream)
    }

    fun deleteFile(aFileName: String) = deleteFile(bucketName, aFileName)
    fun deleteFile(aBucket: String, aFileName: String): String {
        val id = "$aBucket-$aFileName"
        minio.removeObject(
            RemoveObjectArgs.builder()
                .bucket(aBucket)
                .`object`(aFileName)
                .build()
        )
        val request = Request(
            "DELETE",
            "/minio$aBucket/_doc/$id"
        )
        val res = elasticSearchClient.performRequest(request)
        Log.info(res)
        Log.info(res.toString())
        return ""
    }

    fun searchAll() = searchAll(bucketName)
    fun searchAll(aBucket: String): List<String> {
        return searchFull(aBucket, "*")
    }

    fun searchByTag(aTag: String) = searchByTag(bucketName, aTag)

    fun searchByTag(aBucket: String, aTag: String): List<String> {
        val set = mutableSetOf<String>()
        val searchParam: String = aTag
        set.addAll(search(aBucket, listOf("tags"), searchParam, true))
        return set.toList()
    }

    fun getAllBuckets(): List<String> {
        return minio.listBuckets().map {
            it.name()
        }
    }


}