package com.reliefsync.model;

public record DispatchManifestItem(long id, long manifestId, long allocationId,
                                   String centerName, String resourceName, int quantity) {
}
