package org.gitlab4j.simplecr.service;


import java.util.Date;
import java.util.List;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.WebHookListener;
import org.gitlab4j.api.webhook.WebHookManager;
import org.gitlab4j.simplecr.config.SimpleCrConfiguration;
import org.gitlab4j.simplecr.model.MergeSpec;
import org.gitlab4j.simplecr.model.ProjectConfig;
import org.gitlab4j.simplecr.model.Push;
import org.gitlab4j.simplecr.repository.MergeSpecRepository;
import org.gitlab4j.simplecr.repository.ProjectConfigRepository;
import org.gitlab4j.simplecr.repository.PushRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


/**
 * This class listens for Web Hook events and processes them. Basically a push event
 * for a branch will result in a code review request email being sent to whoever pushed the branch.
 *
 * We track the lifecycle of the code review request here and update a Push record. This makes sure
 * we are not doing additional requests on the same branch that has yet to be reviewed.
 *
 */
@Service
public class GitLabWebHookService extends WebHookManager implements WebHookListener {

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

    private static final Logger logger = LoggerFactory.getLogger(GitLabWebHookService.class);

    public GitLabWebHookService() {
        addListener(this);
    }

    /**
     * This method is called when a merge request is either created or changes state. We use it to update the Push record
     * for the branch. This allows us to eliminate sending out multiple notifications.
     *
     * @param mergeRequestEvent
     */
    @Override
    public void onMergeRequestEvent(MergeRequestEvent mergeRequestEvent) {

        final GitLabApi gitLabApi = new GitLabApi(appConfig.getGitLabApiUrl(), appConfig.getGitLabApiToken());

        ObjectAttributes attributes = mergeRequestEvent.getObjectAttributes();
        String branchName = attributes.getSourceBranch();
        int userId = attributes.getAuthorId();
        int projectId = attributes.getTargetProjectId();
        int mergeRequestId = attributes.getIid();
        String mergeState = attributes.getState();
        String mergeStatus = attributes.getMergeStatus();

        logger.info("Merge request notification received, userId={}, " +
                "projectId={}, mergRequestId={}, mergeStatus={}, mergeState={}",
                userId, projectId, mergeRequestId, mergeStatus, mergeState);

        // We only operate on merged or closed state changes
        if (!"merged".equals(mergeState) && !"closed".equals(mergeState)) {
            return;
        }

        // Make sure the merge request is valid
        MergeRequest mergeRequest = null;
        try {
            mergeRequest = gitLabApi.getMergeRequestApi().getMergeRequest(projectId, attributes.getId());
        } catch (GitLabApiException glae) {
            logger.error("Problem getting merge request info, httpStatus={}, error={}",
                    glae.getHttpStatus(), glae.getMessage());
            return;
        }

        // Now find and update the push record

        // Make sure we have a push record that has not been submitted for code review
        List<Push> pushList = pushRepository.find(userId, projectId, branchName, mergeRequestId);
        if (pushList == null || pushList.size() == 0) {
            logger.warn("Could not locate push record for merge request, " +
                    "userId={}, projectId={}, branch={}, mergeRequestId={}",
                    userId, projectId, branchName, mergeRequestId);
            return;
        }

        // If the push record is already updated, we are done here
        Push push = pushList.get(0);
        if (mergeState.equals(push.getMergeState())) {
            logger.info("Push record already updated, userId={}, projectId={}, " +
                    "mergRequestId={}, mergeStatus={}, mergeState={}",
                    userId, projectId, mergeRequestId, mergeStatus, mergeState);
            return;
        }

        // If the MR was merged, get the merged by ID
        int mergedById = 0;
        if ("merged".equals(mergeState)) {
            User user = mergeRequestEvent.getUser();
            if (user != null) {
                if (user.getId() != null) {
                    mergedById = user.getId();
                } else if (!StringUtils.isEmpty(user.getUsername())) {
                    try {
                        List<User> users = gitLabApi.getUserApi().findUsers(user.getUsername());
                        if (users != null && !users.isEmpty())
                            mergedById = users.get(0).getId();
                    } catch (GitLabApiException gle) {
                        logger.warn("Error trying to determine merged by ID, message={}", gle.getMessage());
                    }
                }
            }

            if (mergedById == 0) {
                mergedById = (mergeRequest.getAssignee() != null ? mergeRequest.getAssignee().getId() : 0);
            }
        }

        push.setMergeStatusDate(attributes.getUpdatedAt());
        push.setMergeState(mergeState);
        push.setMergeStatus(mergeStatus);
        push.setMergedById(mergedById);
        pushRepository.save(push);
        logger.info("Updated push record, userId={}, projectId={}, mergRequestId={}, mergeStatus={}, mergeState={}",
                userId, projectId, mergeRequestId, mergeStatus, mergeState);
    }

