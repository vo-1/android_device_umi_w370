# Copy this file to wherever your store your update archive files and execute it there.
#!/bin/bash
if [ -z "$1" ]
    then
        echo "ERROR: Usage: $0 <update_file.zip>"
        exit 1
fi
if ! [ -f "$1" ]
    then
        echo "ERROR: File not found"
        exit 2
fi
unzip -p $1 system/build.prop >build.prop
if [ $(stat -c %s build.prop) == "0" ]
    then
        echo "ERROR: build.prop missing in patch archive"
        rm build.props
        exit 3
fi
read -p "Update name (displayed to user): " name
read -p "Download link: " link
read -p "Add changelog entry (Keep empty to finish): " changelog
changelog="      \"$changelog\""
while [ true ]; do
    read -p "Add changelog entry (Keep empty to finish): " input
    if [ -z "$input" ]
        then
            break
    fi
    changelog="$changelog,\n      \"$input\""
done
echo "  {"
echo "    \"name\": \"$name\","
echo "    \"filename\": \"$1\","
echo "    \"md5\": \"$(md5sum $1 | awk '{ print $1 }')\","
echo "    \"size\": $(stat -c %s $1),"
echo "    \"builddate\": $(cat build.prop | grep ro.build.date.utc= | cut -d '=' -f 2),"
echo "    \"releasedate\": $(stat -c %Y $1)000,"
echo "    \"device\": \"$(cat build.prop | grep ro.product.device= | cut -d '=' -f 2)\","
echo "    \"url\": \"$link\","
printf "    \"patchlevel\": "
printf "$(cat build.prop | grep ro.build.patchlevel= | cut -d '=' -f 2)"
if [ -z $(cat build.prop | grep ro.build.patchlevel= | cut -d '=' -f 2) ]
    then
        printf "0"
fi
printf ",\n"
printf "    \"changelog\": [\n$changelog\n    ]\n"
echo "  }"
