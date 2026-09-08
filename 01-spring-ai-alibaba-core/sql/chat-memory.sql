CREATE DATABASE size_ai
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'size_ai'@'localhost' IDENTIFIED BY '请替换为强密码';
GRANT ALL PRIVILEGES ON size_ai.* TO 'size_ai'@'localhost';
FLUSH PRIVILEGES;


/* docker 中的mysql 可以这么操作*/
CREATE USER IF NOT EXISTS 'size_ai'@'%' IDENTIFIED BY '你的实际密码';
GRANT ALL PRIVILEGES ON size_ai.* TO 'size_ai'@'%';
FLUSH PRIVILEGES;


      /*启动项目报错是因为我们用的是 生成结果固定为 64 个字符 默认 Spring AI 默认表字段是36 */
      /* 所以执行当下这句sql即可 */
USE size_ai;

ALTER TABLE SPRING_AI_CHAT_MEMORY
    MODIFY COLUMN conversation_id VARCHAR(64) NOT NULL;