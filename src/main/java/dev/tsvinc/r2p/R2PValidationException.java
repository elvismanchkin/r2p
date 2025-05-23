package dev.tsvinc.r2p;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

public class R2PValidationException extends AbstractThrowableProblem {

    public R2PValidationException(String message) {
        super(
                null,
                "Validation Error",
                Status.BAD_REQUEST,
                message
        );
    }
}