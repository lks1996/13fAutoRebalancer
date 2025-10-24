package com.autoRebalancer.Kis.Repository;

import com.autoRebalancer.Kis.Entity.Token;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class FirestoreTokenRepository implements TokenRepository {
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "tokens";

    public FirestoreTokenRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public Token save(Token token) {
        try {
            firestore.collection(COLLECTION_NAME)
                    .add(token)
                    .get();
            return token;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save token", e);
        }
    }

    @Override
    public Optional<Token> findTopByTypeOrderByIdDesc(String type) {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("type", type)
                    .orderBy("expiration", Query.Direction.DESCENDING)
                    .limit(1)
                    .get();

            // future.get() 해서 실제 결과(QuerySnapshot)를 가져옴
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            if (documents.isEmpty()) {
                return Optional.empty();
            }

            return Optional.ofNullable(documents.get(0).toObject(Token.class));

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch latest token", e);
        }
    }

}
