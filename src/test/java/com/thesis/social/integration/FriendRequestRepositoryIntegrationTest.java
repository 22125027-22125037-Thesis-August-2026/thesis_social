package com.thesis.social.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.thesis.social.friend.entity.FriendRequestEntity;
import com.thesis.social.friend.entity.FriendRequestStatus;
import com.thesis.social.friend.repository.FriendRequestRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("resource")
class FriendRequestRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("social_features")
        .withUsername("social")
        .withPassword("social");

    @DynamicPropertySource
    static void databaseProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private FriendRequestRepository repository;

    @Test
    void shouldPersistPendingFriendRequest() {
        FriendRequestEntity entity = new FriendRequestEntity();
        entity.setSenderId(UUID.randomUUID());
        entity.setReceiverId(UUID.randomUUID());
        entity.setStatus(FriendRequestStatus.PENDING);

        FriendRequestEntity saved = repository.save(entity);

        assertTrue(repository.existsBySenderIdAndReceiverIdAndStatus(
            saved.getSenderId(),
            saved.getReceiverId(),
            FriendRequestStatus.PENDING
        ));
    }
}
