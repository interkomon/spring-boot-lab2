package ru.kovyazin.lab2.service;

import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import ru.kovyazin.lab2.exception.ValidationFailedException;

import java.util.stream.Collectors;

@Service
public class RequestValidationService implements ValidationService {
    @Override
    public void isValid(BindingResult result) throws ValidationFailedException {
        if (result.hasErrors()) {
            String details = result.getFieldErrors().stream()
                    .map(error -> error.getField() + ": "
                            + error.getDefaultMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new ValidationFailedException(details);
        }
    }
}
