package com.example.mysqltidbreport;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

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

            parsed.passThroughArgs.add(arg);
        }
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

    private static class ParsedOptions {
        private String username;
        private String password;
        private String host;
        private String port;
        private String database;
        private final List<String> passThroughArgs = new ArrayList<>();

        private boolean hasDatasourceOverrides() {
            return username != null || password != null || host != null || port != null || database != null;
        }
    }
}
