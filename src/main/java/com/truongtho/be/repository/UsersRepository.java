package com.truongtho.be.repository;

import com.truongtho.be.entity.Users;
import com.truongtho.be.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, String> {
    Optional<Users> findByEmail(String email);
    Optional<Users> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    @Query(value = """
            select distinct u from Users u left join u.roles r
            where (:keyword is null
                   or lower(u.fullName) like :keyword
                   or lower(u.email) like :keyword
                   or u.phoneNumber like :keyword)
              and (:role is null or r.name = :role)
              and (:status is null or u.userStatus = :status)
            """,
            countQuery = """
            select count(distinct u) from Users u left join u.roles r
            where (:keyword is null
                   or lower(u.fullName) like :keyword
                   or lower(u.email) like :keyword
                   or u.phoneNumber like :keyword)
              and (:role is null or r.name = :role)
              and (:status is null or u.userStatus = :status)
            """)
    Page<Users> search(@Param("keyword") String keyword,
                       @Param("role") String role,
                       @Param("status") UserStatus status,
                       Pageable pageable);
}
