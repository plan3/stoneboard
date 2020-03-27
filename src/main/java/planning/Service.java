package planning;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import planning.providers.Github;
import planning.providers.GithubExceptionMapper;
import planning.resources.Milestones;
import planning.resources.OAuth;

import java.util.Optional;

import static com.google.common.base.Strings.emptyToNull;
import static java.lang.System.getenv;
import static java.util.Arrays.asList;

public class Service extends Application<Configuration> {

  private static String env(final String variable) {
    return optionalEnv(variable)
        .orElseThrow(
            () -> new IllegalArgumentException("No value for required variable " + variable));
  }

  private static Optional<String> optionalEnv(final String variable) {
    return Optional.ofNullable(emptyToNull(getenv(variable)));
  }

  public static void main(final String[] args) throws Exception {
    new Service().run(args);
  }

  @Override
  public void initialize(final Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(new AssetsBundle());
    bootstrap.addBundle(new ViewBundle());
  }

  @Override
  public void run(final Configuration configuration, final Environment environment)
      throws Exception {
    // FIXME: Doesn't scale to 1+ dynos
    final String githubHostname = optionalEnv("GITHUB_HOSTNAME").orElse("github.com");
    environment.servlets().setSessionHandler(new SessionHandler(new HashSessionManager()));
    environment
        .jersey()
        .register(new Milestones(asList(env("REPOSITORIES").split(",")), githubHostname));
    environment
        .jersey()
        .register(new OAuth(env("GITHUB_CLIENT_ID"), env("GITHUB_CLIENT_SECRET"), githubHostname));
    environment.jersey().register(new Github(githubHostname));
    environment.jersey().register(GithubExceptionMapper.class);
  }
}
