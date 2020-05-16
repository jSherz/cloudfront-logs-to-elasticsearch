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
import software.amazon.awssdk.services.sqs.model.Message

import scala.compat.java8.FutureConverters.toScala
import scala.concurrent.Future
import scala.io.Source

/**
  * Used to download and decompress the log files stored in S3.
  */
class FileDownloader(private implicit val system: ActorSystem) {

  private val s3Client = S3AsyncClient
    .builder()
    .build()

  private implicit val executionContext: MessageDispatcher =
    system.dispatchers.lookup("file-downloader-dispatcher")

  private def downloadAndDecompress(
      bucket: String,
      key: String
  ): Future[String] = {
    val getResponseFuture = s3Client.getObject(
      GetObjectRequest.builder().bucket(bucket).key(key).build(),
      AsyncResponseTransformer.toBytes[GetObjectResponse]
    )
    toScala(getResponseFuture).map(body => {
      val in = new GZIPInputStream(body.asInputStream())
      val out = Source.fromInputStream(in).mkString

      system.log.info(
        s"Downloaded and gunzipped file s3://${bucket}/${key}"
      )

      out
    })
  }

  /**
    * Used to map over the items returned by [[akka.stream.alpakka.s3.scaladsl.S3.listBucket]] and download the file. As
    * CloudFront log files are gzipped, we also decompress them into a [[String]].
    *
    * @param file
    * @return
    */
  def downloadAndGunzip(file: ListBucketResultContents): Future[String] =
    downloadAndDecompress(file.bucketName, file.key)

  /**
    * Used to receive file upload notifications via  [[akka.stream.alpakka.sqs.scaladsl.SqsSource]] and download the
    * file before decompressing it.
    *
    * @param file
    * @return
    */
  def downloadAndGunzipSingle(message: Message): Future[String] = {
    import SQSNotificationMessageJsonProtocol._
    import spray.json._

    val parsed =
      message.body().parseJson.convertTo[WrappedSQSNotificationMessage]

    val bucket = parsed.records.head.s3.bucket.name
    val key = parsed.records.head.s3.`object`.key

    system.log.info(s"Downloading and processing s3://$bucket/$key")

    downloadAndDecompress(bucket, key)
  }

  def close(): Unit = {
    s3Client.close()
  }
}
