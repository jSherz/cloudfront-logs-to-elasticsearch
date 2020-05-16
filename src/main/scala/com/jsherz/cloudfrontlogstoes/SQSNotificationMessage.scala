package com.jsherz.cloudfrontlogstoes

import java.util.Date

import spray.json._

case class UserIdentity(principalId: String)

case class SQSNotificationMessage(
    eventVersion: String,
    eventSource: String,
    awsRegion: String,
    eventTime: Date,
    eventName: String,
    userIdentity: UserIdentity,
    requestParameters: Map[String, String],
    responseElements: Map[String, String],
    s3: S3Event
)

case class S3EventBucket(
    name: String,
    ownerIdentity: Map[String, String],
    arn: String
)

case class S3EventObject(
    key: String,
    size: Long,
    eTag: String,
    sequencer: String
)

case class S3Event(
    s3SchemaVersion: String,
    configurationId: String,
    bucket: S3EventBucket,
    `object`: S3EventObject
)

case class WrappedSQSNotificationMessage(
    records: List[SQSNotificationMessage]
)

object SQSNotificationMessageJsonProtocol extends DefaultJsonProtocol {

  import LogEntryJsonProtocol.DateJsonFormat

  implicit val userIdentitityJsonFormat: JsonFormat[UserIdentity] = jsonFormat1(
    UserIdentity
  )

  implicit val s3EventObjectJsonFormat: JsonFormat[S3EventObject] = jsonFormat4(
    S3EventObject
  )

  implicit val s3EventBucketJsonFormat: JsonFormat[S3EventBucket] = jsonFormat3(
    S3EventBucket
  )

  implicit val s3EventJsonFormat: JsonFormat[S3Event] = jsonFormat4(
    S3Event
  )

//  implicit val wrappedSQSNotificationMessageFormat
//      : JsonFormat[WrappedSQSNotificationMessage] = jsonFormat1(
//    WrappedSQSNotificationMessage
//  )

  implicit val sqsNotificationMesseageJsonFormat
      : JsonFormat[SQSNotificationMessage] = jsonFormat9(SQSNotificationMessage)

  /**
    * AWS only supports one item in the Records field and thus we only handle one item.
    */
  implicit object WrappedSQSNotificationMessageJsonFormat
      extends RootJsonFormat[WrappedSQSNotificationMessage] {
    override def read(json: JsValue): WrappedSQSNotificationMessage = {
      json.asJsObject.getFields(
        "Records"
      ) match {
        case Seq(
            records: JsValue
            ) =>
          WrappedSQSNotificationMessage(
            List(
              sqsNotificationMesseageJsonFormat
                .read(records.convertTo[JsArray].elements(0))
            )
          )
      }
    }

    override def write(obj: WrappedSQSNotificationMessage): JsValue =
      JsObject(
        "Records" -> JsArray(
          sqsNotificationMesseageJsonFormat.write(obj.records.head)
        )
      )
  }

}
