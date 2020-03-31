package planning;

import org.kohsuke.github.GitHub;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.io.IOException;

public interface GithubClientFactory {

  GithubClientFactory dotCom =
      (hostname, client, token) ->
          GitHub.connectUsingOAuth(hostname, client.getAccessToken().getTokenValue());

  GithubClientFactory enterprise =
      (hostname, client, token) -> {
        final String login = token.getPrincipal().getAttribute("login");
        return GitHub.connectToEnterpriseWithOAuth(
            "https://" + hostname + "/api/v3", login, client.getAccessToken().getTokenValue());
      };

  default GitHub create(final OAuth2AuthorizedClientService service, final String hostname) {
    // Pry out the access token and setup a Github API client with it
    final OAuth2AuthenticationToken token =
        (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    final OAuth2AuthorizedClient client =
        service.loadAuthorizedClient(token.getAuthorizedClientRegistrationId(), token.getName());
    try {
      return create(hostname, client, token);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  };

  GitHub create(
      final String hostname,
      final OAuth2AuthorizedClient client,
      final OAuth2AuthenticationToken token)
      throws IOException;
}
