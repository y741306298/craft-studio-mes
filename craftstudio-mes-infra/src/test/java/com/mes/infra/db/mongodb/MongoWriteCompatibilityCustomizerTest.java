package com.mes.infra.db.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import org.bson.UuidRepresentation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.mongodb.autoconfigure.StandardMongoClientSettingsBuilderCustomizer;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.List;
import java.util.ArrayList;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;

import static org.junit.jupiter.api.Assertions.*;

class MongoWriteCompatibilityCustomizerTest {

    @Test
    void disablesRetryWritesAfterConnectionStringWhilePreservingOtherSettings() {
        ConnectionString connectionString = new ConnectionString(
                "mongodb://localhost:27018/mes?retryWrites=true&retryReads=false&appName=mes-test");
        MongoClientSettings.Builder builder = MongoClientSettings.builder().applyConnectionString(connectionString);
        MongoClientSettings before = builder.build();

        new MongoWriteCompatibilityCustomizer().customize(builder);

        MongoClientSettings after = builder.build();
        assertFalse(after.getRetryWrites());
        assertEquals(before.getRetryReads(), after.getRetryReads());
        assertEquals(before.getClusterSettings().getHosts(), after.getClusterSettings().getHosts());
        assertEquals(before.getApplicationName(), after.getApplicationName());
        assertEquals(before.getWriteConcern(), after.getWriteConcern());
    }

    @Test
    void runsAfterBootStandardSettingsSoUriCannotReenableRetryWrites() {
        MongoClientSettingsBuilderCustomizer standard = new StandardMongoClientSettingsBuilderCustomizer(
                () -> new ConnectionString("mongodb://localhost:27017/mes?retryWrites=true"),
                UuidRepresentation.STANDARD);
        MongoClientSettingsBuilderCustomizer compatibility = new MongoWriteCompatibilityCustomizer();
        List<MongoClientSettingsBuilderCustomizer> customizers = new ArrayList<>(List.of(compatibility, standard));
        AnnotationAwareOrderComparator.sort(customizers);
        MongoClientSettings.Builder builder = MongoClientSettings.builder();
        customizers.forEach(customizer -> customizer.customize(builder));
        assertSame(compatibility, customizers.getLast());
        assertFalse(builder.build().getRetryWrites());
    }
}
