package com.example.trafficcsv.config;

import com.azure.cosmos.*;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CosmosConfig {

    @Value("${azure.cosmos.uri}")
    private String uri;

    @Value("${azure.cosmos.key}")
    private String key;

    @Value("${azure.cosmos.database}")
    private String databaseName;

    @Value("${azure.cosmos.container}")
    private String containerName;

    private CosmosAsyncClient asyncClient;

    /** Build the reactive Cosmos client */
    @Bean
    public CosmosAsyncClient cosmosAsyncClient() {
        asyncClient = new CosmosClientBuilder()
                .endpoint(uri)
                .key(key)
                .gatewayMode()              // or .directMode() if you prefer TCP
                .buildAsyncClient();
        return asyncClient;
    }

    /** Expose the container so the service can @Autowired it */
    @Bean
    public CosmosAsyncContainer cosmosAsyncContainer(CosmosAsyncClient client) {
        return client
                .getDatabase(databaseName)
                .getContainer(containerName);
    }

    /** Graceful shutdown */
    @PreDestroy
    public void close() {
        if (asyncClient != null) {
            asyncClient.close();
        }
    }
}
