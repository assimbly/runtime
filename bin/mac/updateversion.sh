if [ -n "$1" ]; then
    printf "\nupdate version to: $1\n"
    mvn -f ../../pom.xml versions:set -DgenerateBackupPoms=false -DnewVersion="$1"
    mvn -f ../../pom.xml versions:set-property -Dproperty=assimbly.version -DnewVersion="$1"
else
    printf "\nUsage:\n"
    printf "\nupdateversion.sh <versionnumber>\n"
fi