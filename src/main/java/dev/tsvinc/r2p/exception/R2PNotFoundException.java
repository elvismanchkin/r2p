package dev.tsvinc.r2p.exception;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

public class R2PNotFoundException extends AbstractThrowableProblem {

    public R2PNotFoundException(String message) {
        super(null, "Not Found", Status.NOT_FOUND, message);
    }
}
