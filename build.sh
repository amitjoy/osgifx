./gradlew clean build --max-workers=5 --parallel
./gradlew :com.osgifx.console.product:export.osgifx
cp com.osgifx.console.product/target/distributions/executable/osgifx.jar com.osgifx.console.dist/uberjar
./gradlew jpackage --info