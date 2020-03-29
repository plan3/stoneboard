package planning.resources;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import planning.ServiceConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@RestController
public class Milestones {

  private final List<String> repositories;
  private final String hostname;
  @Autowired private OAuth2AuthorizedClientService service;

  @Autowired
  public Milestones(final ServiceConfig config) throws IOException {
    this(config.repositories(), config.hostname());
  }

  public Milestones(final List<String> repositories, final String hostname) throws IOException {
    this.repositories = repositories;
    this.hostname = hostname;
  }

  private GitHub ghClient() throws IOException {
    // Pry out the client ID and setup a Github API client with it
    final OAuth2AuthenticationToken token =
        (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    final OAuth2AuthorizedClient client =
        service.loadAuthorizedClient(token.getAuthorizedClientRegistrationId(), token.getName());
    return GitHub.connectUsingOAuth(this.hostname, client.getAccessToken().getTokenValue());
  }

  @GetMapping("/")
  public ModelAndView index(@AuthenticationPrincipal OAuth2User principal) {
    final Map<String, String> user =
        new HashMap<String, String>() {
          {
            put("login", principal.getAttribute("login").toString());
            put("avatarUrl", principal.getAttribute("avatar_url").toString());
          }
        };
    return new ModelAndView(
        "index",
        new HashMap<String, Object>() {
          {
            put("githubHostname", Milestones.this.hostname);
            put("repositories", Milestones.this.repositories);
            put("user", user);
          }
        });
  }

  @GetMapping("milestones/{org}/{repo}")
  public List<Map<String, Object>> milestones(
      @PathVariable("org") final String org, @PathVariable("repo") final String repo)
      throws IOException {
    final ArrayList<Map<String, Object>> result = new ArrayList<>();
    final GHRepository repository = ghClient().getRepository(String.format("%s/%s", org, repo));
    for (final GHMilestone milestone : repository.listMilestones(GHIssueState.OPEN)) {
      final List<Map<String, String>> participants = participants(repository, milestone);
      result.add(milestone(org, repo, milestone, participants));
    }
    return result;
  }

  private List<Map<String, String>> participants(
      final GHRepository repository, final GHMilestone milestone) throws IOException {
    return repository
        .getIssues(GHIssueState.ALL, milestone)
        .parallelStream()
        .map(GHIssue::getAssignees)
        .flatMap(List::stream)
        .distinct()
        .map(
            u ->
                new HashMap<String, String>() {
                  {
                    put("url", u.getUrl().toString());
                    put("avatarUrl", u.getAvatarUrl());
                    put("login", u.getLogin());
                  }
                })
        .collect(toList());
  }

  private Map<String, Object> milestone(
      final String org,
      final String repo,
      final GHMilestone milestone,
      final List<Map<String, String>> participants) {
    return new HashMap<String, Object>() {
      {
        put("organisation", org);
        put("repository", repo);
        put("id", UUID.randomUUID().toString());
        put("url", milestone.getUrl().toString());
        put("title", milestone.getTitle());
        put("number", milestone.getNumber());
        put("openIssues", milestone.getOpenIssues());
        put("closedIssues", milestone.getClosedIssues());
        put("description", Objects.toString(milestone.getDescription(), ""));
        put("slug", String.format("%s/%s/%d", org, repo, milestone.getNumber()));
        put("assignees", participants);
      }
    };
  }
}
