package com.college.icrs.repository;


import com.college.icrs.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional <User> findByVerificationCode(String verificationcode);

    Optional <User> findByUsername(String username);
}


