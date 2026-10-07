package com.sinan.taskmanager.service;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sinan.taskmanager.entity.User;
import com.sinan.taskmanager.exception.DuplicateResourceException;
import com.sinan.taskmanager.exception.ResourceNotFoundException;
import com.sinan.taskmanager.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional
	public User createUser(User user) {
		String email = normalizeEmail(user.getEmail());
		if (userRepository.existsByEmail(email)) {
			throw emailTaken(email);
		}
		user.setName(user.getName().trim());
		user.setEmail(email);
		return userRepository.save(user);
	}

	public User getUser(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User", id));
	}

	public List<User> getAllUsers() {
		return userRepository.findAll(Sort.by("id"));
	}

	@Transactional
	public User updateUser(Long id, User changes) {
		User user = getUser(id);
		String email = normalizeEmail(changes.getEmail());
		if (userRepository.existsByEmailAndIdNot(email, id)) {
			throw emailTaken(email);
		}
		user.setName(changes.getName().trim());
		user.setEmail(email);
		return userRepository.save(user);
	}

	/** Deletes the user together with all of their tasks. */
	@Transactional
	public void deleteUser(Long id) {
		userRepository.delete(getUser(id));
	}

	// Emails are stored lower-cased so the unique constraint is case-insensitive.
	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static DuplicateResourceException emailTaken(String email) {
		return new DuplicateResourceException("Email " + email + " is already in use");
	}

}
