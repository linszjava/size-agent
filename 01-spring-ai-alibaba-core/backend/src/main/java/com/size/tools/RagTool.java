package com.size.tools;

import cn.hutool.core.io.FileUtil;
import com.size.exception.KnowledgeBaseException;
import com.size.model.rag.KnowledgeVisibility;
import com.size.model.rag.RagSourceDto;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RagTool {

    /*  ========================upload   start  =======================================*/
    public static Document withSecurityMetadata(
            Document document,
            String documentId,
            String fileName,
            String tenantId,
            String departmentId,
            KnowledgeVisibility visibility) {
        Map<String, Object> metadata = new LinkedHashMap<>(document.getMetadata());
        metadata.put("documentId", documentId);
        metadata.put("fileName", fileName);
        metadata.put("tenantId", tenantId);
        metadata.put("departmentId", departmentId);
        metadata.put("visibility", visibility.name());
        metadata.put("status", "ACTIVE");
        return new Document(document.getText(), metadata);
    }

    public static ByteArrayResource namedResource(byte[] bytes, String fileName) {
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
    }

    public static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new KnowledgeBaseException("上传文件不能为空");
        }
        String fileName = FileUtil.getName(file.getOriginalFilename());
        String extension = FileUtil.extName(fileName).toLowerCase();
        if (!List.of("pdf", "doc", "docx", "txt", "md").contains(extension)) {
            throw new KnowledgeBaseException(
                    "仅支持 PDF、Word、TXT 和 Markdown 文件"
            );
        }
    }

    public static boolean isPdf(String fileName) {
        return "pdf".equalsIgnoreCase(FileUtil.extName(fileName));
    }



    /*=================ask start ==============================================*/
    public static Filter.Expression buildPermissionFilter(String tenantId, String departmentId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        // Spring AI 1.1.2 inserts Redis TAG values verbatim. Escape only the query,
        // keeping stored metadata and the secondary permission check unchanged.
        return builder.and(
                builder.and(builder.eq("tenantId", redisTagId(tenantId)),
                        builder.eq("status", "ACTIVE")),
                builder.group(builder.or(builder.eq("visibility", "PUBLIC"),
                        builder.eq("departmentId", redisTagId(departmentId))))
        ).build();
    }

    private static String redisTagId(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("租户和部门 ID 格式不合法");
        }
        return value.replace("-", "\\-");
    }

    public static boolean canRead(
            Document document,
            String tenantId,
            String departmentId) {
        Map<String, Object> metadata = document.getMetadata();
        if (!tenantId.equals(metadata.get("tenantId"))
                || !"ACTIVE".equals(metadata.get("status"))) {
            return false;
        }
        return "PUBLIC".equals(metadata.get("visibility"))
                || departmentId.equals(metadata.get("departmentId"));
    }

    public static String buildContext(List<Document> documents) {
        StringBuilder context = new StringBuilder();
        for (int index = 0; index < documents.size(); index++) {
            Document document = documents.get(index);
            context.append("[来源").append(index + 1).append("] ")
                    .append("文件：")
                    .append(document.getMetadata().get("fileName"));

            Object page = document.getMetadata().get(
                    PagePdfDocumentReader.METADATA_START_PAGE_NUMBER
            );
            if (page != null) {
                context.append("，页码：").append(page);
            }

            context.append('\n')
                    .append(document.getText())
                    .append("\n\n");
        }
        return context.toString();
    }

    public static List<RagSourceDto> toSources(List<Document> documents) {
        return documents.stream()
                .map(document -> new RagSourceDto(
                        document.getMetadata().get("documentId").toString(),
                        document.getMetadata().get("fileName").toString(),
                        toInteger(document.getMetadata().get(
                                PagePdfDocumentReader.METADATA_START_PAGE_NUMBER
                        )),
                        document.getScore()
                ))
                .distinct()
                .toList();
    }

    public static Integer toInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

}
