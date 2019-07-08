
package org.gitlab4j.simplecr.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectHook;
import org.gitlab4j.simplecr.model.MergeSpec;
import org.gitlab4j.simplecr.model.ProjectConfig;
import org.gitlab4j.simplecr.model.ProjectConfig.MailToType;
import org.gitlab4j.simplecr.repository.MergeSpecRepository;
import org.gitlab4j.simplecr.repository.ProjectConfigRepository;
import org.gitlab4j.simplecr.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This class provides services for Simple-CR administration.
 */
@Service
public class ProjectConfigService {

    @Autowired
    private ProjectConfigRepository projectConfigRepository;

    @Autowired
    private MergeSpecRepository mergeSpecRepository;

    @Autowired
    private GitLabApi gitLabApi;

    private Logger logger = LoggerFactory.getLogger(ProjectConfigService.class);

    // Define the merge specs for a standard GitFlow Git Woirkflow
    private static final String[][]  GITFLOW_MERGE_SPECS = {
            {"^feature.*", "^develop"},
            {"^bug.*"    , "^develop"},
            {"^develop"  , "^master"},
            {"^develop"  , "^release.*"},
            {"^hotfix.*" , "^master"},
            {"^release.*", "^master"}
    };

    public Project getProject(String groupName, String projectName) throws GitLabApiException{
        return gitLabApi.getProjectApi().getProject(groupName, projectName);
    }

    public Project getProject(Integer projectId) throws GitLabApiException{
        return gitLabApi.getProjectApi().getProject(projectId);
    }

    public ProjectConfig getProjectConfig(String groupName, String projectName) throws GitLabApiException{

        logger.info("Fetching project configuration, group={}, project={}", groupName, projectName);

        // Get the specified project
        Project project = getProject(groupName, projectName);

        // Load the Simple-CR project config
        return (projectConfigRepository.findByProjectId(project.getId()));
    }

    public ProjectConfig getProjectConfig(Project project) {
        return (getProjectConfig(project.getId()));
    }

    public ProjectConfig getProjectConfig(Integer projectId) {
        return (projectConfigRepository.findByProjectId(projectId));
    }

    public ProjectConfig addProjectConfig(Project project, Boolean enabled, MailToType mailToType,
           String additionalMailTo, String excludeMailTo, Boolean includeDefaultMailTo, Boolean gitflowMergeSpecs, String webhookUrl) throws GitLabApiException {

        Integer projectId = project.getId();
        ProjectConfig projectConfig = new ProjectConfig();
        projectConfig.setHookId(0);
        projectConfig.setCreated(new Date());
        projectConfig.setProjectId(projectId);
        projectConfig.setEnabled(enabled);
        projectConfig.setMailToType(mailToType);
        projectConfig.setAdditionalMailTo(StringUtils.getListFromString(additionalMailTo, ","));
        projectConfig.setExcludeMailTo(StringUtils.getListFromString(excludeMailTo, ","));
        projectConfig.setIncludeDefaultMailTo(includeDefaultMailTo);
        projectConfig = projectConfigRepository.save(projectConfig);

        if (gitflowMergeSpecs) {

            logger.info("Creating MergeSpecs for GitFlow workflow, projectId={}", projectId);  

            for (String[] spec : GITFLOW_MERGE_SPECS) {
                MergeSpec mergeSpec = new MergeSpec(projectConfig, projectId, spec[0], spec[1]);
                mergeSpecRepository.save(mergeSpec);
            }

            logger.info("Finished creating MergeSpecs for GitFlow workflow, projectId={}", projectId);
        }
   
        try {

            ProjectHook hookConfig = new ProjectHook().withPushEvents(true).withMergeRequestsEvents(true);
            ProjectHook projectHook = gitLabApi.getProjectApi().addHook(project, webhookUrl, hookConfig, false, "simple-cr-" + projectConfig.getId());
            projectConfig.setHookId(projectHook.getId());
            logger.info("Added Simple-CR webhook to GitLab project, projectId={}", projectId);
            
        } catch (GitLabApiException glae) {
            
            try {
                projectConfigRepository.delete(projectConfig);
            } catch (Exception ignore) {}

            throw glae;
        }
        
        projectConfig = projectConfigRepository.save(projectConfig);
        return projectConfig;
    }
 
