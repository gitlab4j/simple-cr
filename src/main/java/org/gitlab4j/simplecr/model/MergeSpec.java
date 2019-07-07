package org.gitlab4j.simplecr.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(indexes = {
    @Index(name = "merge_spec_index", columnList = "project_id, branch_regex, target_branch_regex", unique = true)
})
public class MergeSpec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_config_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private ProjectConfig projectConfig;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "branch_regex", nullable = false)
    private String branchRegex;

    @Column(name = "target_branch_regex", nullable = false)
    private String targetBranchRegex;

    public MergeSpec() {
    }

    public MergeSpec(ProjectConfig projectConfig, int projectId, String branchRegex, String targetBranchRegex) {
        this.projectConfig = projectConfig;
        this.projectId = projectId;
        this.branchRegex = branchRegex;
        this.targetBranchRegex = targetBranchRegex;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectConfig getProjectConfig() {
        return projectConfig;
    }

    public void setProjectConfig(ProjectConfig projectConfig) {
        this.projectConfig = projectConfig;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getBranchRegex() {
        return branchRegex;
    }

    public void setBranchRegex(String branchRegex) {
        this.branchRegex = branchRegex;
    }

    public String getTargetBranchRegex() {
        return targetBranchRegex;
    }

    public void setTargetBranchRegex(String targetBranchRegex) {
        this.targetBranchRegex = targetBranchRegex;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((branchRegex == null) ? 0 : branchRegex.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((projectConfig == null) ? 0 : projectConfig.hashCode());
        result = prime * result + ((projectId == null) ? 0 : projectId.hashCode());
        result = prime * result + ((targetBranchRegex == null) ? 0 : targetBranchRegex.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        MergeSpec other = (MergeSpec) obj;
        if (branchRegex == null) {
            if (other.branchRegex != null)
                return false;
        } else if (!branchRegex.equals(other.branchRegex)) {
            return false;
        }

        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id)) {
            return false;
        }

        if (projectConfig == null) {
            if (other.projectConfig != null)
                return false;
        } else if (!projectConfig.equals(other.projectConfig)) {
            return false;
        }

        if (projectId == null) {
            if (other.projectId != null)
                return false;
        } else if (!projectId.equals(other.projectId)) {
            return false;
        }

        if (targetBranchRegex == null) {
            if (other.targetBranchRegex != null)
                return false;
        } else if (!targetBranchRegex.equals(other.targetBranchRegex)) {
            return false;
        }

        return true;
    }
}
