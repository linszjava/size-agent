package com.size.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RagConfig {

    @Bean(destroyMethod = "close")
    public JedisPooled jedisPooled(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.username}") String username,
            @Value("${spring.data.redis.password}") String password
    ) {
        return new JedisPooled(host, port, username, password);
    }

    /* 为什么元数据字段必须在这里声明：

- Redis 会保存 `Document.metadata`，但“保存”不等于“可过滤检索”。
- `tenantId == 'xxx'` 这类 Filter 只能使用已进入 RediSearch Schema 的字段。
- ID、状态和权限适合 `tag`；页码和序号适合 `numeric`；文件名适合 `text`。
- 改变这些字段或 Embedding 维度后，应使用新的 `index-name` 和 `prefix` 重建向量。
 */
    @Bean
    public VectorStore vectorStore(
            JedisPooled jedisPooled,
            EmbeddingModel embeddingModel,
            @Value("${spring.ai.vectorstore.redis.index-name}") String indexName,
            @Value("${spring.ai.vectorstore.redis.prefix}") String prefix,
            @Value("${spring.ai.vectorstore.redis.initialize-schema}") boolean initializeSchema
    ) {
        return RedisVectorStore.builder(jedisPooled,embeddingModel)
                .indexName(indexName)
                .prefix(prefix)
                .metadataFields(
                        RedisVectorStore.MetadataField.tag("tenantId"),
                        RedisVectorStore.MetadataField.tag("departmentId"),
                        RedisVectorStore.MetadataField.tag("visibility"),
                        RedisVectorStore.MetadataField.tag("status"),
                        RedisVectorStore.MetadataField.tag("documentId"),
                        RedisVectorStore.MetadataField.text("fileName"),
                        RedisVectorStore.MetadataField.numeric("pageNumber"),
                        RedisVectorStore.MetadataField.numeric("chunkIndex")
                )
                .initializeSchema(initializeSchema)
                .build();

    }
}
