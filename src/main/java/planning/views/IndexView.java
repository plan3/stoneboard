package planning.views;

import io.dropwizard.views.View;

import java.util.List;

import org.eclipse.egit.github.core.User;

public class IndexView extends View {
    private final User user;
    private final List<String> repositories;

    public IndexView(final User user, final List<String> repositories) {
        super("index.mustache");
        this.user = user;
        this.repositories = repositories;
    }

    public User getUser() {
        return this.user;
    }

    public List<String> getRepositories() {
        return this.repositories;
    }
}
