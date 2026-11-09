package com.healthsys.patient.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Erros no formato RFC 7807 (Problem Details), igual ao auth-service. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CpfJaCadastradoException.class)
    public ProblemDetail cpfDuplicado(CpfJaCadastradoException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setProperty("erros", Map.of("cpf", ex.getMessage()));
        return pd;
    }

    /**
     * RN54: edição feita sobre uma versão antiga do paciente. Vale tanto para a checagem do service
     * quanto para o caso de duas gravações simultâneas, em que o próprio banco recusa a segunda.
     */
    @ExceptionHandler({ConflitoEdicaoException.class, ObjectOptimisticLockingFailureException.class})
    public ProblemDetail conflitoDeEdicao(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ConflitoEdicaoException.MENSAGEM);
        pd.setProperty("erros", Map.of("versao", ConflitoEdicaoException.MENSAGEM));
        return pd;
    }

    /** Rede de segurança: dois cadastros simultâneos com o mesmo CPF batem no índice UNIQUE do banco. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail violacaoIntegridade(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Dados conflitam com um registro existente");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> erros.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos");
        pd.setProperty("erros", erros);
        return pd;
    }

    /** JSON malformado, sexo fora de M/F/O ou data fora do formato AAAA-MM-DD. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail corpoInvalido(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido. Verifique o JSON, se o sexo é M, F ou O e se a data está no formato AAAA-MM-DD.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Valor inválido para o parâmetro '" + ex.getName() + "'");
    }
}