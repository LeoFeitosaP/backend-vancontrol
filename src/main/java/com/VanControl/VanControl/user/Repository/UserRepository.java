package com.VanControl.VanControl.user.Repository;

import com.VanControl.VanControl.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("select u from User u where lower(trim(u.email)) = lower(trim(:email))")
    Optional<User> findByEmail(@Param("email") String email);

    User findByCpf(String cpf);

    @Query(value = """
            select exists (
                select 1 from users
                where replace(replace(trim(cpf), '.', ''), '-', '') =
                      replace(replace(trim(:cpf), '.', ''), '-', '')
            )
            """, nativeQuery = true)
    boolean existsByCpfNormalizado(@Param("cpf") String cpf);
}