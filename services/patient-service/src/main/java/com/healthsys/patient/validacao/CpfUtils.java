package com.healthsys.patient.validacao;

/** Regras de CPF: normalização (só dígitos) e conferência dos dois dígitos verificadores. */
public final class CpfUtils {

    private CpfUtils() {
    }

    public static String somenteDigitos(String valor) {
        return valor == null ? null : valor.replaceAll("\\D", "");
    }

    public static boolean valido(String valor) {
        String cpf = somenteDigitos(valor);
        if (cpf == null || cpf.length() != 11) {
            return false;
        }
        // 000.000.000-00, 111.111.111-11, ... passam no cálculo, mas não são CPFs válidos.
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }
        return digitoVerificador(cpf, 9) == cpf.charAt(9) - '0'
                && digitoVerificador(cpf, 10) == cpf.charAt(10) - '0';
    }

    /** Calcula o dígito na posição "tamanho" a partir dos "tamanho" dígitos anteriores (pesos decrescentes até 2). */
    private static int digitoVerificador(String cpf, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
