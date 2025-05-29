package dev.tsvinc.r2p.exception;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

public class R2PBusinessException extends AbstractThrowableProblem {

    public R2PBusinessException(String message) {
        super(null, "Business Rule Violation", Status.BAD_REQUEST, message);
    }
}