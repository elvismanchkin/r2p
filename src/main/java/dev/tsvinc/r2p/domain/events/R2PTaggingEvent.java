package dev.tsvinc.r2p.domain.events;

import dev.tsvinc.r2p.api.dto.request.TransactionTaggingRequest;
import dev.tsvinc.r2p.domain.entity.R2PTransaction;

public record R2PTaggingEvent(R2PTransaction transaction, TransactionTaggingRequest request) {}
