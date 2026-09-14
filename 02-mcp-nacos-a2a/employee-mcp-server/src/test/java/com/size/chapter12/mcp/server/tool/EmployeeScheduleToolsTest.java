package com.size.chapter12.mcp.server.tool;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeScheduleToolsTest {

    @Test
    void shouldReturnDemoSchedule() {
        var result = new EmployeeScheduleTools().queryEmployeeSchedule("E1001", "2026-09-14");
        assertThat(result.employeeId()).isEqualTo("E1001");
        assertThat(result.date()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(result.shift()).contains("白班");
    }
}
