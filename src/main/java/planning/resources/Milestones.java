package planning.resources;

import static java.util.Collections.emptySet;
import static java.util.Collections.synchronizedSet;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

import io.dropwizard.auth.Auth;
import io.dropwizard.views.View;
import planning.views.IndexView;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.MilestoneService;
import org.eclipse.egit.github.core.service.UserService;

import com.google.common.collect.ImmutableMap;

@Path("/")
@Produces(TEXT_HTML)
public class Milestones {

    private final List<String> repositories;

    public Milestones(final List<String> repositories) {
        this.repositories = repositories;
    }

    @GET
    public View index(@Auth final GitHubClient client) throws IOException {
        final UserService users = new UserService(client);
        return new IndexView(users.getUser(), this.repositories);
    }

    @GET
    @Path("milestones/{org}/{repo}")
    @Produces(APPLICATION_JSON)
    public List<Map<String, Object>> milestones(@Auth final GitHubClient client,
                                                @PathParam("org") final String org,
                                                @PathParam("repo") final String repo) throws IOException {
        final ConcurrentMap<Integer, Set<Map<String, String>>> assignees = new ConcurrentHashMap<>();
        new IssueService(client)
                .getIssues(org, repo, ImmutableMap.of("milestone", "*", "state", "open"))
                .parallelStream()
                .forEach((issue) -> {
                    final User assignee = issue.getAssignee();
                    if(assignee != null) {
                        final int milestone = issue.getMilestone().getNumber();
                        assignees.putIfAbsent(milestone, synchronizedSet(new HashSet<>()));
                        // User doesn't implement equals/hashCode because a Set<User> had been too simple...
                        assignees.get(milestone).add(ImmutableMap.of(
                                "url", assignee.getUrl(),
                                "avatarUrl", assignee.getAvatarUrl(),
                                "login", assignee.getLogin()));
                    }
                });
        return new MilestoneService(client)
                .getMilestones(org, repo, "open")
                .parallelStream()
                .map(milestone -> merge(milestone, org, repo, assignees))
                .collect(Collectors.toList());
    }

    private static Map<String, Object> merge(final Milestone milestone,
                                             final String org,
                                             final String repo,
                                             final Map<Integer, Set<Map<String, String>>> assignees) {
        return ImmutableMap.<String, Object>builder()
                .put("organisation", org)
                .put("repository", repo)
                .put("id", UUID.randomUUID().toString())
                .put("url", milestone.getUrl())
                .put("title", milestone.getTitle())
                .put("number", milestone.getNumber())
                .put("openIssues", milestone.getOpenIssues())
                .put("closedIssues", milestone.getClosedIssues())
                .put("description", milestone.getDescription())
                .put("slug", String.format("%s/%s/%d", org, repo, milestone.getNumber()))
                .put("assignees", Optional.ofNullable(assignees.get(milestone.getNumber())).orElse(emptySet()))
                .build();
    }
}
