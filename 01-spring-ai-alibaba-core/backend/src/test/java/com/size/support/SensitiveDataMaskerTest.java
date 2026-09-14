package com.size.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    @Test
    void shouldMaskBearerJsonSecretsAndMobile() {
        String source = "Authorization=Bearer abc.def; body={\"apiKey\":\"sk-secret\"," +
                "\"password\":\"123456\"}; mobile=13812345678";

        assertThat(SensitiveDataMasker.mask(source))
                .isEqualTo("Authorization=Bearer ***; body={\"apiKey\":\"***\"," +
                        "\"password\":\"***\"}; mobile=138****5678");
    }

    @Test
    void shouldKeepNullAndOrdinaryText() {
        assertThat(SensitiveDataMasker.mask(null)).isNull();
        assertThat(SensitiveDataMasker.mask("模型调用成功")).isEqualTo("模型调用成功");
    }
}
