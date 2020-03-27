package planning.resources;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

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

  private GitHub ghClient(final String hostname) throws IOException {
    // Pry out the client ID and setup a Github API client with it
    final var oauthToken =
        (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    final var client =
        service.loadAuthorizedClient(
            oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
    return GitHub.connectUsingOAuth(hostname, client.getAccessToken().getTokenValue());
  }

  @GetMapping("/")
  public ModelAndView index(@AuthenticationPrincipal OAuth2User principal) {
    return new ModelAndView(
        "index",
        Map.of(
            "githubHostname",
            this.hostname,
            "repositories",
            this.repositories,
            "user",
            Map.of(
                "login",
                principal.getAttribute("login"),
                "avatarUrl",
                principal.getAttribute("avatar_url"))));
  }

  @GetMapping("milestones/{org}/{repo}")
  public List<Map<String, Object>> milestones(
      @PathVariable("org") final String org, @PathVariable("repo") final String repo)
      throws IOException {
    final var result = new ArrayList<Map<String, Object>>();
    final var repository = ghClient(hostname).getRepository(String.format("%s/%s", org, repo));
    for (final GHMilestone milestone : repository.listMilestones(GHIssueState.OPEN)) {
      final var participants = participants(repository, milestone);
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
                Map.of(
                    "url",
                    u.getUrl().toString(),
                    "avatarUrl",
                    u.getAvatarUrl(),
                    "login",
                    u.getLogin()))
        .collect(Collectors.toList());
  }

  private Map<String, Object> milestone(
      final String org,
      final String repo,
      final GHMilestone milestone,
      final List<Map<String, String>> participants) {
    return ofEntries(
        entry("organisation", org),
        entry("repository", repo),
        entry("id", UUID.randomUUID().toString()),
        entry("url", milestone.getUrl().toString()),
        entry("title", milestone.getTitle()),
        entry("number", milestone.getNumber()),
        entry("openIssues", milestone.getOpenIssues()),
        entry("closedIssues", milestone.getClosedIssues()),
        entry("description", Objects.toString(milestone.getDescription(), "")),
        entry("slug", String.format("%s/%s/%d", org, repo, milestone.getNumber())),
        entry("assignees", participants));
  }
}
