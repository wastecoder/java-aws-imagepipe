package com.wastecoder.imagepipe.api.adapter.web.handler;

import com.wastecoder.imagepipe.api.domain.exception.ImageNotFoundException;
import com.wastecoder.imagepipe.api.domain.exception.ImageTooLargeException;
import com.wastecoder.imagepipe.api.domain.exception.UnsupportedContentTypeException;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps HTTP-agnostic domain exceptions onto RFC 9457 Problem Details.
 *
 * <p>Extending ResponseEntityExceptionHandler makes Boot's auto-configured handler back off, since
 * it is conditional on that type being absent. That is why {@code spring.mvc.problemdetails.enabled}
 * is not set anywhere: with this bean present it would do nothing.
 *
 * <p>What this does NOT cover: anything that never reaches the DispatcherServlet's exception chain —
 * container-level 404 and 405, filter failures, /error forwards. Those are still rendered in Boot's
 * legacy shape. Do not assume problem+json for every non-2xx.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ImageNotFoundException.class)
    public ProblemDetail handleImageNotFound(ImageNotFoundException exception) {
        ProblemDetail problem = problem(HttpStatus.NOT_FOUND, ProblemType.IMAGE_NOT_FOUND, exception.getMessage());
        problem.setProperty("imageId", exception.imageId().toString());
        return problem;
    }

    /**
     * 422 rather than 415: the request's own media type is fine, it is a field value that is not
     * acceptable. 422 is also what an oversized upload returns — the two are told apart by their
     * type URI, which is exactly what Problem Details is for.
     */
    @ExceptionHandler(UnsupportedContentTypeException.class)
    public ProblemDetail handleUnsupportedContentType(UnsupportedContentTypeException exception) {
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_CONTENT,
                ProblemType.UNSUPPORTED_CONTENT_TYPE, exception.getMessage());
        problem.setProperty("contentType", exception.contentType());
        problem.setProperty("allowedContentTypes", exception.allowedContentTypes());
        return problem;
    }

    /**
     * Also 422, not 413: no oversized payload was ever sent. The client declared a size the API
     * refuses to sign for, and the body of this very request is a few dozen bytes.
     */
    @ExceptionHandler(ImageTooLargeException.class)
    public ProblemDetail handleImageTooLarge(ImageTooLargeException exception) {
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_CONTENT,
                ProblemType.IMAGE_TOO_LARGE, exception.getMessage());
        problem.setProperty("sizeBytes", exception.sizeBytes());
        problem.setProperty("maxSizeBytes", exception.maxSizeBytes());
        return problem;
    }

    /**
     * Bean validation on the request body. Going through super preserves the MessageSource-based
     * resolution of type, title and detail that building a ProblemDetail from scratch would throw
     * away; the default body simply carries no field errors, which is what this adds.
     *
     * <p>There is a second validation exception, HandlerMethodValidationException, raised by
     * constraints on @RequestParam and @PathVariable. This API has none, so overriding it would be
     * untestable dead code. Add the override the day a constrained parameter appears.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        ResponseEntity<Object> response = super.handleMethodArgumentNotValid(exception, headers, status, request);

        // Every hook on ResponseEntityExceptionHandler is nullable in Framework 7: it returns null
        // when the response has already been committed.
        if (response == null || !(response.getBody() instanceof ProblemDetail body)) {
            return response;
        }

        body.setType(ProblemType.INVALID_PAYLOAD.uri());
        body.setTitle(ProblemType.INVALID_PAYLOAD.title());
        body.setProperty("violations", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", Objects.requireNonNullElse(error.getDefaultMessage(), "invalid")))
                .toList());
        return response;
    }

    private static ProblemDetail problem(HttpStatus status, ProblemType type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type.uri());
        problem.setTitle(type.title());
        return problem;
    }
}
