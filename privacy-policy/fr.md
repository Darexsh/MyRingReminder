# Politique de confidentialité – MyRingReminder

Last updated: 2026-11-05

## 1. Responsable du traitement
Daniel Sichler (Darexsh)  
E-mail : sichler.daniel@gmail.com

## 2. Champ d’application
Cette politique s’applique à l’application Android MyRingReminder (`com.darexsh.myringreminder`), distribuée par l’intermédiaire de ce dépôt et sous forme de versions APK.

## 3. Données traitées par MyRingReminder
MyRingReminder traite les données nécessaires au suivi du cycle et à la personnalisation, notamment :
- les dates et l’historique des cycles
- les entrées de suivi des règles (par exemple, l’indication d’un jour de règles, l’intensité des saignements, des symptômes tels que la douleur ou la maladie, ainsi que des marqueurs facultatifs de début et de fin)
- les paramètres de rappel et les horaires de notification
- les notes saisies par l’utilisateur ou l’utilisatrice
- les préférences de conception (couleurs, arrière-plan et langue)
- les paramètres facultatifs de verrouillage de l’application

## 4. Stockage local des données
MyRingReminder stocke vos données localement sur votre appareil (par exemple dans SharedPreferences ou dans le stockage local de l’application).

MyRingReminder ne transmet pas au développeur vos données personnelles relatives au cycle ni vos notes.

Si vous utilisez la sauvegarde ou la restauration, les données de sauvegarde sont exportées ou importées sous forme de fichier local que vous sélectionnez à l’aide du sélecteur de fichiers Android.

Lors de l’exportation des détails des règles au format PDF, le PDF est généré localement sur votre appareil et enregistré uniquement à l’emplacement que vous choisissez dans le sélecteur de fichiers Android.  
Lorsque vous ouvrez ce PDF depuis une notification de l’application, Android peut transmettre le fichier à l’application de lecture PDF que vous avez choisie. Dans ce cas, cette application traite le fichier conformément à sa propre politique de confidentialité.

## 5. Autorisations utilisées
MyRingReminder peut demander les autorisations nécessaires à ses fonctionnalités, telles que :
- les notifications et les alarmes exactes (rappels liés au cycle)
- l’accès aux fichiers ou aux médias (sélection d’une image d’arrière-plan, sauvegarde et restauration)
- les données biométriques ou les identifiants de l’appareil (verrouillage facultatif de l’application)

Les autorisations sont utilisées uniquement pour fournir les fonctionnalités de l’application que vous demandez.

## 6. Analyse, suivi et rapports d’erreurs
D’après l’implémentation actuelle, MyRingReminder n’intègre **aucun** SDK tiers d’analyse ni de signalement des plantages.

## 7. Réseau et liens externes
Les fonctions principales de MyRingReminder sont locales et ne nécessitent pas d’API backend dédiée pour le suivi du cycle.

Pour vérifier les mises à jour, MyRingReminder peut demander les métadonnées des versions à l’API publique GitHub Releases de ce projet.

Lorsque vous ouvrez **Paramètres → Notice du médicament**, l’application demande le fichier public `leaflet-packages.json` du dépôt GitHub de ce projet. Lorsque vous ouvrez **Informations sur l’application → Politique de confidentialité**, elle demande le fichier correspondant à la langue sélectionnée de l’application ; si aucune traduction n’est disponible, la version anglaise est utilisée. Ces requêtes ne contiennent pas vos données de cycle, vos notes ni les paramètres de l’application. GitHub peut traiter des informations de connexion ordinaires, telles que votre adresse IP et les en-têtes de requête, conformément à sa politique de confidentialité. L’application conserve sur votre appareil la dernière copie récupérée avec succès pour chaque langue afin que vous puissiez la consulter si une requête ultérieure échoue.

À votre demande, MyRingReminder peut ouvrir des destinations externes à l’aide d’intents système, notamment :
- les e-mails (`mailto:`)
- Linktree
- Telegram
- GitHub
- Buy Me a Coffee
- les notices hébergées par des services externes d’information sur les médicaments ou par des fabricants

Lorsque vous ouvrez une notice, Android transmet son URL au navigateur ou au lecteur de documents de votre choix. Le service qui héberge le document reçoit la requête ; l’URL peut révéler le produit et la langue de la notice. Le navigateur, le lecteur et l’hébergeur de la notice peuvent traiter des informations de connexion ordinaires conformément à leurs propres politiques de confidentialité. MyRingReminder n’ajoute pas à ces requêtes les données de cycle, les notes ni les paramètres stockés localement.

Lorsque vous ouvrez des liens externes, les politiques de confidentialité des services tiers concernés s’appliquent.

### Services tiers mentionnés dans les liens
- GitHub : https://docs.github.com/en/site-policy/privacy-policies/github-privacy-statement
- Telegram : https://telegram.org/privacy
- Linktree : https://linktr.ee/s/privacy
- Buy Me a Coffee : https://www.buymeacoffee.com/privacy-policy

## 8. Partage des données
D’après l’implémentation actuelle de l’application, MyRingReminder ne transmet pas au développeur les données de cycle, les notes ni les paramètres stockés localement.

## 9. Avertissement médical
MyRingReminder est uniquement un outil d’organisation et de suivi.

Il ne s’agit pas d’un dispositif médical ; l’application ne fournit pas de diagnostic médical et ne remplace pas les conseils d’un professionnel de santé.

En cas de question médicale ou de problème de santé, consultez un professionnel de santé qualifié.

## 10. Vos choix
- Vous pouvez modifier ou supprimer vos données dans l’application.
- Vous pouvez révoquer les autorisations de l’application dans les paramètres Android.
- Vous pouvez choisir de ne pas ouvrir les liens externes.
- Vous pouvez désinstaller l’application et effacer les données locales dans les paramètres Android.

## 11. Modifications de cette politique
Cette politique peut être mise à jour en cas de changement des fonctionnalités de l’application ou des exigences légales.
