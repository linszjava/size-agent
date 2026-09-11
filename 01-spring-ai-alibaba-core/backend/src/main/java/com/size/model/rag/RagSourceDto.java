package com.size.model.rag;

public record RagSourceDto(
        String documentId,
        String fileName,
        Integer pageNumber,
        Double score
) {
}
