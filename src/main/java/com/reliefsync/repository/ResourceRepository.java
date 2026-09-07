package com.reliefsync.repository;

import com.reliefsync.model.Resource;
import java.util.List;
import java.util.Optional;

public interface ResourceRepository {
  Optional<Resource> findById(long id);

  List<Resource> findAll();

  long save(Resource resource);

  void update(Resource resource);
}
