package com.sloy.sevibus.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class ParadaRequestSnapshot {

    @DatabaseField(generatedId = true)
    private Integer id;
    @DatabaseField
    private Integer requestedParadaNumero;
    @DatabaseField
    private Long requestedTimestamp;

    public ParadaRequestSnapshot() {
    }

    public ParadaRequestSnapshot(Integer requestedParadaNumero, Long requestedTimestamp) {
        this.requestedParadaNumero = requestedParadaNumero;
        this.requestedTimestamp = requestedTimestamp;
    }

    public Integer getId() {
        return id;
    }

    public Integer getRequestedParadaNumero() {
        return requestedParadaNumero;
    }

    public void setRequestedParadaNumero(Integer requestedParadaNumero) {
        this.requestedParadaNumero = requestedParadaNumero;
    }

    public Long getRequestedTimestamp() {
        return requestedTimestamp;
    }

    public void setRequestedTimestamp(Long requestedTimestamp) {
        this.requestedTimestamp = requestedTimestamp;
    }
}
