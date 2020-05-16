# cloudfront-logs-to-elasticsearch

This project accompanies the [blog post on my website]. It's used to
ship logs from a CloudFront distribution and put them into Elasticsearch
for analysis with a tool like Kibana or Grafana.

[blog post on my website]:

## Setting up the index pattern

Create an ingestion pipeline to lookup the Geo-IP information of the
requester's IP.

```
PUT /_ingest/pipeline/cloudfront-logs
{
  "description" : "Add geoip info",
  "processors" : [
    {
      "geoip" : {
        "field" : "request.ip"
      }
    }
  ]
}
```

Setup the index template using the "index-template.json" file in this
repo.

```
PUT /_template/cloudfront-logs
{ ... }
```

## Building the project

The following command will build a shaded jar that can be shipped to the
server running this daemon. The tests aren't set to run as they're
currently using a hardcoded bucket name for integration testing with S3.

```bash
sbt 'set test in assembly := {}' clean assembly
```

Creates:

```
target/scala-2.13/cloudfront-logs-to-elasticsaerch-assembly-0.1.jar
```

## Importing historic data

If you've got an S3 bucket with the data already in it, start by
loading in that data:

```bash
export LOGS_BUCKET=jsherz-logs
export ELASTICSEARCH_HOST=http://vms:9200
java -cp target/scala-2.13/cloudfront-logs-to-elasticsaerch-assembly-0.1.jar com.jsherz.cloudfrontlogstoes.ProcessCurrentFiles
```

## Running as a daemon

Once the initial upload has been completed, create an event notification
on your S3 bucket for "All object create events" that sends the message
to an SQS queue. Run the app as follows to read from that SQS queue and
process the notifications to parse the logs:

```bash
export ELASTICSEARCH_HOST=http://vms:9200
export NOTIFICATION_QUEUE_URL=https://sqs.eu-west-1.amazonaws.com/123456789012/jsherz-logs
java -jar target/scala-2.13/cloudfront-logs-to-elasticsaerch-assembly-0.1.jar
```

### See also

- [S3 docs on event notifications](https://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html)
