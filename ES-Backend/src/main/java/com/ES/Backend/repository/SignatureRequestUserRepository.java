package com.ES.Backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ES.Backend.entity.SignatureRequestUser;

public interface SignatureRequestUserRepository extends JpaRepository<SignatureRequestUser, Long> {
    
}
