package org.gitlab4j.simplecr.controller;

import javax.servlet.http.HttpServletRequest;

import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.webhook.Event;
import org.gitlab4j.simplecr.service.GitLabWebHookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


/**
 * This is the webhook endpoint for GitLab webhook events that are sent
 * when there is a new or updated push, merge request or issue.
 */
@RestController
@RequestMapping("webhook")
public class GitLabWebHookController {

    @Autowired
    private GitLabWebHookService gitLabWebHookService;

    @PostMapping(path = {"", "/issue", "/merge_request", "/push"},
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String processEvent(HttpServletRequest request) {

        try {
            Event event = gitLabWebHookService.handleRequest(request);
            return (event != null ? String.format("Processed '%s' event", event.getObjectKind()) : "");
        } catch (GitLabApiException glae) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, glae.getMessage(), glae);
        }
    }
}
