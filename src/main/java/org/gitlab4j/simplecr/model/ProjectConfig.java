package org.gitlab4j.simplecr.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.gitlab4j.api.utils.JacksonJson;

@Entity
@Table(name = "project_config")
public class ProjectConfig {

    public enum MailToType {
        NONE, GROUP, PROJECT;

        public static final MailToType findByString(String s) {

            if (s == null) {
                return (null);
            }

            s = s.trim().toUpperCase();
            if (s.length() == 0) {
                return (null);
            }

            try {
                return (MailToType.valueOf(s));
            } catch (IllegalArgumentException iae) {
                return (null);
            }
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false, unique = true)
    private Integer projectId;

    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "hook_id", nullable = false)
    private Integer hookId;

    @Column(name = "mail_to_type")
    @Enumerated(EnumType.STRING)
    private MailToType mailToType;

    @Column(name = "include_default_mail_to")
    private boolean includeDefaultMailTo;

    @Column(name = "additional_mail_to")
    @Convert(converter = StringListConverter.class)
    private List<String> additionalMailTo = new ArrayList<String>();

    @Column(name = "exclude_mail_to")
    @Convert(converter = StringListConverter.class)
    private List<String> excludeMailTo = new ArrayList<String>();

    public ProjectConfig() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreated(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Integer getHookId() {
        return hookId;
    }

    public void setHookId(Integer hookId) {
        this.hookId = hookId;
    }

    public MailToType getMailToType() {
        return (mailToType);
    }

    public void setMailToType(MailToType mailToType) {
        this.mailToType = mailToType;
    }

    public List<String> getAdditionalMailTo() {
        return additionalMailTo;
    }

    public void setAdditionalMailTo(List<String> additionalMailTo) {
        this.additionalMailTo = additionalMailTo;
    }

    public List<String> getExcludeMailTo() {
        return excludeMailTo;
    }

    public void setExcludeMailTo(List<String> excludeMailTo) {
        this.excludeMailTo = excludeMailTo;
    }

    public boolean getIncludeDefaultMailTo() {
        return includeDefaultMailTo;
    }

    public void setIncludeDefaultMailTo(boolean includeDefaultMailTo) {
        this.includeDefaultMailTo = includeDefaultMailTo;
    }

    @Override
    public String toString() {
        return (JacksonJson.toJsonString(this));
    }

    public String toJson() throws IOException {
        return (JacksonJson.toJsonString(this));
    }
}
