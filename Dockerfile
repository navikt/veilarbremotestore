FROM docker.pkg.github.com/navikt/pus-nais-java-app/pus-nais-java-app:java11

ADD /target/veilarbremotestore-1-jar-with-dependencies.jar app.jar
