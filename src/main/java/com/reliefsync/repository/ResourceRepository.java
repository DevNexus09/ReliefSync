package com.reliefsync.repository;

import com.reliefsync.model.Resource;
import com.reliefsync.model.search.ResourceSearchCriteria;
import java.util.List;
import java.util.Optional;

public interface ResourceRepository {
  Optional<Resource> findById(long id);

  List<Resource> findAll();

  Optional<Resource> findByNameAndUnit(String name, String unit);

  List<Resource> search(ResourceSearchCriteria criteria, int limit);

  long save(Resource resource);

  void update(Resource resource);
}
