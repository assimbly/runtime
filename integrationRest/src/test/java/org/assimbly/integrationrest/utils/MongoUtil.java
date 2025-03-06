package org.assimbly.integrationrest.utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Arrays;

public class MongoUtil {

    private static String TENANT_VARIABLES_COL = "tenant_variables";
    private static String USERS_COL = "users";

    private static String TENANT_VARIABLE_TYPE = "TenantVariable";

    public static void createTenantVariable(MongoDatabase database, String environment, String name, String value) {
        MongoCollection<Document> collection = database.getCollection(TENANT_VARIABLES_COL);

        ObjectId tenantVariableId = new ObjectId();
        ObjectId valueId = new ObjectId();

        collection.insertOne(new Document("_id", tenantVariableId)
                .append("_type", TENANT_VARIABLE_TYPE)
                .append("name", name)
                .append("createdAt", System.currentTimeMillis())
                .append("createdBy", "System")
                .append("values", Arrays.asList(
                        new Document("_id", valueId)
                                .append("environment", environment)
                                .append("value", value)
                                .append("encrypted", false)
                                .append("last_update", System.currentTimeMillis())
                                .append("updatedBy", "System")
                )));
    }

    public static void createUser(String firstName, String lastName, String email, String password) {
        MongoClient mongoClient = MongoClients.create(AssimblyGatewayHeadlessContainer.getMongoContainer().getReplicaSetUrl());
        MongoDatabase database = mongoClient.getDatabase("base");
        MongoCollection<Document> collection = database.getCollection(USERS_COL);

        ObjectId userId = new ObjectId();

        collection.insertOne(new Document("_id", userId)
                .append("_type", TENANT_VARIABLE_TYPE)
                .append("status", "active")
                .append("role", "user")
                .append("first_name", firstName)
                .append("last_name", lastName)
                .append("email", email)
                .append("password_digest", password)
        );
    }
}