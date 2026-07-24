package com.wastecoder.imagepipe.api.application.viewmodel;

/** What the caller declares up front. The size has to be exact: it travels into the signature. */
public record RequestUploadCommand(String contentType, long sizeBytes, String filename) {}
