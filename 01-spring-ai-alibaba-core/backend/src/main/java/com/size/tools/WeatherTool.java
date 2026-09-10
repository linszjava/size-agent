package com.size.tools;


import cn.hutool.core.util.StrUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WeatherTool {

    /* 真实天气项目需要在工具内部通过受控的 HTTP Client 调用天气服务，
    并设置连接超时、读取超时、重试上限和熔断。不要声称演示 Map 是实时天气。*/

    private static final Map<String, WeatherResult> DEMO_WEATHER = Map.of(
            "杭州", new WeatherResult("杭州", "小雨", 26),
            "上海", new WeatherResult("上海", "多云", 28),
            "北京", new WeatherResult("北京", "晴", 24)
    );

    @Tool(description = "查询指定城市的演示天气数据。本学习项目未连接真实天气服务，结果只能用于验证 Tool Calling")
    public WeatherResult getWeather(
            @ToolParam(description = "中国城市名称，例如杭州、上海或北京") String city) {
        String normalizedCity = StrUtil.removeSuffix(StrUtil.trim(city), "市");
        return DEMO_WEATHER.getOrDefault(
                normalizedCity,
                new WeatherResult(normalizedCity, "暂无演示数据", null)
        );
    }

    public record WeatherResult(
            String city,
            String condition,
            Integer temperatureCelsius
    ) {
    }
}
