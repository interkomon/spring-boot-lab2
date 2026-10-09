package ru.kovyazin.lab2.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.kovyazin.lab2.exception.UnsupportedCodeException;
import ru.kovyazin.lab2.exception.ValidationFailedException;
import ru.kovyazin.lab2.model.Request;
import ru.kovyazin.lab2.model.Response;
import ru.kovyazin.lab2.service.ValidationService;

import java.time.Instant;

@RestController
public class MyController {
    private static final Logger LOG =
            LoggerFactory.getLogger(MyController.class);
    private final ValidationService validationService;

    public MyController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping(value = "/feedback",
            consumes = "application/json", produces = "application/json")
    public ResponseEntity<Response> feedback(
            @Valid @RequestBody Request request, BindingResult result) {
        Response response = Response.builder()
                .uid(request.getUid() == null ? "" : request.getUid())
                .operationUid(request.getOperationUid() == null
                        ? "" : request.getOperationUid())
                .systemTime(Instant.now().toString())
                .code("success").errorCode("").errorMessage("").build();
        try {
            validationService.isValid(result);
            if ("123".equals(request.getUid())) {
                throw new UnsupportedCodeException(
                        "Не поддерживаемый код uid: 123");
            }
            return ResponseEntity.ok(response);
        } catch (ValidationFailedException e) {
            response.setCode("failed");
            response.setErrorCode("ValidationException");
            response.setErrorMessage("Ошибка валидации: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (UnsupportedCodeException e) {
            response.setCode("failed");
            response.setErrorCode("UnsupportedCodeException");
            response.setErrorMessage(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            LOG.error("Непредвиденная ошибка обработки /feedback", e);
            response.setCode("failed");
            response.setErrorCode("UnknownException");
            response.setErrorMessage("Произошла непредвиденная ошибка");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
