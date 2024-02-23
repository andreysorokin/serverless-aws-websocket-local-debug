package com.rewintous.syncstate.model;

import com.rewintous.syncstate.Role;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@NoArgsConstructor
@AllArgsConstructor
@Data
@DynamoDbBean
@ToString
public class RoomToConnectionId {
    @Getter(onMethod_={@DynamoDbPartitionKey})
    String roomId;

    @Getter(onMethod_={@DynamoDbSecondaryPartitionKey(indexNames = {"ConnectionIdIndex"})})
    String connectionId;

    @Getter(onMethod_={@DynamoDbSortKey})
    Role role;
}
