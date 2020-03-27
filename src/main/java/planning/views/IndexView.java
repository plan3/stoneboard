package planning.views;

import io.dropwizard.views.View;
import org.eclipse.egit.github.core.User;

import java.util.List;

public class IndexView extends View {
  private final User user;
  private final List<String> repositories;
  private final String githubHostname;

  public IndexView(final User user, final List<String> repositories, final String githubHostname) {
    super("index.mustache");
    this.user = user;
    this.repositories = repositories;
    this.githubHostname = githubHostname;
  }

  public User getUser() {
    return this.user;
  }

  public List<String> getRepositories() {
    return this.repositories;
  }

  public String getGithubHostname() {
    return githubHostname;
  }
}
