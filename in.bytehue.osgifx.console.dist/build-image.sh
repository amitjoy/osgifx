#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

function printUsage() {
    echo "OSGi.fx Image Preparation"
    echo ""
    echo "Usage:"
    echo "    $0 <platform>"
    echo "Examples:"
    echo ""
    echo "    Build image for MAC:"
    echo "        $0 mac"
    echo "    Build image for Linux:"
    echo "       $0 linux"
    echo "    Build image for Windows:"
    echo "       $0 windows"
    echo ""
    exit 0
}

function command_exists() {
    type "$1" &> /dev/null ;
}

 # Parsing Platform Specific Options
case $1 in
'mac')
    platform=mac
    icon=icons/macosx/OSGi.fx.icns
    output=OSGi.fx.app
 ;;
'linux')
    platform=linux64
    icon=icons/linux/OSGi.fx.icns
    output=OSGi.fx-linux
 ;;
'windows')
    platform=windows64
    icon=icons/macosx/OSGi.fx.icns
    output=OSGi.fx-windows
 ;;
*)
    printUsage
 ;;
esac

# Distribution Image Preparation
java -jar $DIR/packr-all-4.0.0.jar \
     --verbose \
     --platform $platform \
     --jdk $JAVA_HOME \
     --executable OSGi.fx \
     --classpath $DIR/osgifx.jar \
     --mainclass aQute.launcher.pre.EmbeddedLauncher\
     --vmargs Xmx1G \
     --icon $icon \
     --bundle in.bytehue.osgifx \
     --output $output

if command_exists zip ; then
    # Distribution Image Compression
	zip -r $DIR/$output.zip $output
fi