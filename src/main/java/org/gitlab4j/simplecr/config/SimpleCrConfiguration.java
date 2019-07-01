package org.gitlab4j.simplecr.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 *
 */
@Configuration
@Primary
@ConfigurationProperties("simplecr")
@EnableConfigurationProperties
public class SimpleCrConfiguration {

    // IMPORTANT: These values must be specified
    private String gitLabApiToken;
    private String gitLabApiUrl;
    private String gitLabWebUrl;
    private String simpleCrUrl;

    private String fromEmail;
    private String fromName;

    private String  defaultTargetBranchesRegex;

    private String dbPassword;
    private String dbUser;
    private String dbName;

    private List<String> defaultReviewers;

    public String getGitLabApiUrl() {
        return (gitLabApiUrl);
    }

    public String getGitLabWebUrl() {
        return (gitLabWebUrl);
    }

    public String getSimpleCrUrl() {
        return (simpleCrUrl);
    }

    public String getGitLabApiToken() {
        return (gitLabApiToken);
    }

    public String getFromEmail() {
        return (fromEmail);
    }

    public String getFromName() {
        return (fromName);
    }

    public List<String> getDefaultReviewers() {
        return (defaultReviewers);
    }

    public String getDefaultTargetBranchesRegex() {
        return (defaultTargetBranchesRegex);
    }

    public String getDbPassword() {
        return (dbPassword);
    }

    public String getDbUser() {
        return (dbUser);
    }

    public String getDbName() {
        return (dbName);
    }

    public void setGitLabApiUrl(String gitLabApiUrl) {
        this.gitLabApiUrl = gitLabApiUrl;
    }

    public void setGitLabWebUrl(String gitLabWebUrl) {
        this.gitLabWebUrl = gitLabWebUrl;
    }

    public void setSimpleCrUrl(String simpleCrUrl) {
        this.simpleCrUrl = simpleCrUrl;
    }

    public void setGitLabApiToken(String gitLabApiToken) {
        this.gitLabApiToken = gitLabApiToken;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public void setDefaultReviewers(List<String> defaultReviewers) {
        this.defaultReviewers = defaultReviewers;
    }

    public void setDefaultTargetBranchesRegex(String defaultTargetBranchesRegex) {
        this.defaultTargetBranchesRegex = defaultTargetBranchesRegex;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }
}