    /**
     * This method is called when a push notification is received. We make sure the state of all the associated objects
     * are correct and if so create a Push record and send an email to the user with a link to a code review submittal form.
     * We also make sure that we don't send multiple emails to the user for additional pushes of a branch that is
     * already pending review.
     *
     * @param pushEvent
     */
    @Override
    public void onPushEvent(PushEvent pushEvent) {

        final GitLabApi gitLabApi = new GitLabApi(appConfig.getGitLabApiUrl(), appConfig.getGitLabApiToken());

        int userId = pushEvent.getUserId();
        int projectId = pushEvent.getProjectId();
        String branchName = pushEvent.getBranch();
        logger.info("A branch has been pushed, userId={}, projectId={}, branch={}",
                userId, projectId, branchName);

        ProjectConfig projectConfig = projectConfigRepository.findByProjectId(projectId);
        if (projectConfig == null) {
            logger.info("This project is not in the Simple-CR system, projectId=%d", projectId);
            return;
        }

        if (StringUtils.isEmpty(branchName)) {
            logger.warn("Branch name is either null or not valid, ref=%s", pushEvent.getRef());
            return;
        }

        if (branchName.equals("master")) {
            logger.info("No code reviews are done on master.");
            return;
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
            logger.info("The pushed branch is not configured to trigger Simple-CR, pushed branch={}", branchName);
            return;
        }

        Project project = null;
        try {
            project = gitLabApi.getProjectApi().getProject(projectId);
        } catch (GitLabApiException glae) {
            logger.error("Problem getting project info, httpStatus=%d, error=%s",
                    glae.getHttpStatus(), glae.getMessage());
            return;
        }

        User user;
        try {
            user = gitLabApi.getUserApi().getUser(userId);
            if (StringUtils.isEmpty(user.getEmail()))
                user.setEmail(pushEvent.getUserEmail());
        } catch (GitLabApiException gle) {
            logger.error("Problem getting user info, httpStatus={}, error={}",
                    gle.getHttpStatus(), gle.getMessage());
            return;
        }

        // Make sure that the branch is still valid (not deleted).
        try {
            gitLabApi.getRepositoryApi().getBranch(projectId, branchName);
        } catch (GitLabApiException gle) {
            logger.error("Problem getting branch info, httpStatus={}, error={}",
                    gle.getHttpStatus(), gle.getMessage());
            return;
        }

        // If after is all "0" this indicates that this notification is for the deletion of that branch.
        String after = pushEvent.getAfter();
        if (after != null && after.matches("^[0]+$")) {
            logger.info(
                    "The branch has been deleted nothing to do here, before={}, after={}",
                    pushEvent.getBefore(), after);
            return;
        }

        // Make sure that we DO NOT have a pending code review for this branch
        List<Push> pushList = pushRepository.findPendingReviews(userId, projectId, branchName);
        if (pushList != null && pushList.size() > 0) {
            logger.info("The branch is already pending review and merge, userId={}, projectId={}, branch={}",
                    userId, projectId, branchName);
            return;
        }

        // Make sure we DO NOT have a push record that has not been submitted for code review
        pushList = pushRepository.find(userId, projectId, branchName, 0);
        if (pushList != null && pushList.size() > 0) {
            logger.info("Branch push notification has already been sent, userId={}, projectId={}, branch={}",
                    userId, projectId, branchName);
            return;
        }

        // Add a Push record for this push event
        Push push = new Push();
        push.setReceivedAt(new Date());
        push.setUserId(userId);
        push.setProjectId(projectId);
        push.setBranch(branchName);
        push.setBefore(pushEvent.getBefore());
        push.setAfter(pushEvent.getAfter());
        push.setMergeRequestId(0);
        pushRepository.save(push);

        // Send the code review email
        emailService.sendCodeReviewEmail(user, project, branchName);
    }
}