    public ProjectConfig updateProjectConfig(ProjectConfig projectConfig, Boolean enabled, MailToType mailToType,
        String additionalMailTo, String excludeMailTo, Boolean includeDefaultMailTo, Boolean gitflowMergeSpecs) {
 
        if (mailToType != null) {
            projectConfig.setMailToType(mailToType);
        }

        if (enabled != null) {
            projectConfig.setEnabled(enabled);
        }

        if (additionalMailTo != null) {

            if (additionalMailTo.trim().isEmpty()) {
                additionalMailTo = null;
            }

            projectConfig.setAdditionalMailTo(StringUtils.getListFromString(additionalMailTo, ","));
        }

        if (excludeMailTo != null) {

            if (excludeMailTo.trim().isEmpty()) {
                excludeMailTo = null;
            }

            projectConfig.setExcludeMailTo(StringUtils.getListFromString(excludeMailTo, ","));
        }

        if (includeDefaultMailTo != null) {
            projectConfig.setIncludeDefaultMailTo(includeDefaultMailTo);
        }

        if (gitflowMergeSpecs) {

            Integer projectId = projectConfig.getProjectId();
            logger.info("Clearing existing merge specs, projectId={}", projectId);
            mergeSpecRepository.clearMergeSpecs(projectId);

            logger.info("Creating MergeSpecs for GitFlow workflow, projectId={}", projectId);
            for (String[] spec : GITFLOW_MERGE_SPECS) {
                MergeSpec mergeSpec = new MergeSpec(projectConfig, projectId, spec[0], spec[1]);
                mergeSpecRepository.save(mergeSpec);
            }
        }

        projectConfig = projectConfigRepository.save(projectConfig);
        return projectConfig;
    }

    public void deleteProjectConfig(ProjectConfig projectConfig) throws GitLabApiException {

        // Delete the hook from the GitLab server
        Integer projectId = projectConfig.getProjectId();
        gitLabApi.getProjectApi().deleteHook(projectId, projectConfig.getHookId());
        logger.info("Deleted Simple-CR webhook from GitLab project, projectId={}", projectId);
      
        // Delete the ProjectConfig from the database
        projectConfigRepository.delete(projectConfig);
    }

    public List<MergeSpec> getMergeSpecs(ProjectConfig projectConfig) {

        // Load the Simple-CR merge specs for the project
        List<MergeSpec> mergeSpecs = mergeSpecRepository.findByProjectId(projectConfig.getProjectId());
        return (mergeSpecs);
    }

    public MergeSpec addMergeSpec(ProjectConfig projectConfig, String branchRegex, String targetBranchRegex) {

        MergeSpec mergeSpec = new MergeSpec();
        mergeSpec.setProjectConfig(projectConfig);
        mergeSpec.setProjectId(projectConfig.getProjectId());
        mergeSpec.setBranchRegex(branchRegex);
        mergeSpec.setTargetBranchRegex(targetBranchRegex);
        mergeSpec = mergeSpecRepository.save(mergeSpec);
        return mergeSpec;
    }

    public MergeSpec deleteMergeSpec(ProjectConfig projectConfig, String branchRegex, String targetBranchRegex) {

        Optional<MergeSpec> mergeSpec = mergeSpecRepository.find(
                projectConfig.getProjectId(), branchRegex, targetBranchRegex);
        if (mergeSpec.isPresent()) {
            mergeSpecRepository.delete(mergeSpec.get());
            return mergeSpec.get();
        } else {
            return null;
        }
    }
}
