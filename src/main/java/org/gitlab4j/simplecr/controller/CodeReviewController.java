
package org.gitlab4j.simplecr.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.gitlab4j.simplecr.beans.AppResponse;
import org.gitlab4j.simplecr.beans.CodeReviewInfo;
import org.gitlab4j.simplecr.config.SimpleCrConfiguration;
import org.gitlab4j.simplecr.model.MergeSpec;
import org.gitlab4j.simplecr.model.ProjectConfig;
import org.gitlab4j.simplecr.model.Push;
import org.gitlab4j.simplecr.repository.MergeSpecRepository;
import org.gitlab4j.simplecr.repository.ProjectConfigRepository;
import org.gitlab4j.simplecr.repository.PushRepository;
import org.gitlab4j.simplecr.service.EmailService;
import org.gitlab4j.simplecr.utils.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * CodeReviewController
 * 
 * This class provides the endpoints for Simple-CR web and mobile client.
 */
@RestController
@RequestMapping("")
public class CodeReviewController {

    @Autowired
    private SimpleCrConfiguration appConfig;

    @Autowired
    private ProjectConfigRepository projectConfigRepository;

    @Autowired
    private PushRepository pushRepository;
    
    @Autowired
    private MergeSpecRepository mergeSpecRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private GitLabApi gitLabApi;

    private Logger logger = LoggerFactory.getLogger(CodeReviewController.class);

    
    @GetMapping(path = "/{projectId}/{branchName}/{userId}/{signature}", produces = MediaType.TEXT_HTML_VALUE)
    public @ResponseBody byte[] index(
            HttpServletResponse response,
            @PathVariable("projectId") int projectId,
            @PathVariable("branchName") String branchName,
            @PathVariable("userId") int userId,
            @PathVariable("signature") String signature) throws IOException {
        
        logger.info("index: projectId={}, branchName={}, userId={}, signature={}", projectId, branchName, userId, signature);

        if (!HashUtils.isValidHash(signature, HashUtils.SHORT_HASH, projectId, branchName, userId)) {
            logger.warn("WARNING: invalid signature,  projectId={}, branchName={}, userId={}, signature={}",
                    projectId, branchName, userId, signature);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return null;
        }

        try (InputStream htmlIn = CodeReviewController.class.getResourceAsStream("/static/simple-cr.html")) {
            return StreamUtils.copyToByteArray(htmlIn);
        }
    }

    @GetMapping(path = "/load", produces = MediaType.APPLICATION_JSON_VALUE)
    public AppResponse<?> load() {
        logger.warn("load() called without parameters");
        return (AppResponse.getMessageResponse(false, "No branch specified, nothing to review here."));
    }

