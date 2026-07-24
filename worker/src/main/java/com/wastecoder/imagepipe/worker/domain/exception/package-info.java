/**
 * Domain exceptions. TransientProcessingException and PermanentProcessingException are domain concepts, not infrastructure details: the difference between retrying and giving up is a business decision.
 */
package com.wastecoder.imagepipe.worker.domain.exception;
