package com.example.mysqltidbreport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliDatasourceArgsParserTest {

    @Test
    void shouldKeepArgsWhenNoShortMysqlOptionsProvided() {
        String[] raw = {"--server.port=18080", "--spring.datasource.username=abc"};

        String[] normalized = CliDatasourceArgsParser.normalize(raw);

        assertArrayEquals(raw, normalized);
    }

    @Test
    void shouldParseShortOptionsAndInlinePassword() {
        String[] raw = {"-u", "root", "-h", "10.0.0.8", "-P", "3307", "-pSecret123", "--server.port=18080"};

        String[] normalized = CliDatasourceArgsParser.normalize(raw);

        assertContains(normalized, "--spring.datasource.url=jdbc:mysql://10.0.0.8:3307/?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai");
        assertContains(normalized, "--spring.datasource.username=root");
        assertContains(normalized, "--spring.datasource.password=Secret123");
        assertContains(normalized, "--server.port=18080");
    }

    @Test
    void shouldParseDatabaseOption() {
        String[] raw = {"-u", "root", "-p", "pwd", "--database=orders"};

        String[] normalized = CliDatasourceArgsParser.normalize(raw);

        assertContains(normalized, "--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/orders?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai");
        assertContains(normalized, "--spring.datasource.username=root");
        assertContains(normalized, "--spring.datasource.password=pwd");
    }

    @Test
    void shouldThrowWhenOptionValueMissing() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> CliDatasourceArgsParser.normalize(new String[]{"-u"}));

        assertTrue(ex.getMessage().contains("Missing value for option: -u"));
    }

    private void assertContains(String[] arr, String expected) {
        for (String it : arr) {
            if (expected.equals(it)) {
                return;
            }
        }
        throw new AssertionError("Missing expected arg: " + expected);
    }
}
