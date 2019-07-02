package org.gitlab4j.simplecr.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.gitlab4j.simplecr.model.MergeSpec;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface MergeSpecRepository extends CrudRepository<MergeSpec, Long>  {

    List<MergeSpec> findByProjectId(Integer projectId);
    List<MergeSpec> findByProjectConfigId(Long projectConfigId);

    @Query(value = "SELECT * FROM merge_spec" +
            " WHERE project_id = :projectId AND branch_regex = :branchRegex AND target_branch = :targetBranch", nativeQuery = true)
    Optional<MergeSpec> find(@Param("projectId") Integer projectId, @Param("branchRegex") String branchRegex, @Param("targetBranch") String targetBranch);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM merge_spec WHERE project_id = :projectId", nativeQuery = true)
    void clearMergeSpecs(@Param("projectId") Integer projectId);
}
