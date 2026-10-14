# CampingCampagnes
CampingCampagnes est un outil de gestion pour les campings. Il permet de gérer les comtpes du personnel interne, la création d'animations ainsi que l'envoi par email du planning d'un animateur.

# Prérequis
- Maven : `sudo apt install maven`
- JDK 25 ou plus : `sudo apt install openjdk-25-jdk`
- Service de base de données MySQL ou MariaDB

# Installation
- Clonez le projet : `git clone https://github.com/AntE204/CampingCampagnes.git`
- Rentrez dans le dossier : `cd CampingCampagnes`
- Dans MySQL, créez une base de données nommée `campingcampagnes`
- Dans cette base, importez le fichier `campingcampagnes.sql` pour créer les tables (vides) de la base
- Lancez l'application avec `./run.sh`
> [!CAUTION]
> L'application risque de ne pas fonctionner correctement si elle est lancée depuis un IDE comme IntelliJ.
> Vous devez toujours l'exécuter à l'aide du script run.sh.

# Utilisation
## Générale
- La navigation dans l'application se fait avec les flèches du clavier.
- Appuyez sur la touche Entrer pour confirmer votre choix dans les menus.
## Technique
- Lors de la première utilisation, l'application vous demandra de créer le compte du directeur.
- Le directeur possède tous les droits : création, modification, suppression de comptes, création d'animation, attribution des animateurs, envoi des plannings.
- Les annimateurs eux possèdent des droits restreints : modifier leur compte et recevoir leur planning.

# Images
TODO