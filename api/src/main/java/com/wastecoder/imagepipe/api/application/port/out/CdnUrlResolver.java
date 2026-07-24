package com.wastecoder.imagepipe.api.application.port.out;

import com.wastecoder.imagepipe.shared.VariantSize;
import java.net.URI;
import java.util.UUID;

/** Driven port: the public URL a variant is served from. */
public interface CdnUrlResolver {

    URI resolve(UUID uploadId, VariantSize size);
}
