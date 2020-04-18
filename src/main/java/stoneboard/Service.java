package stoneboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.support.GenericApplicationContext;

import static stoneboard.ServiceConfig.GITHUB_COM_HOST;
import static stoneboard.ServiceConfig.ENV_GITHUB_HOSTNAME;

@SpringBootApplication
public class Service {
  public Service(@Autowired final GenericApplicationContext context) {
    final GithubClientFactory factory =
        isGithubCom() ? GithubClientFactory.dotCom : GithubClientFactory.enterprise;
    context.registerBean(
        GithubClientFactory.class.getName(), GithubClientFactory.class, () -> factory);
  }

  public static void main(final String[] args) {
    new SpringApplicationBuilder(Service.class)
        .profiles(isGithubCom() ? "githubCom" : "githubEnterprise")
        .run(args);
  }

  private static boolean isGithubCom() {
    final String host = System.getenv(ENV_GITHUB_HOSTNAME);
    return host == null || host.isEmpty() || GITHUB_COM_HOST.equals(host);
  }
}
