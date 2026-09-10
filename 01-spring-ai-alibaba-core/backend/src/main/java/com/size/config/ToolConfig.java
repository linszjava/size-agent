package com.size.config;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Function;

@Configuration
public class ToolConfig {

    @Bean("currentTimeTool")
    public ToolCallback currentTimeTool() {

        /* Function<T, R> 表示： 接收一个 T 类型参数  返回一个 R 类型结果*/
        Function<CurrentTimeRequest, CurrentTimeResponse> function = request -> {
            ZoneId zoneId = ZoneId.of(request.zoneId());
            return new CurrentTimeResponse(
                    zoneId.getId(),
                    ZonedDateTime.now(zoneId).toString()
            );
        };

        return FunctionToolCallback
                .builder("getCurrentTime", function)
                .description("查询指定时区的当前日期和时间，时区必须使用 Asia/Shanghai 等 ZoneId 格式")
                .inputType(CurrentTimeRequest.class)
                .build();
    }



    public record CurrentTimeRequest(String zoneId) {
    }

    public record CurrentTimeResponse(String zoneId, String dateTime) {
    }
}
