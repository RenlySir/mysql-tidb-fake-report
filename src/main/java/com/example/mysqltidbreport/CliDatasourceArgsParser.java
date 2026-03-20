package com.example.mysqltidbreport;

import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class CliDatasourceArgsParser {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final String DEFAULT_PORT = "3306";
    private static final String URL_QUERY = "useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai";

    private CliDatasourceArgsParser() {
    }

    static String[] normalize(String[] args) {
        ParsedOptions parsed = parse(args);
        if (!parsed.hasDatasourceOverrides()) {
            return args;
        }

        String host = StringUtils.hasText(parsed.host) ? parsed.host : DEFAULT_HOST;
        String port = StringUtils.hasText(parsed.port) ? parsed.port : DEFAULT_PORT;
        String database = StringUtils.hasText(parsed.database) ? parsed.database : "";

        List<String> normalized = new ArrayList<>(parsed.passThroughArgs);
        normalized.add("--spring.datasource.url=" + buildJdbcUrl(host, port, database));
        if (parsed.username != null) {
            normalized.add("--spring.datasource.username=" + parsed.username);
        }
        if (parsed.password != null) {
            normalized.add("--spring.datasource.password=" + parsed.password);
        }
        return normalized.toArray(new String[0]);
    }

    private static ParsedOptions parse(String[] args) {
        ParsedOptions parsed = new ParsedOptions();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if ("--".equals(arg)) {
                for (int j = i; j < args.length; j++) {
                    parsed.passThroughArgs.add(args[j]);
                }
                break;
            }

            if ("-u".equals(arg) || "--user".equals(arg)) {
                parsed.username = readNextValue(args, ++i, arg);
                continue;
            }
            if (arg.startsWith("--user=")) {
                parsed.username = arg.substring("--user=".length());
                continue;
            }

            if ("-h".equals(arg) || "--host".equals(arg)) {
                parsed.host = readNextValue(args, ++i, arg);
                continue;
            }
            if (arg.startsWith("--host=")) {
                parsed.host = arg.substring("--host=".length());
                continue;
            }

            if ("-P".equals(arg) || "--port".equals(arg)) {
                parsed.port = readNextValue(args, ++i, arg);
                continue;
            }
            if (arg.startsWith("--port=")) {
                parsed.port = arg.substring("--port=".length());
                continue;
            }

            if ("-d".equals(arg) || "--database".equals(arg) || "--db".equals(arg)) {
                parsed.database = readNextValue(args, ++i, arg);
                continue;
            }
            if (arg.startsWith("--database=")) {
                parsed.database = arg.substring("--database=".length());
                continue;
            }
            if (arg.startsWith("--db=")) {
                parsed.database = arg.substring("--db=".length());
                continue;
            }

            if ("-p".equals(arg) || "--password".equals(arg)) {
                parsed.password = readNextValue(args, ++i, arg);
                continue;
            }
            if (arg.startsWith("--password=")) {
                parsed.password = arg.substring("--password=".length());
                continue;
            }
            if (arg.startsWith("-p") && arg.length() > 2) {
                parsed.password = arg.substring(2);
                continue;
            }

            if ("-c".equals(arg) || "--config".equals(arg)) {
                parsed.configPath = readNextValue(args, ++i, arg);
                continue;
            }
            if (arg.startsWith("--config=")) {
                parsed.configPath = arg.substring("--config=".length());
                continue;
            }

            parsed.passThroughArgs.add(arg);
        }
        applyConfigIfPresent(parsed);
        return parsed;
    }

    private static String readNextValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for option: " + option);
        }
        return args[index];
    }

    private static String buildJdbcUrl(String host, String port, String database) {
        return "jdbc:mysql://" + host + ":" + port + "/" + database + "?" + URL_QUERY;
    }

    private static void applyConfigIfPresent(ParsedOptions parsed) {
        if (!StringUtils.hasText(parsed.configPath)) {
            return;
        }
        Map<String, String> conf = loadConfig(parsed.configPath);

        if (!StringUtils.hasText(parsed.host)) {
            parsed.host = firstNonBlank(conf.get("host"), conf.get("ip"));
        }
        if (!StringUtils.hasText(parsed.port)) {
            parsed.port = conf.get("port");
        }
        if (!StringUtils.hasText(parsed.username)) {
            parsed.username = firstNonBlank(conf.get("user"), conf.get("username"));
        }
        if (!StringUtils.hasText(parsed.password)) {
            parsed.password = firstNonBlank(conf.get("password"), conf.get("passwd"));
        }
        if (!StringUtils.hasText(parsed.database)) {
            parsed.database = firstNonBlank(conf.get("database"), conf.get("db"), conf.get("schema"));
        }
    }

    private static Map<String, String> loadConfig(String configPath) {
        String content;
        try {
            Path path = Path.of(configPath);
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read config file: " + configPath, e);
        }

        Map<String, String> conf = new HashMap<>();
        String[] tokens = content.split("[;\\r\\n]+");
        for (String token : tokens) {
            String item = token.trim();
            if (item.isEmpty() || item.startsWith("#")) {
                continue;
            }
            int sep = item.indexOf('=');
            if (sep <= 0 || sep == item.length() - 1) {
                continue;
            }

            String key = item.substring(0, sep).trim().toLowerCase(Locale.ROOT);
            String value = stripQuotes(item.substring(sep + 1).trim());
            conf.put(key, value);
        }

        if (conf.isEmpty()) {
            throw new IllegalArgumentException("Invalid config format, expected key=value pairs separated by ';' or newline");
        }
        return conf;
    }

    private static String stripQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static class ParsedOptions {
        private String username;
        private String password;
        private String host;
        private String port;
        private String database;
        private String configPath;
        private final List<String> passThroughArgs = new ArrayList<>();

        private boolean hasDatasourceOverrides() {
            return username != null || password != null || host != null || port != null || database != null;
        }
    }
}