    @GetMapping(path = "/load/{projectId}/{branchName}/{userId}/{signature}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AppResponse<?> load(
            HttpServletResponse response,
            @PathVariable("projectId") int projectId,
            @PathVariable("branchName") String branchName,
            @PathVariable("userId") int userId,
            @PathVariable("signature") String signature) throws IOException {

        logger.info("load: projectId={}, branchName={}, userId={}, signature={}", projectId, branchName, userId, signature);

        if (!HashUtils.isValidHash(signature, HashUtils.SHORT_HASH, projectId, branchName, userId)) {
            logger.warn("Invalid signature");
            return (AppResponse.getMessageResponse(false, "Bad code review data request"));
        }

        // Make sure we have this project in the system and it is enabled
        ProjectConfig projectConfig = projectConfigRepository.findByProjectId(projectId);
        if (projectConfig == null) {
            String message = "The specified project was not found in Simple-CR system";
            logger.warn("{}, projectId={}", message, projectId);
            return (AppResponse.getMessageResponse(false, message + "."));
        }

        // Make sure there is a merge spec that matches the branch name
        boolean foundBranchMatch = false;
        List<MergeSpec> mergeSpecs = this.mergeSpecRepository.findByProjectConfigId(projectConfig.getId());
        if (mergeSpecs != null) {
            for (MergeSpec mergeSpec : mergeSpecs) {
                if (branchName.matches(mergeSpec.getBranchRegex())) {
                    foundBranchMatch = true;
                    break;
                }
            }
        }

        if (!foundBranchMatch) {
            String message = "The specified branch is not configured to trigger Simple-CR";
            logger.warn("{}, branh={}", message, branchName);
            return (AppResponse.getMessageResponse(false, message + "."));
        }

        Project project;
        try {
            project = gitLabApi.getProjectApi().getProject(projectId);
        } catch (GitLabApiException glae) {
            logger.error("Problem getting project info, httpStatus={}, error={}", glae.getHttpStatus(), glae.getMessage());
            return (AppResponse.getMessageResponse(false, "Could not load project info for code review"));
        }

        if (project.getId() == null || !project.getId().equals(projectId)) {
            logger.error("Problem getting project info, projectId={}, project.id={}", projectId, project.getId());
            return (AppResponse.getMessageResponse(false, "Could not load project info for code review"));
        }

        User user;
        try {
            user = gitLabApi.getUserApi().getUser(userId);
        } catch (GitLabApiException glae) {
            logger.error("Problem getting user info, httpStatus={}, error={}", glae.getHttpStatus(), glae.getMessage());
            return (AppResponse.getMessageResponse(false, "Could not load project info for code review"));
        }

        if (user.getId() == null || !user.getId().equals(userId)) {
            logger.error("Problem getting user info, userId={}, user.id={}", userId, user.getId());
            return (AppResponse.getMessageResponse(false, "Could not load user info for code review"));
        }

        // We default the status to success, with an empty statusText message
        AppResponse.Status status = AppResponse.Status.OK;
        String statusText = null;

        // Make sure that we don't have a pending code review for this branch
        List<Push> pushList = pushRepository.findPendingReviews(userId, projectId, branchName);
        String title = null;
        String description = null;
        List<String> targetBranches = null;
        String targetBranch = null;
        if (pushList != null && pushList.size() > 0) {

            logger.info("This branch is already pending review, userId={}, projectId={}, branch={}", userId, projectId, branchName);
            statusText = "This branch push is already pending review.";
            status = AppResponse.Status.NO_ACTION;

            try {
                MergeRequest mergeRequest = gitLabApi.getMergeRequestApi().getMergeRequest(projectId, pushList.get(0).getMergeRequestId());
                title = mergeRequest.getTitle();
                description = mergeRequest.getDescription();
                targetBranch = mergeRequest.getTargetBranch();
                targetBranches = new ArrayList<String>();
                targetBranches.add(targetBranch);
            } catch (GitLabApiException glae) {
                logger.warn("Problem getting merge request info, , httpStatus={}, error={}", glae.getHttpStatus(), glae.getMessage());
            }

        } else {

            // Make sure we have a push record that has not been submitted for code review
            pushList = pushRepository.find(userId, projectId, branchName, 0);
            if (pushList == null || pushList.size() == 0) {

                logger.info("No branch pushes are available for review, userId={}, projectId={}, branch={}", userId, projectId, branchName);
                pushList = pushRepository.find(userId, projectId, branchName);
                if (pushList == null || pushList.size() == 0) {
                    statusText = "This branch push has already been reviewed.";
                    status = AppResponse.Status.NO_ACTION;
                } else {
                    Push push = pushList.get(0);
                    statusText = "This branch push has already been submitted for review, current state is '" + push.getMergeState() + "'.";
                    status = AppResponse.Status.NO_ACTION;
                }

            } else {

                // Get the list of available target branches for the branch to be merged into
                targetBranches = new ArrayList<String>();
                for (MergeSpec mergeSpec : mergeSpecs) {
                    if (branchName.matches(mergeSpec.getBranchRegex())) {
                        targetBranches.add(mergeSpec.getTargetBranch());
                    }
                }

                if (targetBranches.size() > 0) {
                    targetBranch = targetBranches.get(0);
                }
            }
        }

        CodeReviewInfo codeReviewInfo = new CodeReviewInfo();
        codeReviewInfo.setGroup(project.getNamespace().getName());
        codeReviewInfo.setProjectId(projectId);
        codeReviewInfo.setProjectName(project.getName());
        codeReviewInfo.setProjectUrl(project.getWebUrl());
        codeReviewInfo.setSourceBranch(branchName);
        codeReviewInfo.setUserId(userId);
        codeReviewInfo.setName(user.getName());
        codeReviewInfo.setEmail(user.getEmail());
        codeReviewInfo.setGitlabWebUrl(appConfig.getGitLabWebUrl());
        codeReviewInfo.setTargetBranch(targetBranch);
        codeReviewInfo.setTargetBranches(targetBranches);
        codeReviewInfo.setTitle(title);
        codeReviewInfo.setDescription(description);
        return (AppResponse.getResponse(status, statusText, codeReviewInfo));
    }

