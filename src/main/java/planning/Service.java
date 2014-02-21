package planning;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;
import static java.lang.System.getenv;
import static java.util.Arrays.asList;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import planning.providers.Github;
import planning.providers.GithubExceptionMapper;
import planning.resources.Milestones;
import planning.resources.OAuth;

import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;

public class Service extends Application<Configuration> {

    @Override
    public void initialize(final Bootstrap<Configuration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle());
        bootstrap.addBundle(new ViewBundle());
    }

    @Override
    public void run(final Configuration configuration, final Environment environment) throws Exception {
        // FIXME: Doesn't scale to 1+ dynos
        environment.servlets().setSessionHandler(new SessionHandler(new HashSessionManager()));
        environment.jersey().register(new Milestones(asList(env("REPOSITORIES").split(","))));
        environment.jersey().register(new OAuth(env("GITHUB_CLIENT_ID"), env("GITHUB_CLIENT_SECRET")));
        environment.jersey().register(Github.class);
        environment.jersey().register(GithubExceptionMapper.class);
    }

    private static String env(final String variable) {
        return checkNotNull(emptyToNull(getenv(variable)), "No value for: " + variable);
    }

    public static void main(final String[] args) throws Exception {
        new Service().run(args);
    }
}
