var TreeNode = function(attrs) {
  this.title = attrs.title;
  this.slug = attrs.slug;
  this.url = attrs.url;
  this.number = attrs.number;
  this.organisation = attrs.organisation;
  this.repository = attrs.repository;
  this.description = attrs.description;
  this.openIssues = attrs.openIssues;
  this.closedIssues = attrs.closedIssues;
  this.children = (attrs.children || []);
  this.dependencies = extractDependencies(this);
  this.assignees = attrs.assignees;
  this.inTree = false;

  function extractDependencies(node) {
    var matches = /\[(.+)\]/g.exec(node.description);
    var dependencies = [];
    if(matches !== null) {
      dependencies = matches[1].split(/,| /);
      return dependencies.map(function(slug) {
        return slug.trim();
      }).filter(function(slug) {
        // Make sure we don't depend on ourself
        return slug !== node.slug;
      });
    }
    return [];
  }
};

var tree = function(milestones) {
  var nodes = milestones.map(function(m) {
    return new TreeNode(m);
  });

  function findNodeBySlug(slug) {
    var result = nodes.filter(function(node) {
      return node.slug === slug;
    });
    return result[0];
  };

  function removeUnresolvedDependencies() {
    nodes.forEach(function(node) {
      node.dependencies.forEach(function(dependency) {
        if(!findNodeBySlug(dependency)) {
          var index = node.dependencies.indexOf(dependency);
          node.dependencies.splice(index, 1);
        }
      });
    });
  };

  function setupChildren() {
    var roots = nodes.filter(function(node) {
      if(node.dependencies.length === 0) {
        return node;
      }
      return false;
    });
    roots.forEach(function(rootNode) {
      findChildren(rootNode);
    });
  };

  function findChildren(parent) {
    parent.children = nodes.filter(function(node) {
      if(node.dependencies.indexOf(parent.slug) !== -1) {
        return findChildren(node);
      }
    });
    return parent;
  };

  function buildGraph(nodes, accumulator) {
    if(accumulator === undefined) {
      var accumulator = {
        nodes: [],
        links: []
      };
    }
    nodes.forEach(function(node) {
      if(node.inTree === false) {
        accumulator.nodes.push({
          title: node.title,
          slug: node.slug,
          root: (node.dependencies.length === 0),
          url: "http://github.com/" + node.organisation + "/" + node.repository + "/issues?milestone=" + node.number,
          repoUrl: "http://github.com/" + node.organisation + "/" + node.repository,
          editLink: "http://github.com/" + node.organisation + "/" + node.repository + "/milestones/" + node.number + "/edit",
          organisation: node.organisation,
          repository: node.repository,
          openIssues: node.openIssues,
          closedIssues: node.closedIssues,
          progressPercentage: Math.round((node.closedIssues / (node.openIssues + node.closedIssues)) * 100),
          childrenSize: node.children.length,
          assignees: node.assignees
        });
        node.inTree = true;
      }
      node.children.forEach(function(child) {
        var inLinks = accumulator.links.some(function(link) {
          return link.source == node.slug && link.target == child.slug;
        });
        if(!inLinks) {
          accumulator.links.push({
            source: node.slug,
            target: child.slug
          });
        }
        buildGraph(node.children, accumulator);
      });
    });
    return accumulator;
  }

  return {
    toGraph: function() {
      removeUnresolvedDependencies();
      setupChildren();
      graph = buildGraph(nodes);
      return graph;
    }
  };
};

var renderer = function(graphData) {
  var graph = new dagreD3.Digraph(),
    svg = d3.select("svg"),
    svgGroup = d3.select("svg g"),
    nodeHeight = 60,
    nodeSeperation = 10;

  function setGraphDimension() {
    svg.attr({
      "width": "100%",
      "height": window.innerHeight - 80
    });
  }

  function addNodes() {
    graphData.nodes.forEach(function(node) {
      graph.addNode(node.slug, { label: nodeHtml(node) });
    });
  }

  function nodeHtml(node) {
    var totalIssues = parseInt(node.openIssues)+parseInt(node.closedIssues);
    var outer = $("<div/>", {
      class: "milestone " + (node.assignees.length > 0 ? "has-assignees" : ""),
      css: {
        "background": "linear-gradient(to right, rgba(30,255,0, .4)" + node.progressPercentage + "%, white 0%)"
      }
    });
    $("<a/>", {
      href: node.url,
      html: node.title
    }).appendTo(outer);
    $("<a/>", {
      href: node.editLink,
      html: "✍",
      class: "edit"
    }).appendTo(outer);
    $("<br/>").appendTo(outer);
    $("<span/>", {
      html: $("<a/>", {
          href: node.repoUrl,
          html: node.organisation + "/" + node.repository
        }).prop("outerHTML") + " ⚑ Open issues " + node.openIssues + "/" + totalIssues
    }).appendTo(outer);
    if(node.assignees.length > 0) {
      var assignees = $("<ul/>", {
        class: "assignees"
      });
      node.assignees.forEach(function(assignee) {
        $("<li/>", {
          html: $("<a/>", {
            href: "http://www.github.com/" + assignee.login,
            html: $("<img/>", {
              src: assignee.avatarUrl,
              title: assignee.login
            })
          })
        }).appendTo(assignees);
      });
      assignees.appendTo(outer);
    }
    return outer.prop("outerHTML");
  }

  function addLinks() {
    graphData.links.forEach(function(link) {
      graph.addEdge(null, link.source, link.target);
    });
  }

  return {
    render: function() {
      var graphRenderer = new dagreD3.Renderer(),
        layout = dagreD3.layout()
          .nodeSep(nodeSeperation)
          .rankDir("LR");
      setGraphDimension();
      addNodes();
      addLinks();
      graphRenderer.layout(layout).run(graph, svgGroup);
    }
  }
}

var load = function(slug) {
  var org = slug.split("/")[0];
  var repo = slug.split("/")[1];
  $("#milestones-spinner").show();
  $("#orgModal").modal("hide");
  $("svg").hide();
  $.cookie("slug", slug);
  $("#current-org").html("(" + slug + ")");
  $.getJSON("/milestones/" + slug).done(function(data) {
    $("svg").show();
    graphData = tree(data).toGraph();
    renderer(graphData).render();
    $("#milestones-spinner").hide();
  }).fail(function() {
    alert("There was a problem with the request.");
  });
}

var selectOrg = function() {
  $("#orgModal").modal("show");
  $(".modal-dialog").css("z-index", "1500");
}

$(document).ready(function() {
  if($.cookie("slug")) {
    load($.cookie("slug"));
  } else {
    selectOrg();
  }
});