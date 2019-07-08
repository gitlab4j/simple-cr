
package org.gitlab4j.simplecr.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.simplecr.beans.AppResponse;
import org.gitlab4j.simplecr.config.SimpleCrConfiguration;
import org.gitlab4j.simplecr.model.MergeSpec;
import org.gitlab4j.simplecr.model.ProjectConfig;
import org.gitlab4j.simplecr.model.ProjectConfig.MailToType;
import org.gitlab4j.simplecr.service.ProjectConfigService;
import org.gitlab4j.simplecr.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
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
    private ProjectConfigService projectConfigService;

    private Logger logger = LoggerFactory.getLogger(AdminController.class);

    @GetMapping(path = "/{groupName}/{projectName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AppResponse<?> getProjectConfig(
            @PathVariable("groupName") String groupName,
            @PathVariable("projectName") String projectName) {

        logger.info("List code review setup for project, group={}, project={}", groupName, projectName);

        // Get the specified project config
        ProjectConfig projectConfig;
        try {

            projectConfig = projectConfigService.getProjectConfig(groupName, projectName);
            if (projectConfig != null) {
                return (AppResponse.getDataResponse(true, projectConfig));
            } else {
                return (AppResponse.getMessageResponse(false, "Project is not configured in the Simple-CR system."));
            }

        } catch (GitLabApiException glae) {
           logger.warn("Problem getting project info, error={}", glae.getMessage());
           return AppResponse.getMessageResponse(false, "Could not load project info from GitLab server");
        }
    }

    @PostMapping(path = "/{groupName}/{projectName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AppResponse<?> addProjectConfig(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable("groupName") String groupName,
            @PathVariable("projectName") String projectName,
            @RequestParam(name = "enabled", defaultValue = "true") Boolean enabled,
            @RequestParam(name = "mail_to_type", defaultValue = "project") MailToType mailToType,
            @RequestParam(name = "additional_mail_to", required = false) String additionalMailTo,
            @RequestParam(name = "exclude_mail_to", required = false) String excludeMailTo,
            @RequestParam(name = "include_default_mail_to", defaultValue = "false") Boolean includeDefaultMailTo,
            @RequestParam(name = "gitflow_merge_specs", defaultValue = "false") Boolean gitflowMergeSpecs) {

        logger.info("Add code review setup for project, group={}, project={}", groupName, projectName);

        // Make sure the project exists in the GitLab server
        Project project;
        try {
            project = projectConfigService.getProject(groupName, projectName);
        } catch (GitLabApiException glae) {
            logger.warn("Problem getting project info, error={}", glae.getMessage());
            return AppResponse.getMessageResponse(false, "Could not load project info from GitLab server");
        }

        // Make sure the specified project config does not exist
        ProjectConfig projectConfig = projectConfigService.getProjectConfig(project);
        if (projectConfig != null) {
            logger.warn("This project is already in the system, use PUT to make modifications, group={}, project={}", groupName, projectName);
            String message = "This project is already in the system, use PUT to make modifications.";
            return (AppResponse.getMessageResponse(AppResponse.Status.NO_ACTION, message));
        }

        // Create the ProjectConfig with the loaded project
        try {
            String webhookUrl = StringUtils.buildUrlString(appConfig.getSimpleCrUrl(), request.getContextPath(), "webhook");
            projectConfig = projectConfigService.addProjectConfig(project, enabled, mailToType,
                    additionalMailTo, excludeMailTo, includeDefaultMailTo, gitflowMergeSpecs, webhookUrl);
        } catch (Exception e) {
            return (AppResponse.getMessageResponse(false, e.getMessage()));
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
        @RequestParam(name = "mail_to_type", required = false) MailToType mailToType,
        @RequestParam(name = "additional_mail_to", required = false) String additionalMailTo,
        @RequestParam(name = "exclude_mail_to", required = false) String excludeMailTo,
        @RequestParam(name = "include_default_mail_to", required = false) Boolean includeDefaultMailTo,
        @RequestParam(name = "getflow_merge_specs", defaultValue = "false") Boolean gitflowMergeSpecs) {

        logger.info("Update code review setup for project, group={}, project={}", groupName, projectName);

        // Get the specified project config
        ProjectConfig projectConfig;
        try {

            projectConfig = projectConfigService.getProjectConfig(groupName, projectName);
            if (projectConfig == null) {
                return (AppResponse.getMessageResponse(false, "Project is not configured in the Simple-CR system."));
            }

        } catch (GitLabApiException glae) {
           logger.warn("Problem getting project info, error={}", glae.getMessage());
           return AppResponse.getMessageResponse(false, "Could not load project info from GitLab server");
        }

        try {
            projectConfig = projectConfigService.updateProjectConfig(projectConfig, enabled, mailToType,
                    additionalMailTo, excludeMailTo, includeDefaultMailTo, gitflowMergeSpecs);
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

        // Get the specified project config
        ProjectConfig projectConfig;
        try {

            projectConfig = projectConfigService.getProjectConfig(groupName, projectName);
            if (projectConfig == null) {
                return (AppResponse.getMessageResponse(false, "Project is not configured in the Simple-CR system."));
            }

        } catch (GitLabApiException glae) {
           logger.warn("Problem getting project info, error={}", glae.getMessage());
           return AppResponse.getMessageResponse(false, "Could not load project info from GitLab server");
        }

        // Delete the project config
        try {
            projectConfigService.deleteProjectConfig(projectConfig);
        } catch (GitLabApiException glae) {
            logger.warn("Problem deleting project config, error={}", glae.getMessage());
            return AppResponse.getMessageResponse(false, "Could not delete project configuration, error=" + glae.getMessage());
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

        // Get the specified project config
        ProjectConfig projectConfig;
        try {

            projectConfig = projectConfigService.getProjectConfig(groupName, projectName);
            if (projectConfig == null) {
                return (AppResponse.getMessageResponse(false, "Project is not configured in the Simple-CR system."));
            }

        } catch (GitLabApiException glae) {
           logger.warn("Problem getting project info, error={}", glae.getMessage());
           return AppResponse.getMessageResponse(false, "Could not load project info from GitLab server");
        }

        List<MergeSpec> mergeSpecs = projectConfigService.getMergeSpecs(projectConfig);
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
            @RequestParam("target_branch_regex") String targetBranchRegex) {

        logger.info("Add code review merge spec for project, group={}, project={}, branchRegex={}, targetBranchRegex={}",
                groupName, projectName, branchRegex, targetBranchRegex);

        // Get the specified project config
        ProjectConfig projectConfig;
        try {

            projectConfig = projectConfigService.getProjectConfig(groupName, projectName);
            if (projectConfig == null) {
                return (AppResponse.getMessageResponse(false, "Project is not configured in the Simple-CR system."));
            }

        } catch (GitLabApiException glae) {
           logger.warn("Problem getting project info, error={}", glae.getMessage());
           return AppResponse.getMessageResponse(false, "Could not load project info from GitLab server");
        }

        MergeSpec mergeSpec = projectConfigService.addMergeSpec(projectConfig, branchRegex, targetBranchRegex);
        return (AppResponse.getDataResponse(true, mergeSpec));
    }

    @DeleteMapping(path = "/{groupName}/{projectName}/merge_specs", produces = MediaType.APPLICATION_JSON_VALUE)
    public AppResponse<?> deleteMergeSpec(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable("groupName") String groupName,
            @PathVariable("projectName") String projectName,
            @RequestParam("branch_regex") String branchRegex,
            @RequestParam("target_branch_regex") String targetBranchRegex) {

        logger.info("Delete code review merge spec for project, group={}, project={}, branchRegex={}, targetBranchRegex={}",
                groupName, projectName, branchRegex, targetBranchRegex);

        // Get the specified project config
        ProjectConfig projectConfig;
        try {

            projectConfig = projectConfigService.getProjectConfig(groupName, projectName);
            if (projectConfig == null) {
                return (AppResponse.getMessageResponse(false, "Project is not configured in the Simple-CR system."));
            }

        } catch (GitLabApiException glae) {
           logger.warn("Problem getting project info, error={}", glae.getMessage());
           return AppResponse.getMessageResponse(false, "Could not load project info from GitLab server");
        }

       MergeSpec mergeSpec = projectConfigService.deleteMergeSpec(projectConfig, branchRegex, targetBranchRegex);
       if (mergeSpec == null) {
           String message = "Could not find the specified merge spec for project " + groupName + "/" + projectName;
           logger.warn(message);
           return (AppResponse.getMessageResponse(false, message));
       }

       String message = "Deleted the specified merge spec for project " + groupName + "/" + projectName;
       logger.info(message);
       return (AppResponse.getMessageResponse(true, message));
    }

    @InitBinder
    public void initBinder(final WebDataBinder webdataBinder) {
        webdataBinder.registerCustomEditor(MailToType.class, new MailToType.Converter());
    }
}