    @PostMapping(path = "/submit", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public AppResponse<?> submitMergeRequest(
            HttpServletResponse response,
            @RequestParam(name = "merge_request[user_id]") int userId,
            @RequestParam(name = "merge_request[source_project_id]") int sourceProjectId,
            @RequestParam(name = "merge_request[source_branch]") String sourceBranch,
            @RequestParam(name = "merge_request[target_project_id]") int targetProjectId,
            @RequestParam(name = "merge_request[target_branch]") String targetBranch,
            @RequestParam(name = "merge_request[title]") String title,
            @RequestParam(name = "merge_request[description]") String description) {

        logger.info("submit: user_id={}, source_project_id={}, sourceBranch={}," +
                "targetProjectId={}, targetBranch={}, title={}, description={}",
                userId , sourceProjectId , sourceBranch, targetProjectId, targetBranch, title, description);

        // Make sure we have this project in the system and it is enabled
        ProjectConfig projectConfig = projectConfigRepository.findByProjectId(targetProjectId);
        if (projectConfig == null) {
            logger.info("The target project is not in the simple-cr system, targetProjectId={}", targetProjectId);
            String message = "The specified project was not found in Simple-CR system.";
            return (AppResponse.getMessageResponse(AppResponse.Status.NO_ACTION, message));
        }

        if (!projectConfig.getEnabled()) {
            logger.info("The target project does not have code reviews enabled, targetProjectId=" + targetProjectId);
            String message = "The target project does not have code reviews enabled.";
            return (AppResponse.getMessageResponse(AppResponse.Status.NO_ACTION, message));
        }

        // Make sure we have a push record that has not been submitted for code review
        List<Push> pushList = pushRepository.find(userId, sourceProjectId, sourceBranch, 0);
        if (pushList == null || pushList.size() == 0) {
            logger.info("No branch pushes are available for review, userId={}, projectId={}, branch={}", 
                    userId, sourceProjectId, sourceBranch);
            return (AppResponse.getMessageResponse(AppResponse.Status.NO_ACTION, "This branch is already pending review."));
        }

        MergeRequest mergeRequest;
        try {
            mergeRequest = gitLabApi.getMergeRequestApi().createMergeRequest(targetProjectId, sourceBranch, targetBranch, title, description, null);
        } catch (GitLabApiException glae) {
            logger.error("Problem creating merge request, httpStatus={}, error={}", glae.getHttpStatus(), glae.getMessage());
            return (AppResponse.getMessageResponse(AppResponse.Status.NO_ACTION, "This branch has already been merged or deleted"));
        }

        // Update the Push record
        pushRepository.setMergeRequestInfo(pushList.get(0).getId(), mergeRequest.getIid(),
                mergeRequest.getCreatedAt(), mergeRequest.getState(), mergeRequest.getMergeStatus());

        // Send the merge request email to all the reviewers
        emailService.sendMergeRequestEmail(projectConfig, mergeRequest);

        return (AppResponse.getMessageResponse(true, "Your request for code review and merge has been submitted."));
    }
}
