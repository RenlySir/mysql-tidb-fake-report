package com.example.mysqltidbreport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliDatasourceArgsParserTest {

    @TempDir
    Path tempDir;

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

    @Test
    void shouldLoadDatasourceFromConfigFile() throws Exception {
        Path conf = tempDir.resolve("config.yaml");
        Files.writeString(conf, "host=192.12.10.8;port=3306;password=abc123;user=report_user;");

        String[] normalized = CliDatasourceArgsParser.normalize(new String[]{"-c", conf.toString()});

        assertContains(normalized, "--spring.datasource.url=jdbc:mysql://192.12.10.8:3306/?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai");
        assertContains(normalized, "--spring.datasource.username=report_user");
        assertContains(normalized, "--spring.datasource.password=abc123");
    }

    @Test
    void shouldPreferCliArgsOverConfigFile() throws Exception {
        Path conf = tempDir.resolve("config.yaml");
        Files.writeString(conf, "host=192.12.10.8;port=3306;password=from_config;user=cfg_user;");

        String[] normalized = CliDatasourceArgsParser.normalize(new String[]{
                "-c", conf.toString(),
                "-h", "10.0.0.8",
                "-P", "3310",
                "-u", "cli_user",
                "-pcli_pwd"
        });

        assertContains(normalized, "--spring.datasource.url=jdbc:mysql://10.0.0.8:3310/?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai");
        assertContains(normalized, "--spring.datasource.username=cli_user");
        assertContains(normalized, "--spring.datasource.password=cli_pwd");
    }

    @Test
    void shouldThrowWhenConfigFileMissing() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> CliDatasourceArgsParser.normalize(new String[]{"-c", tempDir.resolve("missing.yaml").toString()}));

        assertTrue(ex.getMessage().contains("Unable to read config file"));
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
