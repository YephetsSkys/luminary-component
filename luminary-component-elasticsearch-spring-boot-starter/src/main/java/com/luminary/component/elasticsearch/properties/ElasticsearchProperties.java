package com.luminary.component.elasticsearch.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import com.luminary.component.elasticsearch.config.JestConfig;

@Data
@ConfigurationProperties("luminary.elasticsearch")
public class ElasticsearchProperties {

    private String username = JestConfig.DEFAULT_USER_NAME;

    private String password = JestConfig.DEFAULT_PASSWORD;

    private String server = StringUtils.arrayToCommaDelimitedString(JestConfig.DEFAULT_SERVER);

    private boolean multiThreaded = JestConfig.DEFAULT_MULTI_THREADED;

    private int maxTotalConnection = JestConfig.DEFAULT_MAX_TOTAL_CONNECTION;

    private int maxTotalConnectionPerRoute = JestConfig.DEFAULT_MAX_TOTAL_CONNECTION_PER_ROUTE;

    private boolean discoveryEnabled = JestConfig.DEFAULT_DISCOVERY_ENABLED;

    private int discorveryFrequency = JestConfig.DISCORVERY_FREQUENCY;

}
