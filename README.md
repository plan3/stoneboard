# Stoneboard - A GitHub milestone visualizer

It iterates through all projects a user has access to, in the configured organisations, and fetches milestones and issues. These are then displayed in a graph visualising the milestone relationships.

Specify milestones dependencies by adding this to a milestone description. For example:

`[blah/gh-dashboard-test-three/3 blah/gh-dashboard-test-three/2]`

This tells us the milestone has two dependencies; milestone 2 and 3 in the repo “gh-dashboard-test-three” in the organisation “blah”.

![example](stoneboard-example.png)

## F#!k it, :squirrel: it

<a href="https://heroku.com/deploy"><img src="https://www.herokucdn.com/deploy/button.svg" alt="Deploy to Heroku"></a>
<br/>
<a href="https://deploy.cloud.run"><img src="https://deploy.cloud.run/button.svg" width="20%" height="auto" alt="Deploy to Google Cloud Run"></a>

Docker images are built from `master` and pushed to [Github Packages](https://github.com/plan3/stoneboard/packages/189650) and [Docker Hub](https://hub.docker.com/r/martengustafson/stoneboard).

### Demo

There are two demo instances available, one on [Heroku](https://stoneboard.herokuapp.com/) and one on [Google Cloud Run](https://stoneboard-jh667k4lfa-lz.a.run.app/).
Both are configured to render the milestones and issues from this repository.

# About

## Rationale

The underlying reasoning behind this style of task visualisation and planning is partly described in the presentation _"[Bastardised Kanban](https://speakerdeck.com/chids/bastardised-kanban)"_ from 2015.

## Local development

### Prerequisites

You will need Java 8 and Maven 3 installed and "properly" setup (in your path etc).

### Setup

Create a file called `.env` with the variables listed beneath _Configuration_ below.

You can set up a GitHub app for local dev on [https://github.com/settings/applications](https://github.com/settings/applications),
the callback will be `http://127.0.0.1:8080/login/oauth2/code/github`.

### Start

Assuming that you have the Heroku CLI installed, start the app with:

    $ mvn clean package && heroku local:start

## Deploy

It's built to run on Heroku with the following configuration:

### Configuration

Config variables for local `.env` and Heroku app:

```
GITHUB_CLIENT_ID=A-GITHUB-CLIENT-ID
GITHUB_CLIENT_SECRET=A-GITHUB-CLIENT-SECRET
GITHUB_CLIENT_SCOPES=user # set this to user,repo if you need to access private repositories
GITHUB_HOSTNAME=github.com (optional, used for GitHub Enterprise custom domains)
REPOSITORIES=org/repo,org/repo...
```
