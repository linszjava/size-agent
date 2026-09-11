package com.size.model.rag;

import java.util.List;

public record RagAnswerResponse(
        String answer,
        List<RagSourceDto> sources
) {
}
