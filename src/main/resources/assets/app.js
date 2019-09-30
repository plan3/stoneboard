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
    var matches = /\[(.+)]/g.exec(node.description);
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

var tree = function(milestones, githubHostname) {
  var githubUrl = "https://" + githubHostname;

  var nodes = milestones.map(function(m) {
    return new TreeNode(m);
  });

  function findNodeBySlug(slug) {
    var result = nodes.filter(function(node) {
      return node.slug === slug;
    });
    return result[0];
  }

  function removeUnresolvedDependencies() {
    nodes.forEach(function(node) {
      node.dependencies.forEach(function(dependency) {
        if(!findNodeBySlug(dependency)) {
          var index = node.dependencies.indexOf(dependency);
          node.dependencies.splice(index, 1);
        }
      });
    });
  }

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
  }

  function findChildren(parent) {
    parent.children = nodes.filter(function(node) {
      if(node.dependencies.indexOf(parent.slug) !== -1) {
        return findChildren(node);
      }
    });
    return parent;
  }

  function buildGraph(nodes, accumulator) {
    accumulator = accumulator || {nodes: [], links: []};

    nodes.forEach(function(node) {
      if(node.inTree === false) {
        accumulator.nodes.push({
          title: node.title,
          slug: node.slug,
          root: (node.dependencies.length === 0),
          url: githubUrl + "/" + node.organisation + "/" + node.repository + "/issues?milestone=" + node.number,
          repoUrl: githubUrl + "/" + node.organisation + "/" + node.repository,
          editLink: githubUrl + "/" + node.organisation + "/" + node.repository
                                                + "/milestones/" + node.number + "/edit",
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
      return buildGraph(nodes);
    }
  };
};

function setGraphDimension() {
  var svg = d3.select("svg");
  svg.attr({
    "width": "100%",
    "height": Math.max(svg.node().getBBox().height + 100, window.innerHeight - 80)
  });
}

var renderer = function(graphData, githubHostname) {
  var graph = new dagreD3.Digraph(),
    svgGroup = d3.select("svg g");

  function addNodes() {
    graphData.nodes.forEach(function(node) {
      graph.addNode(node.slug, { label: nodeHtml(node) });
    });
  }

  function nodeHtml(node) {
    var totalIssues = parseInt(node.openIssues)+parseInt(node.closedIssues);
    var properties = { class: "milestone " + (node.assignees.length > 0 ? "has-assignees" : "")};
    if(node.progressPercentage > 0) {
      properties.css = {"background": "linear-gradient(to right, rgba(30,255,0, .4)" + node.progressPercentage + "%, white 0%)"};
    }
    var outer = $("<div/>", properties);
    $("<a/>", {
      href: node.url,
      html: node.title
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
            href: "https://" + githubHostname + "/" + assignee.login,
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
          .nodeSep(50)
          .rankDir("LR");
      addNodes();
      addLinks();
      graphRenderer.layout(layout).run(graph, svgGroup);
      setGraphDimension();
    }
  }
};

var load = function(githubHostname) {
  $("#milestones-spinner").show();
  $("svg").hide();
  $.getJSON("/milestones/").done(function(data) {
    $("svg").show();
    var graphData = tree(data, githubHostname).toGraph();
    renderer(graphData, githubHostname).render();
    d3.selectAll(".milestone a").on("mousedown", function(){
      d3.event.stopPropagation();
    });

    // Add zoom behavior: see https://github.com/andyperlitch/dagre-d3/blob/issue%2315/demo/interactive-demo.html#L173-L192
    var zoom = d3.behavior.zoom();
    var svg = d3.select("svg");
    var lastRegistered = {
      translate: zoom.translate(),
      scale: zoom.scale()
    };
    zoom.on("zoom", function() {
      var ev = d3.event;
      if (!ev.sourceEvent.altKey && ev.sourceEvent.type === "wheel") {
        var sev = ev.sourceEvent;
        window.scrollBy(0, sev.deltaY);
        zoom.translate(lastRegistered.translate);
        zoom.scale(lastRegistered.scale);
      } else {
        lastRegistered.translate = ev.translate;
        lastRegistered.scale = ev.scale;
        svg.select("g")
            .attr("transform", "translate(" + ev.translate + ") scale(" + ev.scale + ")");
      }
    });
    d3.select("svg").call(zoom);

    $("#milestones-spinner").hide();
  }).fail(function() {
    alert("There was a problem with the request.");
  });
};

function initApp(githubHostname) {
  $(document).ready(function() {
    load(githubHostname);
  });
}

window.onresize = setGraphDimension;
