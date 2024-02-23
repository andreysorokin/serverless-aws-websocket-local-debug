package com.rewintous.syncstate.api.clients;

import com.rewintous.syncstate.Role;
import com.rewintous.syncstate.SyncState;
import com.rewintous.syncstate.model.RoomToConnectionId;
import lombok.SneakyThrows;
import lombok.var;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

public class DynamoDbStateDao {

    private static final String ROOM_CONNECTIONS_TABLE = "roomToConnection";
    private static final String CONNECTION_ID_INDEX = "ConnectionIdIndex";
    private final DynamoDbTable<RoomToConnectionId> roomToConnectionTable;

    @SneakyThrows
    public DynamoDbStateDao(String stage) {
        DynamoDbEnhancedClient.Builder builder = DynamoDbEnhancedClient
                .builder();

        if ("local".equals(stage)) {
            builder.dynamoDbClient(
                    // Configure an instance of the standard client.
                    DynamoDbClient.builder()
                            .region(Region.US_EAST_1)
                            .endpointOverride(new URI("http://localhost:8000"))
                            .credentialsProvider(
                                    StaticCredentialsProvider.create(
                                            AwsBasicCredentials.create("dummyAccessKey", "dummySecretKey")
                                    )
                            )
                            .build());
        }

        DynamoDbEnhancedClient enhancedClient = builder.build();

        this.roomToConnectionTable =
                enhancedClient.table(String.format("%s-%s", ROOM_CONNECTIONS_TABLE, stage),
                        TableSchema.fromBean(RoomToConnectionId.class));

    }


    public void deleteByConnectionId(String connectionId) {
        var pagedResult = roomToConnectionTable.index(CONNECTION_ID_INDEX)
                .query(QueryEnhancedRequest.builder()
                        .queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(connectionId)))
                        .build());

        pagedResult.stream().forEach(page -> page.items().forEach(roomToConnectionTable::deleteItem));
    }

    public void persistStateConnectionId(SyncState syncState, String connectionId) {
        roomToConnectionTable.putItem(new RoomToConnectionId(syncState.getRoomId(), connectionId, syncState.getRole()));
    }

    public RoomToConnectionId retrieveConnectionId(Role role, String roomId) {
        Key key = Key.builder()
                .partitionValue(roomId)
                .sortValue(role.name())
                .build();

        return roomToConnectionTable.getItem(item -> item.key(key));
    }



}
