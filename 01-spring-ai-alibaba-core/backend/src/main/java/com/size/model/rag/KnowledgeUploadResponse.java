package com.size.model.rag;

public record KnowledgeUploadResponse(
        String documentId,
        String fileName,
        int chunkCount,
        String status
) {
}
