package org.gitlab4j.simplecr.beans;

import java.util.List;

public class CodeReviewInfo {

    private String group;
    private Integer projectId;
    private String projectName;
    private String projectUrl;

    private Integer userId;
    private String name;
    private String email;

    private String commitId;
    private String sourceBranch;
    private String targetBranch;

    private String gitlabWebUrl;

    private List<String> targetBranches;
    private String title;
    private String description;

    /**
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * @param group the group to set
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * @return the projectId
     */
    public Integer getProjectId() {
        return projectId;
    }

    /**
     * @param projectId the projectId to set
     */
    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    /**
     * @return the projectName
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * @param projectName the projectName to set
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * @return the projectUrl
     */
    public String getProjectUrl() {
        return projectUrl;
    }

    /**
     * @param projectUrl the projectUrl to set
     */
    public void setProjectUrl(String projectUrl) {
        this.projectUrl = projectUrl;
    }

    /**
     * @return the userId
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * @param userId the userId to set
     */
    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    /**
     * @return the name of the user that pushed the branch
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the email of the user that pushed the branch
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the commitId
     */
    public String getCommitId() {
        return commitId;
    }

    /**
     * @param commitId the commitId to set
     */
    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    /**
     * @return the sourceBranch
     */
    public String getSourceBranch() {
        return sourceBranch;
    }

    /**
     * @param sourceBranch the sourceBranch to set
     */
    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    /**
     * @return the targetBranch
     */
    public String getTargetBranch() {
        return targetBranch;
    }

    /**
     * @param targetBranch the targetBranch to set
     */
    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    /**
     * @return the gitlabWebUrl
     */
    public String getGitlabWebUrl() {
        return gitlabWebUrl;
    }

    /**
     * @param gitlabWebUrl the gitlabWebUrl to set
     */
    public void setGitlabWebUrl(String gitlabWebUrl) {
        this.gitlabWebUrl = gitlabWebUrl;
    }

    /**
     * @return the targetBranches
     */
    public List<String> getTargetBranches() {
        return targetBranches;
    }

    /**
     * @param targetBranches the targetBranches to set
     */
    public void setTargetBranches(List<String> targetBranches) {
        this.targetBranches = targetBranches;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
