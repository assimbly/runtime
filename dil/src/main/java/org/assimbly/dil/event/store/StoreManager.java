package org.assimbly.dil.event.store;

import org.assimbly.dil.event.domain.Store;
import org.assimbly.dil.event.store.impl.ConsoleStore;
import org.assimbly.dil.event.store.impl.ElasticStore;
import org.assimbly.dil.event.store.impl.FileStore;

import java.util.ArrayList;

public class StoreManager {

    private FileStore fileStore;
    private ElasticStore elasticStore;
    private ConsoleStore consoleStore;
    private String collectorId;
    private ArrayList<Store> stores;

    public StoreManager(String collectorId, ArrayList<Store> stores){
        this.collectorId = collectorId;
        this.stores = stores;
        storeSetup();
    }

    public void storeEvent(String json){

        for(org.assimbly.dil.event.domain.Store store: stores){

            switch (store.getType()) {
                case "file":
                    fileStore.store(json);
                    break;
                case "elastic":
                    elasticStore.store(json);
                    break;
                case "console":
                    consoleStore.store(json);
                    break;
            }

        }

    }

    private void storeSetup(){

        for(org.assimbly.dil.event.domain.Store store: stores){

            switch (store.getType()) {
                case "file":
                    fileStore = new FileStore(collectorId, store);
                    break;
                case "elastic":
                    elasticStore = new ElasticStore(collectorId, store);
                    break;
                case "console":
                    consoleStore = new ConsoleStore(collectorId, store);
                    break;
            }

        }

    }

}
