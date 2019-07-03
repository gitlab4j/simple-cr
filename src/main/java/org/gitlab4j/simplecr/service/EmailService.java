package org.gitlab4j.simplecr.service;

import static org.springframework.util.StringUtils.hasText;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Author;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.gitlab4j.simplecr.config.SimpleCrConfiguration;
import org.gitlab4j.simplecr.model.ProjectConfig;
import org.gitlab4j.simplecr.model.ProjectConfig.MailToType;
import org.gitlab4j.simplecr.utils.HashUtils;
import org.gitlab4j.simplecr.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * 
 */
@Service
public class EmailService {

    @Autowired
    private SimpleCrConfiguration appConfig;

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private TemplateEngine htmlTemplateEngine;

    @Autowired
    private GitLabApi gitLabApi;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${spring.mail.host}")
    private String smtpHost;

    @Value("${spring.mail.port}")
    private int smtpPort;

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private static final String CODE_REVIEW_TEMPLATE = "email/code-review";
    private static final String CODE_REVIEW_SUBJECT = "Your Branch Push";

    private static final String MERGE_REQUEST_TEMPLATE = "email/merge-request";
    private static final String MERGE_REQUEST_SUBJECT = "Code Review/Merge Request";

    public EmailService() {
    }

    public boolean sendMergeRequestEmail(ProjectConfig projectConfig, MergeRequest mergeRequest) {

        if (!isEmailEnabled()) {
            return (false);
        }

        Integer projectId = mergeRequest.getProjectId();
        Project project;
        try {
            project = gitLabApi.getProjectApi().getProject(projectId);
        } catch (GitLabApiException gle) {
            logger.error("Problem getting project info, httpStatus=" + gle.getHttpStatus() + ", error=" + gle.getMessage(), gle);
            return (false);
        }

        if (project.getId() == null || !project.getId().equals(projectId)) {
            logger.error("Problem getting project info, projectId=" + projectId + ", project.id=" + project.getId());
            return (false);
        }

        Author author = mergeRequest.getAuthor();
        String branch = mergeRequest.getSourceBranch();
        String projectName = project.getName().trim();
        String group = project.getNamespace().getName().trim();
        String mergeRequestLink = appConfig.getGitLabWebUrl() + "/" + group + "/" + projectName + "/merge_requests/" + mergeRequest.getIid();

        Collection<String> reviewers = getReviewers(projectConfig, project.getNamespace().getId(), author);
        if (reviewers == null || reviewers.size() < 1) {
            logger.warn("No reviewers are configured for this project.");
            return (false);
        }

        final Context context = new Context();
        context.setVariable("gitlabWebUrl", appConfig.getGitLabWebUrl());
        context.setVariable("mergeRequestLink", mergeRequestLink);
        context.setVariable("mergeRequest", mergeRequest);
        context.setVariable("projectName", projectName);
        context.setVariable("project", project);
        context.setVariable("branch", branch);
        context.setVariable("group", group);
        context.setVariable("author", author);

        try {

            String htmlContent = htmlTemplateEngine.process(MERGE_REQUEST_TEMPLATE, context);        
            send(reviewers, MERGE_REQUEST_SUBJECT, htmlContent);
            return (true);

        } catch (Exception e) {
            logger.error("Something went wrong while sending code review email, error=" + e.getMessage(), e);
            return (false);
        }
    }

    boolean sendCodeReviewEmail(User user, Project project, String branch) {

        if (!isEmailEnabled()) {
            return (false);
        }

        /*
         * Set up all the data for the code review request email and send it to the user that
         * initiated the branch push.
         */
        Integer userId = user.getId();
        Integer projectId = project.getId();
        String encodedBranch = StringUtils.urlEncodeString(branch);
        String signature = HashUtils.makeHash(HashUtils.SHORT_HASH, projectId, branch, userId);
        String codeReviewLink = StringUtils.buildUrlString(appConfig.getSimpleCrUrl(), contextPath,
                projectId.toString(), encodedBranch, userId.toString(), signature);

        final Context context = new Context();
        context.setVariable("codeReviewLink", codeReviewLink);
        context.setVariable("gitlabWebUrl", appConfig.getGitLabWebUrl());
        context.setVariable("projectName", project.getName());
        context.setVariable("project", project);
        context.setVariable("branch", branch);
        context.setVariable("group", project.getNamespace().getName());
        context.setVariable("user", user);

        try {

            String htmlContent = htmlTemplateEngine.process(CODE_REVIEW_TEMPLATE, context);        
            send(user.getEmail(), user.getName(), CODE_REVIEW_SUBJECT, htmlContent);
            return (true);

        } catch (Exception e) {
            logger.error("Something went wrong while sending code review email, error=" + e.getMessage(), e);
            return (false);
        }
    }

