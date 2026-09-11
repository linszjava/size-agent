package com.size.tools;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.redis.RedisFilterExpressionConverter;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RagToolTest {
    @Test
    void escapesHyphensInRedisQueryWithoutChangingPermissionMetadata() {
        var converter = new RedisFilterExpressionConverter(List.of(
                RedisVectorStore.MetadataField.tag("tenantId"),
                RedisVectorStore.MetadataField.tag("departmentId"),
                RedisVectorStore.MetadataField.tag("status"),
                RedisVectorStore.MetadataField.tag("visibility")));
        String query = converter.convertExpression(RagTool.buildPermissionFilter("tenant-001", "department-001"));
        assertEquals("@tenantId:{tenant\\-001} @status:{ACTIVE} (@visibility:{PUBLIC} | @departmentId:{department\\-001})", query);
        assertTrue(query.contains("@tenantId:{tenant\\-001}"));
        assertTrue(query.contains("@departmentId:{department\\-001}"));
        var doc = new Document("制度", Map.of("tenantId", "tenant-001", "departmentId", "department-001",
                "visibility", "DEPARTMENT", "status", "ACTIVE"));
        assertTrue(RagTool.canRead(doc, "tenant-001", "department-001"));
        assertFalse(RagTool.canRead(doc, "other", "department-001"));
        assertFalse(RagTool.canRead(doc, "tenant-001", "other"));
    }

    @Test
    void rejectsQuerySyntaxInIds() {
        assertThrows(IllegalArgumentException.class, () -> RagTool.buildPermissionFilter("tenant}|*", "dept"));
    }
}
