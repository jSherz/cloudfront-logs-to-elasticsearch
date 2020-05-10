package com.jsherz.cloudfrontlogstoes

import java.util.zip.GZIPInputStream

import akka.actor.ActorSystem
import akka.dispatch.MessageDispatcher
import akka.stream.alpakka.s3.ListBucketResultContents
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.{
  GetObjectRequest,
  GetObjectResponse
}

import scala.compat.java8.FutureConverters.toScala
import scala.concurrent.Future
import scala.io.Source

/**
  * Used to download and decompress the log files stored in S3.
  */
class FileDownloader(private implicit val system: ActorSystem) {
  private val s3Client = S3AsyncClient.builder().build()

  private implicit val executionContext: MessageDispatcher =
    system.dispatchers.lookup("file-downloader-dispatcher")

  /**
    * Used to map over the items returned by [[akka.stream.alpakka.s3.scaladsl.S3.listBucket]] and download the file. As
    * CloudFront log files are gzipped, we also decompress them into a [[String]].
    *
    * @param file
    * @return
    */
  def downloadAndGunzip(file: ListBucketResultContents): Future[String] = {
    val getResponseFuture = s3Client.getObject(
      GetObjectRequest.builder().bucket(file.bucketName).key(file.key).build(),
      AsyncResponseTransformer.toBytes[GetObjectResponse]
    )
    toScala(getResponseFuture).map(body => {
      val in = new GZIPInputStream(body.asInputStream())
      val out = Source.fromInputStream(in).mkString

      system.log.debug(
        s"Downloaded and gunzipped file s3://${file.bucketName}/${file.key}"
      )

      out
    })
  }
}
