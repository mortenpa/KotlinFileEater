package com.hrblizz.fileapi.library

import com.hrblizz.fileapi.controller.exception.BadRequestException
import com.hrblizz.fileapi.library.log.ExceptionLogItem
import com.hrblizz.fileapi.library.log.LogItem
import com.hrblizz.fileapi.library.log.Logger
import com.hrblizz.fileapi.rest.ErrorMessage
import com.mongodb.MongoSocketException
import com.mongodb.MongoTimeoutException
import org.springframework.beans.TypeMismatchException
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.BindException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.ConnectException
import java.util.ArrayList

@ControllerAdvice
class RestExceptionHandler(
    private val log: Logger
) : ResponseEntityExceptionHandler() {

    override fun handleExceptionInternal(
        e: Exception,
        body: Any?,
        headers: HttpHeaders?,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        if (status.is5xxServerError) {
            log.crit(ExceptionLogItem("Internal exception: " + e.message, e))
        }

        if (body != null && body is com.hrblizz.fileapi.rest.ResponseEntity<*>) {
            return ResponseEntity(body, headers, status)
        }

        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            status,
            listOf(ErrorMessage(status.reasonPhrase))
        )
        return ResponseEntity(apiError, headers, status)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        val errors = ArrayList<ErrorMessage>()
        for (error in ex.bindingResult.fieldErrors) {
            errors.add(ErrorMessage("${error.field}: ${error.defaultMessage}"))
        }
        for (error in ex.bindingResult.globalErrors) {
            errors.add(ErrorMessage("${error.objectName}: ${error.defaultMessage}"))
        }

        val errorStatus = HttpStatus.BAD_REQUEST
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(errorStatus, errors)

        return handleExceptionInternal(ex, apiError, headers, errorStatus, request)
    }

    override fun handleBindException(
        ex: BindException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        val errors = ArrayList<ErrorMessage>()
        for (error in ex.bindingResult.fieldErrors) {
            errors.add(ErrorMessage("${error.field}: ${error.defaultMessage}"))
        }
        for (error in ex.bindingResult.globalErrors) {
            errors.add(ErrorMessage("${error.objectName}: ${error.defaultMessage}"))
        }

        val errorStatus = HttpStatus.BAD_REQUEST
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(errorStatus, errors)

        return handleExceptionInternal(ex, apiError, headers, errorStatus, request)
    }

    override fun handleTypeMismatch(
        ex: TypeMismatchException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        val errorStatus = HttpStatus.BAD_REQUEST
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            errorStatus,
            listOf(ErrorMessage("${ex.value} value for ${ex.propertyName} should be of type ${ex.requiredType}")),
        )
        return handleExceptionInternal(ex, apiError, headers, errorStatus, request)
    }

    @ExceptionHandler(MultipartException::class)
    fun handleMultipartException(ex: MultipartException, request: WebRequest): ResponseEntity<Any> {
        val errorStatus = HttpStatus.BAD_REQUEST
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            errorStatus,
            listOf(ErrorMessage("Invalid multipart request: ${ex.message}")),
        )
        return handleExceptionInternal(ex, apiError, null, errorStatus, request)
    }


    override fun handleMissingServletRequestPart(
        ex: MissingServletRequestPartException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        val errorStatus = HttpStatus.BAD_REQUEST
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            errorStatus,
            listOf(ErrorMessage("${ex.requestPartName} part is missing")),
        )
        return handleExceptionInternal(ex, apiError, headers, errorStatus, request)
    }

    override fun handleMissingServletRequestParameter(
        ex: MissingServletRequestParameterException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        val errorStatus = HttpStatus.BAD_REQUEST
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            errorStatus,
            listOf(ErrorMessage("${ex.parameterName} parameter is missing")),
        )
        return handleExceptionInternal(ex, apiError, headers, errorStatus, request)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(ex: MethodArgumentTypeMismatchException, request: WebRequest): ResponseEntity<Any> {
        val errorStatus = HttpStatus.BAD_REQUEST
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            errorStatus,
            listOf(ErrorMessage("${ex.name} should be of type ${ex.requiredType?.name}")),
        )
        return ResponseEntity(apiError, HttpHeaders(), errorStatus)
    }

    override fun handleNoHandlerFoundException(ex: NoHandlerFoundException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        val errorStatus = HttpStatus.NOT_FOUND
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            errorStatus,
            listOf(ErrorMessage(errorStatus.reasonPhrase)),
        )
        return ResponseEntity(apiError, HttpHeaders(), errorStatus)
    }

    override fun handleHttpRequestMethodNotSupported(ex: HttpRequestMethodNotSupportedException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        val errorStatus = HttpStatus.METHOD_NOT_ALLOWED
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            errorStatus,
            listOf(ErrorMessage(errorStatus.reasonPhrase)),
        )
        return ResponseEntity(apiError, HttpHeaders(), errorStatus)
    }

    override fun handleHttpMediaTypeNotSupported(ex: HttpMediaTypeNotSupportedException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        val errorStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            errorStatus,
            listOf(ErrorMessage(errorStatus.reasonPhrase)),
        )
        return ResponseEntity(apiError, HttpHeaders(), errorStatus)
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(ex: BadRequestException): ResponseEntity<Any> {
        var status: HttpStatus? = getResponseStatus(ex.javaClass)
        if (status == null) {
            status = HttpStatus.BAD_REQUEST
        }

        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            status,
            listOf(ErrorMessage(ex.message)),
        )
        return ResponseEntity(apiError, HttpHeaders(), status)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<Any> {
        val errorStatus = HttpStatus.UNAUTHORIZED
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            errorStatus,
            listOf(ErrorMessage(ex.message)),
        )
        return ResponseEntity(apiError, HttpHeaders(), errorStatus)
    }

    @ExceptionHandler(MongoTimeoutException::class)
    fun handleDatabaseTimedOut(ex: MongoTimeoutException): ResponseEntity<Any> {
        val errorStatus = HttpStatus.INTERNAL_SERVER_ERROR
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            errorStatus,
            listOf(ErrorMessage(ex.message)),
        )
        log.crit(LogItem("Database connection timed out"))
        return ResponseEntity(apiError, HttpHeaders(), errorStatus)
    }

    @ExceptionHandler(NoSuchFileException::class)
    fun handleNoFileError(ex: NoSuchFileException): ResponseEntity<Any> {
        val errorStatus = HttpStatus.INTERNAL_SERVER_ERROR
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            errorStatus,
            listOf(ErrorMessage(ex.message)),
        )
        log.crit(LogItem("Target file wasn't found"))
        return ResponseEntity(apiError, HttpHeaders(), errorStatus)
    }

    @ExceptionHandler(MongoSocketException::class)
    fun handleDatabaseSocketError(ex: MongoSocketException): ResponseEntity<Any> {
        val errorStatus = HttpStatus.INTERNAL_SERVER_ERROR
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            errorStatus,
            listOf(ErrorMessage(ex.message)),
        )
        return ResponseEntity(apiError, HttpHeaders(), errorStatus)
    }

    @ExceptionHandler(DataAccessResourceFailureException::class)
    fun handleDataAccessFailureError(ex: DataAccessResourceFailureException): ResponseEntity<Any> {
        val errorStatus = HttpStatus.INTERNAL_SERVER_ERROR
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            errorStatus,
            listOf(ErrorMessage("Timed out after refusing to connect with database")),
        )
        log.crit(LogItem("Timed out after refusing to connect with database"))
        return ResponseEntity(apiError, HttpHeaders(), errorStatus)
    }


    @ExceptionHandler(ConnectException::class)
    fun handleConnectError(ex: ConnectException): ResponseEntity<Any> {
        val errorStatus = HttpStatus.INTERNAL_SERVER_ERROR
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            errorStatus,
            listOf(ErrorMessage(ex.message)),
        )
        return ResponseEntity(apiError, HttpHeaders(), errorStatus)
    }

    @ExceptionHandler(Exception::class)
    fun handleAll(ex: Exception, request: WebRequest): ResponseEntity<Any> {
        this.log.error(ExceptionLogItem("Unhandled exception: ${ex.localizedMessage}", ex))

        val errorStatus = HttpStatus.INTERNAL_SERVER_ERROR
        val apiError = com.hrblizz.fileapi.rest.ResponseEntity<Any>(
            errorStatus,
            listOf(ErrorMessage("Unknown error occurred")),
        )
        return ResponseEntity(apiError, HttpHeaders(), errorStatus)
    }

    private fun <T> getResponseStatus(ex: Class<T>?): HttpStatus? {
        if (ex == null) {
            return null
        }

        val responseStatus = ex.getAnnotation(ResponseStatus::class.java)
        if (responseStatus != null) {
            return responseStatus.value
        }
        return getResponseStatus(ex.superclass)
    }
}
