package com.autoRebalancer.Kis.Repository;

import com.autoRebalancer.Kis.Entity.Token;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
public interface TokenRepository {

    @Transactional(readOnly = true)
    Optional<Token> findTopByTypeOrderByIdDesc(String type);

    Token save(Token token);
}
