package org.assimbly.dil.event.store.impl;

public class ConsoleStore {
    public ConsoleStore(String collectorId, org.assimbly.dil.event.domain.Store store) {
    }

    public void store(String json){
        IO.println(json);
    }
}
