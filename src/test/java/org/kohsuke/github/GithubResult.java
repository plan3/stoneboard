package org.kohsuke.github;

import static java.util.Collections.singletonList;

import java.util.Iterator;
import java.util.List;

// PagedIterator's constructor is package private, hence this whole hoop jumping
public class GithubResult<T> extends PagedIterable<T> {

  private final Iterator<T[]> actual;

  public GithubResult(final T... items) {
    this(singletonList(items).iterator());
  }

  public GithubResult(final List<T> items) {
    this((T[]) items.toArray());
  }

  public GithubResult(final Iterator<T[]> actual) {
    this.actual = actual;
  }

  @Override
  public PagedIterator<T> _iterator(int i) {
    return new PagedIterator<T>(this.actual, null) {};
  }
}
