#!/bin/bash

# A script for updating common files for projects (e.g. inspection profiles).
#
# Updates the files by the specified relative paths (paths to files from a project root)
# from the file repository. If the files are outdated,
# the new versions will be pushed to the newly created branch.
#
# The syntax is: `update-files.bat first_path second_path ...`
#
# E.g. you want to update the file `foo/bar/file.txt`, then this file
# will be replaced by the file from the repository with URL `repository_url/foo/bar/file.txt`.

FILE_REPOSITORY="https://raw.githubusercontent.com/SpineEventEngine/core-java/master"

git stash && echo "Your changes were stashed."

# Overwrite the specified files.
cd ../
for file_path in "$@"
do
    echo "Downloading $FILE_REPOSITORY/$file_path"
    curl "$FILE_REPOSITORY/$file_path" > "$file_path"
    if [ $? != 0 ]; then
        echo "An error occurred during the downloading."
        # Undo the changes caused by the downloading.
        git stash && git stash pop
        # Undo the stashing of the initial changes.
        git stash apply
        exit 1
    fi
done

git diff --quiet HEAD --
if [ $? ]; then
    echo "The specified files are outdated."

    # Push the changes to the new branch.
    BRANCH_NAME="update-outdated-files"
    git checkout -B "$BRANCH_NAME"
    git commit -am "Update the outdated files"
    git push -u origin "$BRANCH_NAME"

    # Checkout the previous branch.
    git checkout -
else
    echo "The specified files are up to date."
fi

# Undo the stashing of the initial changes.
git stash apply
