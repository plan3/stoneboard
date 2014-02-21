# Milestones Visualizer

Proof of concept for a GitHub milestone visualizer. 

It iterates through all projects a user has access to, in the configured organisations, and fetches milestones and issues. These are then displayed in a graph visualising the milestone relationships.

Specify milestones dependencies by adding this to a milestone description. For example:

`[plan3/gh-dashboard-test-three/3 plan3/gh-dashboard-test-three/2]`

This tells us the milestone has two dependencies; milestone 2 and 3 in the repo “gh-dashboard-test-three” in the organisation “plan3”.

## Local development

### Setup

Create a file called .env with the variables listed beneath _Configuration_ below.

You can set up a GitHub app for local dev on [https://github.com/settings/applications](https://github.com/settings/applications).

### Start

Start the app with:
    
    $ mvn clean package && foreman start

## Deploy

It's built to run on Heroku with the following configuration:
    
### Configuration

Config variables for local .env and Heroku app:

```
GITHUB_CLIENT_ID=A-GITHUB-CLIENT-ID
GITHUB_CLIENT_SECRET=A-GITHUB-CLIENT-SECRET
CALLBACK_URL=https://planning.plan3dev.se/auth/callback (locally: http://127.0.0.1:5000/auth/callback)
REPOSITORIES=org/repo,org/repo...
```
