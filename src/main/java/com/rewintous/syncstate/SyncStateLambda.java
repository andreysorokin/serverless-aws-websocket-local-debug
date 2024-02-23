package com.rewintous.syncstate;

import com.amazonaws.services.apigatewaymanagementapi.AmazonApiGatewayManagementApi;
import com.amazonaws.services.apigatewaymanagementapi.model.PostToConnectionRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2WebSocketEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2WebSocketResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rewintous.syncstate.api.clients.DynamoDbStateDao;
import lombok.var;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.rewintous.syncstate.api.clients.GatewayClientUtils.getGatewayManagementApi;

public class SyncStateLambda implements RequestHandler<APIGatewayV2WebSocketEvent, APIGatewayV2WebSocketResponse> {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOG = Logger.getLogger(SyncStateLambda.class.getName());

    @Override
    public APIGatewayV2WebSocketResponse handleRequest(APIGatewayV2WebSocketEvent event, Context context) {
        final String eventType = event.getRequestContext().getEventType();
        LOG.info(() -> String.format("Processing event type: %s for connectionId: %s", eventType, event.getRequestContext().getConnectionId()));

        if ("CONNECT".equals(eventType)) {
            return ok();
        }

        var stateDao = new DynamoDbStateDao(event.getRequestContext().getStage());

        if ("DISCONNECT".equals(eventType)) {
            return handleDisconnect(stateDao, event);
        }


        return handleSyncEvents(stateDao, event);
    }

    private APIGatewayV2WebSocketResponse handleDisconnect(DynamoDbStateDao stateDao, APIGatewayV2WebSocketEvent event) {
        stateDao.deleteByConnectionId(event.getRequestContext().getConnectionId());
        return ok();
    }

    private APIGatewayV2WebSocketResponse handleSyncEvents(DynamoDbStateDao stateDao, APIGatewayV2WebSocketEvent event) {
        SyncState syncState = gson.fromJson(event.getBody(), SyncState.class);
        LOG.info(String.format("syncState: %s", syncState));
        if (syncState == null) {
            return badRequest(new IllegalArgumentException("Empty syncState payload"));
        }

        AmazonApiGatewayManagementApi apiGwClient =
                getGatewayManagementApi(
                        event.getRequestContext().getDomainName(),
                        event.getRequestContext().getStage());

        final String connectionId = event.getRequestContext().getConnectionId();
        LOG.info(String.format("Persisting syncstate: %s for connectionId: %s", syncState, connectionId ));
        stateDao.persistStateConnectionId(syncState, connectionId);

        if (Role.PUBLISHER == syncState.getRole()) {
            var roomToConnectionIdEntry = stateDao.retrieveConnectionId(Role.CLIENT, syncState.getRoomId());
            LOG.log(Level.FINE, String.format("Retrueved room %s clientInfo %s", syncState.getRoomId(), roomToConnectionIdEntry));
            if (roomToConnectionIdEntry != null) {
                String clientConnectionId = roomToConnectionIdEntry.getConnectionId();
                var result = apiGwClient.postToConnection(new PostToConnectionRequest().withConnectionId(clientConnectionId)
                        .withData(ByteBuffer.wrap(gson.toJson(syncState).getBytes())));
                LOG.log(Level.FINE, String.format("Response from connectionId %s in room %s is %s", clientConnectionId, syncState.getRoomId(), result));
            }
        }

        return ok();
    }

    private static APIGatewayV2WebSocketResponse createErrorResponse(int statusCode, Exception e) {
        return createResponse(statusCode, String.format("{'ack': false, 'error': %s}", e.getMessage()));
    }

    private static APIGatewayV2WebSocketResponse createResponse(int statusCode, String body) {
        var response = new APIGatewayV2WebSocketResponse();
        response.setStatusCode(statusCode);
        response.setBody(body);
        return response;
    }

    private static APIGatewayV2WebSocketResponse ok() {
        return createResponse(200, "{'ack': true}");
    }

    private static APIGatewayV2WebSocketResponse badRequest(Exception e) {
        return createErrorResponse(400, e);
    }
}