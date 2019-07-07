
package org.gitlab4j.simplecr.controller;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectHook;
import org.gitlab4j.simplecr.beans.AppResponse;
import org.gitlab4j.simplecr.config.SimpleCrConfiguration;
import org.gitlab4j.simplecr.model.MergeSpec;
import org.gitlab4j.simplecr.model.ProjectConfig;
import org.gitlab4j.simplecr.model.ProjectConfig.MailToType;
import org.gitlab4j.simplecr.repository.MergeSpecRepository;
import org.gitlab4j.simplecr.repository.ProjectConfigRepository;
import org.gitlab4j.simplecr.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminController
 *
 * This class provides an endpoint for Simple-CR admin functionality, providing for the management of
 * the GitLab repository project being monitored for pushes and merge requests.
 */
@RestController
@RequestMapping("admin")
public class AdminController {

    @Autowired
    private SimpleCrConfiguration appConfig;

    @Autowired
    private ProjectConfigRepository projectConfigRepository;

    @Autowired
    private MergeSpecRepository mergeSpecRepository;

    @Autowired
    private GitLabApi gitLabApi;

    private Logger logger = LoggerFactory.getLogger(AdminController.class);

    // Define the merge specs for a standard GitFlow Git Woirkflow
    private static final String[][]  GITFLOW_MERGE_SPECS = {
            {"^feature.*", "^develop"},
            {"^bug.*"    , "^develop"},
            {"^develop"  , "^master"},
            {"^develop"  , "^release.*"},
            {"^hotfix.*" , "^master"},
            {"^release.*", "^master"}
    };

    @GetMapping(path = "/{groupName}/{projectName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AppResponse<?> getProjectConfig(
            @PathVariable("groupName") String groupName,
            @PathVariable("projectName") String projectName) {

        logger.info("List code review setup for project, group={}, project={}", groupName, projectName);

        // Get the specified project
        Project project;
        try {
            project = gitLabApi.getProjectApi().getProject(groupName, projectName);
        } catch (GitLabApiException glae) {
           logger.warn("Problem getting project info, error={}", glae.getMessage());
           return AppResponse.getMessageResponse(false, "Could not load project info from GitLab server");
        }

        // Load the Simple-CR project config
        int projectId = project.getId();
        ProjectConfig projectConfig = projectConfigRepository.findByProjectId(projectId);
        if (projectConfig == null) {
            return (AppResponse.getMessageResponse(false, "Project is not configured in the Simple-CR system."));
        }

        return (AppResponse.getDataResponse(true, projectConfig));
    }

