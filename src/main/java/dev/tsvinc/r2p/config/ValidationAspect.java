package dev.tsvinc.r2p.config;

import dev.tsvinc.r2p.api.dto.request.InitiateR2pRequest;
import dev.tsvinc.r2p.service.validation.R2PBusinessValidationService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ValidationAspect {

    private final R2PBusinessValidationService validationService;

    @Before("@annotation(org.springframework.web.bind.annotation.PostMapping) && args(request,..)")
    public void validateInitiateRequest(InitiateR2pRequest request) {
        if (request != null) {
            validationService.validateInitiateRequest(request);
        }
    }
}