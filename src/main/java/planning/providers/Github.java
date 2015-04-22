package planning.providers;

import io.dropwizard.auth.Auth;
import org.eclipse.egit.github.core.client.IGitHubConstants;
import planning.resources.OAuth;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Provider;

import org.eclipse.egit.github.core.client.GitHubClient;

import com.google.common.base.Optional;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.Parameter;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

@Provider
public class Github implements InjectableProvider<Auth, Parameter> {
    private final String githubHostname;

    @Context
    private HttpServletRequest request;

    public Github(final String githubHostname) {
        this.githubHostname = githubHostname;
    }

    @Override
    public ComponentScope getScope() {
        return ComponentScope.PerRequest;
    }

    @Override
    public Injectable<GitHubClient> getInjectable(final ComponentContext ic, final Auth a, final Parameter c) {
        return new GithubInjectable(this.request);
    }

    private final class GithubInjectable extends AbstractHttpContextInjectable<GitHubClient> {
        private final HttpServletRequest request;

        public GithubInjectable(final HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public GitHubClient getValue(final HttpContext c) {
            final Optional<String> token = readToken(this.request.getSession(false));
            if(token.isPresent()) {
                final GitHubClient client = new GitHubClient(IGitHubConstants.HOST_DEFAULT.equals(Github.this.githubHostname) ? "api.": "" + Github.this.githubHostname);
                client.setOAuth2Token(token.get());
                return client;
            }
            final URI login = UriBuilder.fromResource(OAuth.class).build();
            throw new WebApplicationException(Response.temporaryRedirect(login).build());
        }

        private Optional<String> readToken(final HttpSession session) {
            return (session == null) ? Optional.absent() : Optional.fromNullable((String)session.getAttribute("token"));
        }
    }
}