package org.gitlab4j.simplecr.model;

import java.io.IOException;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.gitlab4j.api.utils.JacksonJson;

@Entity
@Table(indexes = { @Index(name = "push_index", columnList = "user_id, project_id, branch, merge_request_id") })
public class Push {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "received_at", nullable = false)
    private Date receivedAt;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "branch", nullable = false)
    private String branch;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "before", nullable = false)
    private String before;

    @Column(name = "after", nullable = false)
    private String after;

    @Column(name = "merge_request_id")
    private Integer mergeRequestId;

    @Column(name = "merge_status_date")
    private Date mergeStatusDate;

    @Column(name = "merge_state")
    private String mergeState;

    @Column(name = "merge_status")
    private String mergeStatus;

    @Column(name = "merged_by_id")
    private Integer mergedById;

    public Push() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Date receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public Integer getMergeRequestId() {
        return mergeRequestId;
    }

    public void setMergeRequestId(Integer mergeRequestId) {
        this.mergeRequestId = mergeRequestId;
    }

    public Date getMergeStatusDate() {
        return mergeStatusDate;
    }

    public void setMergeStatusDate(Date mergeStatusDate) {
        this.mergeStatusDate = mergeStatusDate;
    }

    public String getMergeStatus() {
        return mergeStatus;
    }

    public void setMergeStatus(String mergeStatus) {
        this.mergeStatus = mergeStatus;
    }

    public String getMergeState() {
        return mergeState;
    }

    public void setMergeState(String mergeState) {
        this.mergeState = mergeState;
    }

    public Integer getMergedById() {
        return mergedById;
    }

    public void setMergedById(Integer mergedById) {
        this.mergedById = mergedById;
    }

    @Override
    public String toString() {
        return (JacksonJson.toJsonString(this));
    }

    public String toJson() throws IOException {
        return (JacksonJson.toJsonString(this));
    }
}
