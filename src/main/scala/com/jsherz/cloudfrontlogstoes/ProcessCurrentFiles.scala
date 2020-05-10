package com.jsherz.cloudfrontlogstoes

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.elasticsearch.{
  ApiVersion,
  ElasticsearchWriteSettings,
  RetryWithBackoff,
  WriteMessage
}
import akka.stream.alpakka.elasticsearch.scaladsl.ElasticsearchSink
import akka.stream.alpakka.s3.scaladsl.S3
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import scala.concurrent.duration._

object ProcessCurrentFiles extends App {

  val bucket = "jsherz-logs"

  implicit val system: ActorSystem = ActorSystem("ProcessCurrentFiles")
  implicit val materializer: Materializer = Materializer.matFromSystem

  val downloader = new FileDownloader

  val source = S3.listBucket(bucket, None)

  import LogEntryJsonProtocol._

  implicit val esClient: RestClient =
    RestClient.builder(new HttpHost("vms", 9200)).build()

  val esSettings = ElasticsearchWriteSettings()
    .withApiVersion(ApiVersion.V7)
    .withBufferSize(100)
    .withRetryLogic(RetryWithBackoff(3, 100 millis, 5 seconds))

  source
    .mapAsync(10)(downloader.downloadAndGunzip)
    .via(new ParseLogFileFlow)
    // turn into ES message
    // bulk to ES
    .map(entry =>
      WriteMessage
        .createIndexMessage[LogEntry](LogEntryUtil.id(entry), entry)
        .withIndexName(LogEntryUtil.indexName(entry))
    )
    .log("blah")
    .runWith(
      ElasticsearchSink.create[LogEntry](
        "unused-index-name",
        typeName = "_doc",
        settings = esSettings
      )
    )

  // 1. write ES mappings for case class
  // 2. write JSON converter for ""
  // 3. wire up code above
  // 4. create Kibana visualisation
  // 5. clean up, CI, blog post

}
