package com.sinan.taskmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sinan.taskmanager.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

}
