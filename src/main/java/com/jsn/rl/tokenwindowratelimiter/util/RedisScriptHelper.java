package com.jsn.rl.tokenwindowratelimiter.util;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisScriptingCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsn.rl.tokenwindowratelimiter.TokenWindowRateLimiter2;

public class RedisScriptHelper {
    private static final Logger LOG = LoggerFactory.getLogger(RedisScriptHelper.class);
    private StatefulRedisConnection<String, String> connection;
    private Map<String, String> scripts = new HashMap<>();

    public RedisScriptHelper(StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
    }

    public String loadScript(String scriptName) throws IOException {
        String script = readScriptFile(scriptName);
        //LOG.info("<<<<<>>>>>>>script: {}", script);
        RedisScriptingCommands<String, String> scriptingCommands = connection.sync();
        String sha1 = scriptingCommands.scriptLoad(script);

        scripts.put(scriptName, sha1);
        return sha1;
    }

    private String readScriptFile(String scriptName) throws IOException {
        URL url = RedisScriptHelper.class.getClassLoader().getResource(scriptName);

        if (url == null) {
            throw new IllegalArgumentException("script '" + scriptName + "' not found");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    public void unloadScript(String scriptName) {
        scripts.remove(scriptName);
    }

    public List<Long> executeScript(String scriptName, String[] keys, String[] args) {
        if (!scripts.containsKey(scriptName)) {
            throw new IllegalArgumentException("Script not loaded: " + scriptName);
        }
        RedisScriptingCommands<String, String> scriptingCommands = connection.sync();
        String sha1 = scripts.get(scriptName);
        List<Long> ret = scriptingCommands.evalsha(sha1, ScriptOutputType.MULTI, keys, args);

        if(ret.get(0) == null) {
            ret.set(0, 0L);
        }
        return ret;
    }
}