package planning;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.support.GenericApplicationContext;

import static planning.ServiceConfig.DEFAULT_HOST;
import static planning.ServiceConfig.HOSTNAME_ENV;

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
    final String host = System.getenv(HOSTNAME_ENV);
    return host == null || host.isEmpty() || DEFAULT_HOST.equals(host);
  }
}
