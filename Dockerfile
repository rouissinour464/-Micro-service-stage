# Étape 1 : construire l'application Spring Boot avec Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Définir le dossier de travail
WORKDIR /app

# Copier le fichier pom.xml (gestion des dépendances)
COPY pom.xml .

# Copier le code source du projet
COPY src src

# Compiler le projet et générer le fichier JAR
# L'option -DskipTests permet de ne pas exécuter les tests
RUN mvn clean package -DskipTests


# Étape 2 : créer une image finale plus légère pour l'exécution
FROM eclipse-temurin:21-jre

# Définir le dossier de travail
WORKDIR /app

# Copier uniquement le fichier JAR depuis l'étape précédente
COPY --from=build /app/target/*.jar app.jar

# Exposer le port utilisé par le microservice
EXPOSE 8082

# Commande pour démarrer l'application Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]