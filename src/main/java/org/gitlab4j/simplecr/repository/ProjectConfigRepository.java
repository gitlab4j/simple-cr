package org.gitlab4j.simplecr.repository;

import org.gitlab4j.simplecr.model.ProjectConfig;
import org.springframework.data.repository.CrudRepository;

public interface ProjectConfigRepository extends CrudRepository<ProjectConfig, Long>  {
    ProjectConfig findByProjectId(int projectId);
}
