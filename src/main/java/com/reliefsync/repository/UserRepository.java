package com.reliefsync.repository;

import com.reliefsync.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
  Optional<User> findById(long id);

  Optional<User> findByUsername(String username);

  List<User> findAll();

  long save(User user);

  void update(User user);

  boolean existsByUsername(String username);
}
