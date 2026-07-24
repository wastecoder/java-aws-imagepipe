#!/bin/bash
# Runs inside the LocalStack container once the gateway is ready.
#
# Two buckets, never one: a single bucket would let the worker's own writes retrigger it, which is
# recursive invocation. See PROJECT.md section 5.
set -euo pipefail

awslocal s3api create-bucket --bucket imagepipe-raw
awslocal s3api create-bucket --bucket imagepipe-derived

# Local shortcut, and local only. On AWS the derived bucket is private and readable solely by
# CloudFront through Origin Access Control (phase 7). This exists so the CDN URL returned by
# GET /images/{id} can be opened by hand once the worker starts writing variants in phase 2.
awslocal s3api put-bucket-policy --bucket imagepipe-derived --policy '{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "LocalPublicRead",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::imagepipe-derived/*"
    }
  ]
}'