    void send(String email, String name, String subject, String htmlContent)
            throws MailException, MessagingException, UnsupportedEncodingException {
        EmailAddress emailAddress = new EmailAddress(email, name);
        sendMail(Arrays.asList(emailAddress), subject, htmlContent);
    }

    void send(Collection<String> emailList, String subject, String htmlContent)
            throws MailException, MessagingException, UnsupportedEncodingException {

        List<EmailAddress> toEmailList = new ArrayList<EmailAddress>(emailList.size());
        for (String email : emailList) {
            toEmailList.add(new EmailAddress(email, null));
        }

        sendMail(toEmailList, subject, htmlContent);
    }

    private boolean isEmailEnabled() {
        return (hasText(smtpHost) && smtpPort > 0);
    }

    private void sendMail(List<EmailAddress> toEmailList, String subject, String htmlContent)
            throws MailException, MessagingException, UnsupportedEncodingException {

        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name());

        for (EmailAddress toEmail : toEmailList) {
            helper.addTo(toEmail.email, toEmail.name);
        }

        helper.setFrom(appConfig.getFromEmail(),  appConfig.getFromName());
        helper.setText(htmlContent, true);
        helper.setSubject(subject);
        emailSender.send(message);
    }

    private class EmailAddress {

        private String email;
        private String name;

        EmailAddress(String email, String name) {
            this.email = email;
            this.name = name;
        }
    }

    /**
     * Get the set of reviewer email addresses for the ProjectConfig. This list will always exclude the
     * author's email unless the author is the only reviewer.
     * 
     * @param projectConfig
     * @param author
     * @return
     */
    private Collection<String> getReviewers(ProjectConfig projectConfig, int groupId, Author author) {

        TreeSet<String> reviewers = new TreeSet<String>();
        List<Member> members = null;
        if (MailToType.GROUP.equals(projectConfig.getMailToType())) {

            try {
                members = gitLabApi.getGroupApi().getMembers(groupId);
                logger.info("GROUP reviewer list, numMembers=" + (members == null ? 0 : members.size()));
            } catch (GitLabApiException e) {
                logger.error("Something went wrong while getting group members, error=" + e.getMessage(), e);
            }

        } else if (MailToType.PROJECT.equals(projectConfig.getMailToType())) {

            try {
                members = gitLabApi.getProjectApi().getMembers(projectConfig.getProjectId());
                logger.info("PROJECT reviewer list, numMembers=" + (members == null ? 0 : members.size()));
            } catch (GitLabApiException e) {
                logger.error("Something went wrong while getting project members, error=" + e.getMessage(), e);
            }
        }

        if (members != null) {
            for (Member member : members) {
                Optional<User> optionalUser = gitLabApi.getUserApi().getOptionalUser(member.getId());
                if (optionalUser.isPresent()) {
                    String email = optionalUser.get().getEmail();
                    if (email != null && email.trim().length() > 0)
                        reviewers.add(email);
                }
            }
        }

        List<String> additionalMailToList = projectConfig.getAdditionalMailTo();
        if (additionalMailToList != null) {
            reviewers.addAll(additionalMailToList);
        }

        if (projectConfig.getIncludeDefaultMailTo()) {
            reviewers.addAll(appConfig.getDefaultReviewers());
        }

        if (reviewers == null || reviewers.size() == 0) {
            return (null);
        }

        // Get the list of excluded emails and remove them from the reviewers list
        List<String> excludeEmails = projectConfig.getExcludeMailTo();
        if (excludeEmails != null) {
            reviewers.removeAll(excludeEmails);
        }

        // If the list > 1 in length make sure the author is not in the list
        if (reviewers.size() > 1) {
            String authorEmail = author.getEmail();
            reviewers.remove(authorEmail);
        }

        return (reviewers);
    }
}