    @PostMapping(path = "/{groupName}/{projectName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AppResponse<?> addProjectConfig(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable("groupName") String groupName,
            @PathVariable("projectName") String projectName,
            @RequestParam(name = "enabled", defaultValue = "true") Boolean enabled,
            @RequestParam(name = "mail_to_type", defaultValue = "project") String mailTo,
            @RequestParam(name = "additional_mail_to", required = false) String additionalMailTo,
            @RequestParam(name = "exclude_mail_to", required = false) String excludeMailTo,
            @RequestParam(name = "include_default_mail_to", defaultValue = "false") Boolean includeDefaultMailTo,
            @RequestParam(name = "gitflow_merge_specs", defaultValue = "false") Boolean gitflowMergeSpecs) {

        logger.info("Add code review setup for project, group={}, project={}", groupName, projectName);

        // Get the specified project
        Project project;
        try {
            project = gitLabApi.getProjectApi().getProject(groupName, projectName);
        } catch (GitLabApiException glae) {
           logger.warn("Problem getting project info, error={}", glae.getMessage());
           return AppResponse.getMessageResponse(false, "Could not load project info from GitLab server");
        }

        // See if we already have this project in the system
        int projectId = project.getId();
        ProjectConfig projectConfig = projectConfigRepository.findByProjectId(projectId);
        if (projectConfig != null) {
            logger.warn("This project is already in the system, use PUT to make modifications, group={}, project={}", groupName, projectName);
            String message = "This project is already in the system, use PUT to make modifications.";
            return (AppResponse.getMessageResponse(AppResponse.Status.NO_ACTION, message));
        }

        MailToType mailToType = MailToType.findByString(mailTo);
        if (mailToType == null) {
            return (AppResponse.getMessageResponse(false, "Invalid mail_to[" + mailTo + "]"));
        }

        try {

            projectConfig = new ProjectConfig();
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

        } catch (Exception e) {
            return (AppResponse.getMessageResponse(false, e.getMessage()));
        }

        // Build the Url to the simple-cr webhook
        String webhookUrl = StringUtils.buildUrlString(appConfig.getSimpleCrUrl(), request.getContextPath(), "webhook");

        // Add the webhook to the project at the GitLab server
        try {
            ProjectHook hookConfig = new ProjectHook().withPushEvents(true).withMergeRequestsEvents(true);
            ProjectHook projectHook = gitLabApi.getProjectApi().addHook(projectId, webhookUrl, hookConfig, false, "simple-cr-" + projectConfig.getId());
            projectConfig.setHookId(projectHook.getId());
            projectConfigRepository.save(projectConfig);
        } catch (GitLabApiException glae) {

            try {
                projectConfigRepository.delete(projectConfig);
            } catch (Exception ignore) {}

            return (AppResponse.getMessageResponse(false, glae.getMessage()));
        }

        String createdUrl = request.getRequestURL().toString();
        logger.info("Created project config for {}/{}, location={}", groupName, projectName, createdUrl);

        response.addHeader(HttpHeaders.LOCATION, createdUrl);
        response.setStatus(HttpStatus.CREATED.value());
        return (AppResponse.getDataResponse(true, projectConfig));
    }

    @PutMapping(path = "/{groupName}/{projectName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AppResponse<?> updateProjectConfig(
        HttpServletRequest request,
        HttpServletResponse response,
        @PathVariable("groupName") String groupName,
        @PathVariable("projectName") String projectName,
        @RequestParam(name = "enabled", required = false) Boolean enabled,
        @RequestParam(name = "mail_to_type", required = false) String mailTo,
        @RequestParam(name = "additional_mail_to", required = false) String additionalMailTo,
        @RequestParam(name = "exclude_mail_to", required = false) String excludeMailTo,
        @RequestParam(name = "include_default_mail_to", required = false) Boolean includeDefaultMailTo,
        @RequestParam(name = "getflow_merge_specs", defaultValue = "false") Boolean gitflowMergeSpecs) {

        logger.info("Update code review setup for project, group={}, project={}", groupName, projectName);

        // Get the specified project
        Project project;
        try {
            project = gitLabApi.getProjectApi().getProject(groupName, projectName);
        } catch (GitLabApiException glae) {
           logger.error("Problem getting project info, error={}", glae.getMessage());
           return AppResponse.getMessageResponse(false, "Could not load project info from GitLab server");
        }

        // Make sure we have this project in the system
        int projectId = project.getId();
        ProjectConfig projectConfig = projectConfigRepository.findByProjectId(projectId);
        if (projectConfig == null) {
            logger.warn("This project was not in the system, group={}, project={}", groupName, projectName);
            String message = "The specified project was not found in the Simple-CR system.";
            return (AppResponse.getMessageResponse(false, message));
        }

        MailToType mailToType = null;
        if (mailTo != null && mailTo.trim().length() > 0) {
            mailToType = MailToType.findByString(mailTo);
            if (mailToType == null) {
                return (AppResponse.getMessageResponse(false, "Invalid mail_to[" + mailTo + "]"));
            }

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

            try {

                logger.info("Clearing existing merge specs, projectId={}", projectId);
                mergeSpecRepository.clearMergeSpecs(projectId);

                logger.info("Creating MergeSpecs for GitFlow workflow, projectId={}", projectId);
                for (String[] spec : GITFLOW_MERGE_SPECS) {
                    MergeSpec mergeSpec = new MergeSpec(projectConfig, projectId, spec[0], spec[1]);
                    mergeSpecRepository.save(mergeSpec);
                }

            } catch (Exception e) {
                return (AppResponse.getMessageResponse(false, e.getMessage()));
            }

            logger.info("Finished creating MergeSpecs for GitFlow workflow, projectId={}", projectId); 
        }

        try {
            projectConfig = projectConfigRepository.save(projectConfig);
        } catch (Exception e) {
            return (AppResponse.getMessageResponse(false, e.getMessage()));
        }

        String createdUrl = request.getRequestURL().toString();
        String message = "Updated project config for " + groupName + "/" + projectName;
        logger.info("{}, location={}", message, createdUrl);
        response.addHeader(HttpHeaders.LOCATION, createdUrl);
        return (AppResponse.getResponse(AppResponse.Status.OK, message, projectConfig));
    }

