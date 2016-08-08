#!/bin/bash

set -e # Exit with nonzero exit code if anything fails

SOURCE_BRANCH="master"
TARGET_BRANCH="gh-pages"

function doCompile {
  GRADLE_OPTS='-Xmx600m -Dorg.gradle.jvmargs="-Xmx1500m"' ./gradlew release
}

# Pull requests and commits to other branches shouldn't try to deploy, just build to verify
#if [ "$TRAVIS_PULL_REQUEST" != "false" -o "$TRAVIS_BRANCH" != "$SOURCE_BRANCH" ]; then
#    echo "Skipping deploy; just doing a build."
#    doCompile
#    exit 0
#fi

# Save some useful information
REPO=`git config remote.origin.url`
SSH_REPO=git@github.com:andreas-eberle/settlers-remake.git
SHA=`git rev-parse --verify HEAD`



# Get the deploy key by using Travis's stored variables to decrypt deploy_key.enc
openssl aes-256-cbc -K $encrypted_af9c5a2dd85c_key -iv $encrypted_af9c5a2dd85c_iv -in deploy_key.enc -out deploy_key -d 
chmod 600 deploy_key
eval `ssh-agent -s`
ssh-add deploy_key




# Clone the existing gh-pages for this repo into out/
# Create a new empty branch if gh-pages doesn't exist yet (should only happen on first deply)

rm -rf gh-pages

git clone $SSH_REPO --branch $TARGET_BRANCH --single-branch gh-pages



# Clean out existing contents
rm -rf gh-pages/**/* || exit 0

# Run our compile script
doCompile


# Create the folder name
if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then 
	FOLDER="pullRequests/$TRAVIS_PULL_REQUEST"

elif [ -n "$TRAVIS_TAG" ]; then
	FOLDER="tags/$TRAVIS_TAG"

else
	FOLDER="branch/$TRAVIS_BRANCH"
	
fi

DATE=`date +%Y-%m-%d_%H-%M-%S`
FOLDER="gh-pages/$FOLDER/"
mkdir -v -p "$FOLDER"


# Copy the release files into the folder
cp -R release/ "$FOLDER/${DATE}_$SHA/"

cd gh-pages


# Set git config
git config user.name "Travis CI"
git config user.email "travis@settlers-remake"


# Commit the "changes", i.e. the new version.
# The delta will show diffs between new and old versions.
git add .
git commit -m "Deploy to GitHub Pages: ${SHA}"


# Now that we're all set up, we can push.
git push $SSH_REPO $TARGET_BRANCH