package ru.kovyazin.lab2.service;

import org.springframework.validation.BindingResult;
import ru.kovyazin.lab2.exception.ValidationFailedException;

public interface ValidationService {
    void isValid(BindingResult result) throws ValidationFailedException;
}
