package com.jsherz.cloudfrontlogstoes

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.elasticsearch.scaladsl.ElasticsearchFlow
import akka.stream.alpakka.elasticsearch.{
  ApiVersion,
  ElasticsearchWriteSettings,
  RetryWithBackoff,
  WriteMessage
}
import akka.stream.alpakka.sqs.javadsl.SqsAckFlow
import akka.stream.alpakka.sqs.scaladsl.SqsSource
import akka.stream.alpakka.sqs.{
  MessageAction,
  SqsAckSettings,
  SqsSourceSettings
}
import akka.stream.scaladsl.Sink
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import scala.concurrent.duration._

object LogUploadNotificationProcessor extends App {

  private implicit val system: ActorSystem = ActorSystem("ProcessCurrentFiles")
  private implicit val materializer: Materializer = Materializer.matFromSystem

  private val downloader = new FileDownloader

  private implicit val awsSqsClient: SqsAsyncClient = SqsAsyncClient
    .builder()
    .build()

  system.registerOnTermination(awsSqsClient.close())

  private val queueUrl = sys.env("NOTIFICATION_QUEUE_URL")

  private val source = SqsSource(
    queueUrl,
    SqsSourceSettings().withCloseOnEmptyReceive(false).withWaitTime(20 seconds)
  )

  import LogEntryJsonProtocol._

  private implicit val esClient: RestClient =
    RestClient
      .builder(HttpHost.create(sys.env("ELASTICSEARCH_HOST")))
      .build()

  private val esSettings = ElasticsearchWriteSettings()
    .withApiVersion(ApiVersion.V7)
    .withBufferSize(100) // scalastyle:ignore magic.number
    .withRetryLogic(RetryWithBackoff(3, 100 millis, 5 seconds))

  /*
    We immediately delete the message here even if further processing fails. An
    improvement would be to only acknowledge the message in S3 when each of the
    log entries contained within it had been uploaded to Elasticsearch.
   */
  source
    .map(MessageAction.delete)
    .via(SqsAckFlow.create(queueUrl, SqsAckSettings.create(), awsSqsClient))
    .mapAsync(1)(res => {
      downloader.downloadAndGunzipSingle(res.messageAction.message)
    })
    .via(new ParseLogFileFlow)
    .map(entry =>
      WriteMessage
        .createIndexMessage[LogEntry](LogEntryUtil.id(entry), entry)
        .withIndexName(LogEntryUtil.indexName(entry))
    )
    .via(
      ElasticsearchFlow.create[LogEntry](
        "unused-index-name",
        typeName = "_doc",
        settings = esSettings
      )
    )
    .map(res =>
      res.error
        .foreach(error => system.log.error(s"Failed to index document: $error"))
    )
    .runWith(Sink.ignore)

  system.log.info("Started listening for SQS notifications...")

}
