package com.rewintous.syncstate.api.clients;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.apigatewaymanagementapi.AmazonApiGatewayManagementApi;
import com.amazonaws.services.apigatewaymanagementapi.AmazonApiGatewayManagementApiClientBuilder;

public class GatewayClientUtils {
    public static AmazonApiGatewayManagementApi getGatewayManagementApi(String domainName, String stage) {
        boolean isLocalRun = "localhost".equals(domainName);
        String callbackUrl =
                isLocalRun ?
                        "http://localhost:3001" :
                        String.format("https://%s/%s", domainName, stage);

        return AmazonApiGatewayManagementApiClientBuilder.standard().withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(callbackUrl, null)).build();
    }
}
