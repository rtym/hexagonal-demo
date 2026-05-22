#!/bin/bash
# Runs automatically when LocalStack is ready (mounted as an init script).
set -e

export AWS_ACCESS_KEY_ID=localstack
export AWS_SECRET_ACCESS_KEY=localstack
export AWS_DEFAULT_REGION=us-east-1
ENDPOINT=http://localhost:4566

echo "==> Creating SQS queue: messages-queue"
aws --endpoint-url=$ENDPOINT sqs create-queue \
    --queue-name messages-queue

echo "==> Creating DynamoDB table: processed-messages"
aws --endpoint-url=$ENDPOINT dynamodb create-table \
    --table-name processed-messages \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST

echo "==> Creating S3 bucket: hexdemo-frontend"
aws --endpoint-url=$ENDPOINT s3 mb s3://hexdemo-frontend

echo "==> Configuring S3 static website"
aws --endpoint-url=$ENDPOINT s3 website s3://hexdemo-frontend \
    --index-document index.html \
    --error-document index.html

echo "==> Setting public-read bucket policy"
aws --endpoint-url=$ENDPOINT s3api put-bucket-policy \
    --bucket hexdemo-frontend \
    --policy '{
        "Version":"2012-10-17",
        "Statement":[{
            "Effect":"Allow",
            "Principal":"*",
            "Action":"s3:GetObject",
            "Resource":"arn:aws:s3:::hexdemo-frontend/*"
        }]
    }'

echo "==> Uploading frontend to S3"
aws --endpoint-url=$ENDPOINT s3 cp /etc/localstack/init/ready.d/index.html \
    s3://hexdemo-frontend/index.html \
    --content-type "text/html" 2>/dev/null || \
    echo "    (frontend/index.html not found in init dir — upload manually)"

echo "==> Seeding SQS with sample messages"
for msg in carrot rabbit cabbage carrot rabbit; do
  aws --endpoint-url=$ENDPOINT sqs send-message \
      --queue-url http://localhost:4566/000000000000/messages-queue \
      --message-body "$msg"
  echo "    Sent: $msg"
done

echo "==> LocalStack init complete."
