package com.jsherz.cloudfrontlogstoes

import java.nio.file.{Files, Path}
import java.time.Instant
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.alpakka.s3.ListBucketResultContents
import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpecLike
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.{
  DeleteObjectRequest,
  PutObjectRequest
}

import scala.compat.java8.FutureConverters.toScala
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

class FileDownloaderSpec
    extends TestKit(ActorSystem("FileDownloaderSpec"))
    with AnyFunSpecLike
    with BeforeAndAfterAll {

  private val s3Client = S3AsyncClient.builder().build()

  private val testBucket = "cloudfront-logs-to-elasticsearch-tests"

  private val testDir = UUID.randomUUID().toString

  private val testFileKey = s"$testDir/file.gz"

  private val testFileBytes =
    Files.readAllBytes(
      Path.of(getClass.getClassLoader.getResource("file.gz").toURI)
    )

  private val expectedResult =
    Source.fromResource("expected-result").mkString

  override protected def beforeAll(): Unit = {
    Await.ready(
      toScala(
        s3Client.putObject(
          PutObjectRequest
            .builder()
            .bucket(testBucket)
            .key(testFileKey)
            .build(),
          AsyncRequestBody.fromBytes(testFileBytes)
        )
      ),
      10 seconds
    )
  }

  override protected def afterAll(): Unit = {
    s3Client.deleteObject(
      DeleteObjectRequest
        .builder()
        .bucket(testBucket)
        .key(testFileKey)
        .build()
    )
  }

  describe("downloadAndGunzip") {
    it("downloads a file and decompresses the contents") {
      val toDownload = ListBucketResultContents(
        testBucket,
        testFileKey,
        "12345",
        testFileBytes.length.toLong,
        Instant.now(),
        "standard"
      )

      val downloader = new FileDownloader

      val result =
        Await.result(downloader.downloadAndGunzip(toDownload), 30 seconds)

      assert(result == expectedResult)
    }
  }
}