    @DeleteMapping(path = "/{groupName}/{projectName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AppResponse<?> deleteProjectConfig(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable("groupName") String groupName,
            @PathVariable("projectName") String projectName) {

        logger.info("Delete code review setup for project, group={}, project={}", groupName, projectName);

        // Get the specified project
        Project project;
        try {
            project = gitLabApi.getProjectApi().getProject(groupName, projectName);
        } catch (GitLabApiException glae) {
           logger.warn("Problem getting project info, error={}", glae.getMessage());
           return AppResponse.getMessageResponse(false, "Could not load project info from GitLab server");
        }

        // Make sure we have this project in the system
        int projectId = project.getId();
        ProjectConfig projectConfig = projectConfigRepository.findByProjectId(projectId);
        if (projectConfig == null) {
            logger.warn("This project was not in the system, group={}, project={}", groupName, projectName);
            String message = "The specified project was not found in the Simple-CR the system.";
            return (AppResponse.getMessageResponse(false, message));
        }

        // Delete the hook from the GitLab server
        try {
            gitLabApi.getProjectApi().deleteHook(projectId, projectConfig.getHookId());
        } catch (GitLabApiException glae) {
            logger.warn("Problem deleting webhook, error={}", glae.getMessage());
            return AppResponse.getMessageResponse(false, "Could not delete project webhook from GitLab server");
        }

        // We got here then delete the record
        try {
            projectConfigRepository.delete(projectConfig);
        } catch (Exception e) {
            return (AppResponse.getMessageResponse(false, e.getMessage()));
        }

        String message = "Deleted project config for " + groupName + "/" + projectName;
        logger.info(message);
        return (AppResponse.getMessageResponse(true, message));
    }

    @GetMapping(path = "/{groupName}/{projectName}/merge_specs", produces = MediaType.APPLICATION_JSON_VALUE)
    public AppResponse<?> getMergeSpecs(
            @PathVariable("groupName") String groupName,
            @PathVariable("projectName") String projectName) {

        logger.info("List code review merge specs for project, group={}, project={}", groupName, projectName);

        // Get the specified project
        Project project;
        try {
            project = gitLabApi.getProjectApi().getProject(groupName, projectName);
        } catch (GitLabApiException glae) {
           logger.warn("Problem getting project info, error={}", glae.getMessage());
           return AppResponse.getMessageResponse(false, "Could not load project info from GitLab server");
        }

        // Make sure we have this project in the system
        int projectId = project.getId();
        ProjectConfig projectConfig = projectConfigRepository.findByProjectId(projectId);
        if (projectConfig == null) {      
            String message = "The specified project was not found in the Simple-CR system";
            logger.warn("{}, group={}, project={}", message, groupName, projectName);
            return (AppResponse.getMessageResponse(false, message + "."));
        }

        // Load the Simple-CR merge specs for the project
        List<MergeSpec> mergeSpecs = this.mergeSpecRepository.findByProjectId(projectId);
        if (mergeSpecs == null) {
            return (AppResponse.getMessageResponse(false, "Project has no merge specs."));
        }

        return (AppResponse.getDataResponse(true, mergeSpecs));
    }

