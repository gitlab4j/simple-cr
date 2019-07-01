package org.gitlab4j.simplecr.repository;

import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

import org.gitlab4j.simplecr.model.Push;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface PushRepository extends CrudRepository<Push, Long>  {

    @Query(value = "SELECT * FROM push WHERE project_id = :projectId AND branch = :branch AND user_id = :userId" +
            " AND merge_request_id > 0 AND merge_status IS NULL ORDER BY received_at DESC", nativeQuery = true)
    public List<Push> findPendingReviews(@Param("userId") int userId,
            @Param("projectId") int projectId, @Param("branch") String branch);

    @Query(value = "SELECT * FROM push WHERE project_id = :projectId AND branch = :branch AND user_id = :userId" +
            " AND merge_request_id = :mergeRequestId ORDER BY received_at DESC", nativeQuery = true)
    List<Push> find(@Param("userId") int userId, @Param("projectId") int projectId,
            @Param("branch") String branch, @Param("mergeRequestId") int mergeRequestId);
    
    @Query(value = "SELECT id, received_at, user_id, branch, project_id, before, after, merge_request_id," +
            " merge_status_date, merge_state, merge_status, merged_by_id FROM push" +
            " WHERE project_id = :projectId AND branch = :branch AND user_id = :userId ORDER BY received_at DESC", nativeQuery = true)
    List<Push> find(@Param("userId") int userId, @Param("projectId") int projectId, @Param("branch") String branch);
    
    @Transactional
    @Modifying
    @Query(value = "UPDATE push SET merge_request_id = :mergeRequestId, merge_status_date = :mergeStatusDate," +
            " merge_state = :mergeState, merge_status = :mergeStatus WHERE id = :id", nativeQuery = true)
    int setMergeRequestInfo(@Param("id") long id, @Param("mergeRequestId") int mergeRequestId,
            @Param("mergeStatusDate") Date mergeStatusDate, @Param("mergeState") String mergeState, @Param("mergeStatus") String mergeStatus);
}
