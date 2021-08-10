#!/bin/bash

read -rp "Version: " version
if [ "$version" == "" ]; then
    exit 0
fi
read -rp "Name: " name
if [ "$name" == "" ]; then
    exit 0
fi
read -rp "Release Title: " title
if [ "$title" == "" ]; then
    exit 0
fi
read -rp "Cyan version: " versionCyan
if [ "$versionCyan" == "" ]; then
    exit 0
fi
read -rp "ModKit version: " modkit
if [ "$modkit" == "" ]; then
    exit 0
fi
read -rp "ProtocolHooks version: " phversion
if [ "$phversion" == "" ]; then
    exit 0
fi
read -rp "Supported game versions (speparate by comma's): " gameVersions
if [ "$gameVersions" == "" ]; then
    exit 0
fi
IFS=', ' read -ra gameVersions <<< "$gameVersions"

read -rp "Pre-release? [Y/n] " prerelease
if [ "$prerelease" == "Y" ] || [ "$prerelease" == "y" ]; then
    isPrerelease=true
else
    isPrerelease=false
fi

versionstr=""
for versionNm in "${gameVersions[@]}"; do
    versionstr+="\n- Minecraft $versionNm"
done

json='{
    "tag_name": "'"${version}"'",
    "name": "'"${name}"'",
    "body": "'"${title}"'\n\nSupported game versions:'"$versionstr"'\n\nDependencies:\n- [ProtocolHooks](https://github.com/Stefan0436/ProtocolHooks/releases/'"$phversion"') '"$phversion"'\n- [Cyan](https://aerialworks.ddns.net/cyan) '"$versionCyan"'\n- [ModKit](https://aerialworks.ddns.net/maven/org/asf/cyan/ModKit) '"$modkit"' (part of Cyan)",
    "prerelease": '"$isPrerelease"'
}'

echo
echo
echo "Tag: $version"
echo "Name: $name"
echo
echo "$json" | jq .body -r
read -rp "Is this right? [Y/n] " prerelease
if [ "$prerelease" == "Y" ] || [ "$prerelease" == "y" ]; then
    echo Building all CMF files...
    rm -rf build/cmf
    rm -rf build/libs
    
    chmod +x gradlew
    read -rsp "GitHub Token: " token
    echo
    read -rp "Maven Username: " mavenuser
    read -rsp "Maven Password: " mavenpassword
    echo
    if [ "$mavenuser" == "" ] || [ "$mavenpassword" == "" ]  || [ "$token" == "" ]; then
        exit
    fi
    for versionNm in "${gameVersions[@]}"; do
        ./gradlew publish -PoverrideGameVersion="$versionNm" -Pmavenusername="$mavenuser" -Pmavenpassword="$mavenpassword"
    done
    
    echo Creating git tag...
    if [ "$(git status --porcelain)" == "" ]; then
        git add -A
        git commit -m "[Tag release] $name"
    fi
    
    git tag -a "$version" -m "$name"
    git push
    git push --tags
    
    while : ; do
        echo Publishing github release....
        response="$(echo "$json" | curl -s https://api.github.com/repos/SkySwimmer/MC2FA/releases -H "Accept: application/vnd.github.v3+json" --data-binary @- -X POST -H "Authorization: token $token")"
        id="$(echo "$response" | jq .id -r)"
        if [ "$id" == "null" ]; then
            1>&2 echo Failure!
            read -rsp "GitHub Token: " token
            echo            
        else
            break
        fi
    done
    
    echo Publishing files...
    for file in build/cmf/*.cmf ; do
       curl -s "https://uploads.github.com/repos/SkySwimmer/MC2FA/releases/$id/assets?name=$(basename "$file")" -X POST -H "Content-Type: application/octet-stream" --data-binary "@$file" -H "Authorization: token $token" > /dev/null
    done
    for file in build/libs/*.jar ; do
       curl -s "https://uploads.github.com/repos/SkySwimmer/MC2FA/releases/$id/assets?name=$(basename "$file")" -X POST -H "Content-Type: application/octet-stream" --data-binary "@$file" -H "Authorization: token $token" > /dev/null
    done
    
    echo Done.
fi
