package com.healthsys.patient.exception;

public class CpfJaCadastradoException extends RuntimeException {
    public CpfJaCadastradoException() {
        super("Já existe um paciente cadastrado com este CPF");
    }
}
