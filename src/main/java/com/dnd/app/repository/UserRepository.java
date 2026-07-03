package com.dnd.app.repository;

import com.dnd.app.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Case-insensitive username prefix search for the friends "add user" flow, excluding the caller. */
    @Query("""
            select u from User u
            where u.id <> :excludeId and lower(u.username) like lower(concat(:prefix, '%'))
            order by u.username asc
            """)
    List<User> searchByUsernamePrefix(@Param("prefix") String prefix,
                                      @Param("excludeId") UUID excludeId,
                                      Pageable pageable);
}
