/**
 * Driving adapter: the SQS handler. This is the ONLY place that translates TransientProcessingException into returning a message to the queue and PermanentProcessingException into acknowledging it. The S3 event record is a wire DTO and lives here.
 */
package com.wastecoder.imagepipe.worker.adapter.lambda;
