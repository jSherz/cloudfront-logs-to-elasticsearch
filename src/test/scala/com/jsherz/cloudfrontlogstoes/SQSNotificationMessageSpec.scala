package com.jsherz.cloudfrontlogstoes

import org.scalatest.funspec.AnyFunSpecLike

class SQSNotificationMessageSpec extends AnyFunSpecLike {

  val exampleEvent =
    """
      |{
      |  "Records": [
      |    {
      |      "eventVersion": "2.1",
      |      "eventSource": "aws:s3",
      |      "awsRegion": "eu-west-1",
      |      "eventTime": "2020-05-16T17:33:39.663Z",
      |      "eventName": "ObjectCreated:Put",
      |      "userIdentity": {
      |        "principalId": "AWS:AROASLZSKMR5WNLFCYRXL:prod.iad.dbs.datafeeds.aws.internal"
      |      },
      |      "requestParameters": {
      |        "sourceIPAddress": "72.21.217.31"
      |      },
      |      "responseElements": {
      |        "x-amz-request-id": "052D6C819E847EE4",
      |        "x-amz-id-2": "ABbML9NzfAJAJmWpemKEli2kpfuKtMtUlPu/4vfKV7PxzsUmcBcAPUvP2v3p3hGr69gHaOZGBS8ONZ90XHYTSHsO9zJ1QvDU"
      |      },
      |      "s3": {
      |        "s3SchemaVersion": "1.0",
      |        "configurationId": "NjQzM2JjZTAtNzI1Yi00MTA2LThiZjgtZDAyOWY2ZTRlY2Mw",
      |        "bucket": {
      |          "name": "jsherz-logs",
      |          "ownerIdentity": {
      |            "principalId": "A9AF7FDL1WNTI"
      |          },
      |          "arn": "arn:aws:s3:::jsherz-logs"
      |        },
      |        "object": {
      |          "key": "E1YR6EXJGW75CE.2020-05-16-17.5b6115f3.gz",
      |          "size": 558,
      |          "eTag": "d5bf7436ab2a12019962f4e83b5d2e39",
      |          "sequencer": "005EC023F5D54EF64A"
      |        }
      |      }
      |    }
      |  ]
      |}
      |""".stripMargin

  describe("SQSNotificationMessageJsonProtocol") {
    it("transforms the message to and from JSON") {
      import SQSNotificationMessageJsonProtocol._
      import spray.json._

      assert(
        exampleEvent.parseJson
          .convertTo[WrappedSQSNotificationMessage]
          == exampleEvent.parseJson
            .convertTo[WrappedSQSNotificationMessage]
            .toJson
            .toString
            .parseJson
            .convertTo[WrappedSQSNotificationMessage]
      )
    }
  }

}
