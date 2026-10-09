package ru.kovyazin.lab2.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.kovyazin.lab2.model.Response;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Response> invalidJson(
            HttpMessageNotReadableException exception) {
        Response response = Response.builder()
                .uid("").operationUid("")
                .systemTime(Instant.now().toString())
                .code("failed").errorCode("ValidationException")
                .errorMessage("Ошибка валидации: тело запроса отсутствует, "
                        + "повреждено или содержит неверный тип данных")
                .build();
        return ResponseEntity.badRequest().body(response);
    }
}
