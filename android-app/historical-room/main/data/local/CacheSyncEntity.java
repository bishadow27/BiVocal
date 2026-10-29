package com.example.bivoca.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** A marker is needed to distinguish an empty snapshot from no prior download. */
@Entity(tableName = "cache_sync")
public final class CacheSyncEntity {
    @PrimaryKey @NonNull public final String scope;
    public final long syncedAt;

    public CacheSyncEntity(@NonNull String scope, long syncedAt) {
        this.scope = scope;
        this.syncedAt = syncedAt;
    }
}
