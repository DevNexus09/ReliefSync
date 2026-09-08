package com.reliefsync.model;

/** A relief request; status is the only mutable field and changes via the State pattern. */
public final class ReliefRequest {

    private final long id;
    private final long areaId;
    private final Priority priority;
    private RequestStatus status;
    private final String note;
    private final long createdBy;
    private final String createdAt;

    public ReliefRequest(long id, long areaId, Priority priority, RequestStatus status,
                         String note, long createdBy, String createdAt) {
        this.id = id;
        this.areaId = areaId;
        this.priority = priority;
        this.status = status;
        this.note = note;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public long id() {
        return id;
    }

    public long areaId() {
        return areaId;
    }

    public Priority priority() {
        return priority;
    }

    public RequestStatus status() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String note() {
        return note;
    }

    public long createdBy() {
        return createdBy;
    }

    public String createdAt() {
        return createdAt;
    }
}
