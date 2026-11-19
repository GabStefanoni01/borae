package com.borae.security;

/**
 * Exceção lançada quando uma política de rate limiting ou bloqueio temporário
 * de autenticação é excedida na camada de serviço.
 */
public class RateLimitExceededException extends RuntimeException {

    private final int limit;
    private final int retryAfterSeconds;
    private final String ruleName;

    public RateLimitExceededException(int limit, int retryAfterSeconds, String ruleName, String message) {
        super(message);
        this.limit = limit;
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
        this.ruleName = ruleName;
    }

    public int getLimit() {
        return limit;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String getRuleName() {
        return ruleName;
    }
}