    @PostMapping(path = "/{groupName}/{projectName}/merge_specs", produces = MediaType.APPLICATION_JSON_VALUE)
    public AppResponse<?> addMergeSpec(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable("groupName") String groupName,
            @PathVariable("projectName") String projectName,
            @RequestParam("branch_regex") String branchRegex,
            @RequestParam("target_branch") String targetBranch) {

        logger.info("Add code review merge spec for project, group={}, project={}, branchRegex={}, targetBranch={}",
                groupName, projectName, branchRegex, targetBranch);

        // Get the specified project
        Project project;
        try {
            project = gitLabApi.getProjectApi().getProject(groupName, projectName);
        } catch (GitLabApiException glae) {
           logger.warn("Problem getting project info, error={}", glae.getMessage());
           return AppResponse.getMessageResponse(false, "Could not load project info from GitLab server");
        }

        // Make sure we have this project in the system
        int projectId = project.getId();
        ProjectConfig projectConfig = projectConfigRepository.findByProjectId(projectId);
        if (projectConfig == null) {      
            String message = "The specified project was not found in the Simple-CR system";
            logger.warn("{}, group={}, project={}", message, groupName, projectName);
            return (AppResponse.getMessageResponse(false, message + "."));
        }

        MergeSpec mergeSpec = null;
        try {

            mergeSpec = new MergeSpec();
            mergeSpec.setProjectConfig(projectConfig);
            mergeSpec.setProjectId(projectId);
            mergeSpec.setBranchRegex(branchRegex);
            mergeSpec.setTargetBranchRegex(targetBranch);
            mergeSpec = mergeSpecRepository.save(mergeSpec);

        } catch (Exception e) {
            return (AppResponse.getMessageResponse(false, e.getMessage()));
        }

        return (AppResponse.getDataResponse(true, mergeSpec));
    }

    @DeleteMapping(path = "/{groupName}/{projectName}/merge_specs", produces = MediaType.APPLICATION_JSON_VALUE)
    public AppResponse<?> deleteMergeSpec(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable("groupName") String groupName,
            @PathVariable("projectName") String projectName,
            @RequestParam("branch_regex") String branchRegex,
            @RequestParam("target_branch") String targetBranch) {

        logger.info("Delete code review merge spec for project, group={}, project={}, branchRegex={}, targetBranch={}",
                groupName, projectName, branchRegex, targetBranch);

        // Get the specified project
        Project project;
        try {
            project = gitLabApi.getProjectApi().getProject(groupName, projectName);
        } catch (GitLabApiException glae) {
           logger.warn("Problem getting project info, error={}", glae.getMessage());
           return AppResponse.getMessageResponse(false, "Could not load project info from GitLab server");
        }

        // Make sure we have this project in the system
        int projectId = project.getId();
        ProjectConfig projectConfig = projectConfigRepository.findByProjectId(projectId);
        if (projectConfig == null) {      
            String message = "The specified project was not found in the Simple-CR system";
            logger.warn("{}, group={}, project={}", message, groupName, projectName);
            return (AppResponse.getMessageResponse(false, message + "."));
        }

        Optional<MergeSpec> mergeSpec = mergeSpecRepository.find(projectId, branchRegex, targetBranch);
        if (mergeSpec.isPresent()) {
            try {
                mergeSpecRepository.delete(mergeSpec.get());
            } catch (Exception e) {
                return (AppResponse.getMessageResponse(false, e.getMessage()));
            }
        }

        String message = "Deleted specified merge spec for project " + groupName + "/" + projectName;
        logger.info(message);
        return (AppResponse.getMessageResponse(true, message));
    }
}
