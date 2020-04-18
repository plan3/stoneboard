package planning.resources;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import planning.GithubClientFactory;
import planning.ServiceConfig;

@RestController
public class Milestones {

  private final GithubClientFactory factory;
  private final ServiceConfig config;
  private final OAuth2AuthorizedClientService service;

  public Milestones(
      @Autowired final GithubClientFactory factory,
      @Autowired final ServiceConfig config,
      @Autowired final OAuth2AuthorizedClientService service) {
    this.factory = requireNonNull(factory);
    this.config = requireNonNull(config);
    this.service = requireNonNull(service);
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
            put("githubHostname", Milestones.this.config.hostname());
            put("repositories", Milestones.this.config.repositories());
            put("user", user);
          }
        });
  }

  @GetMapping("milestones/{org}/{repo}")
  public List<Map<String, Object>> milestones(
      @PathVariable("org") final String org, @PathVariable("repo") final String repo)
      throws IOException {
    final GitHub client = this.factory.create(this.service, this.config.hostname());
    final ArrayList<Map<String, Object>> result = new ArrayList<>();
    final GHRepository repository = client.getRepository(String.format("%s/%s", org, repo));
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
