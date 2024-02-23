package com.rewintous.syncstate;

import lombok.Data;

@Data
public class SyncState {
    String roomId;

    private Role role;
    String stateString;
}
