# Spring Cloud Deployer for Kubernetes Sidecar Example

This deploys a single pod with two containers which analyzes tweet sentiments. The main container is
[sentiment-demo](https://github.com/dturanski/sentiment-demo) and the sidecar is [sentiment-analyzer](https://github
.com/dturanski/sentiment-analyzer), 
both available on [dockerhub](https://hub.docker.com/u/dturanski/). 

Once deployed to K8s, you can test it using the instructions [here](https://github.com/dturanski/sentiment-demo)