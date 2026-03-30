# 🌳 Tree Mapper

Application Android pour repérer et cartographier les arbres et plantes sur votre terrain au **465 Route des Hubacs, 26160 Le Poet Laval**.

---

## Fonctionnalités

- 🗺️ **Carte satellite Google Maps** centrée sur votre terrain (coordonnées GPS : 44.5283°N, 5.0167°E)
- 📍 **Géolocalisation GPS** en temps réel (point bleu sur la carte)
- 🌳 **Bouton "Plante repérée"** : ajoute un marqueur à votre position actuelle
- 📋 **Liste des arbres** : visualisez tous les arbres repérés avec leurs coordonnées GPS
- 🗑️ **Suppression** : appuyez sur un marqueur pour l'afficher ou le supprimer
- 💾 **Sauvegarde locale** : les marqueurs persistent après fermeture de l'app (SharedPreferences)
- 📸 **Export** : capture d'écran de la carte avec tous les marqueurs sauvegardée dans la galerie

---

## 1. Obtenir une clé API Google Maps

1. Rendez-vous sur [Google Cloud Console](https://console.cloud.google.com/)
2. Créez un nouveau projet ou sélectionnez un projet existant
3. Activez l'API **Maps SDK for Android** :
   - Menu → API et services → Bibliothèque
   - Recherchez "Maps SDK for Android" → Activer
4. Créez une clé API :
   - Menu → API et services → Identifiants → Créer des identifiants → Clé API
   - Restreignez la clé à l'application Android avec votre `applicationId` (`com.treemapper`)
5. Copiez la clé API générée

---

## 2. Configurer la clé dans le projet

Ouvrez le fichier `local.properties` à la racine du projet et remplacez la valeur :

```properties
sdk.dir=/chemin/vers/votre/android/sdk
MAPS_API_KEY=VOTRE_CLE_API_ICI
```

> ⚠️ Ne commitez jamais votre vraie clé API dans Git !

---

## 3. Compiler et installer l'application

### Prérequis
- Android Studio Hedgehog (2023.1.1) ou plus récent
- Android SDK API 24 minimum
- Un appareil Android ou émulateur

### Étapes

1. **Clonez le dépôt** :
   ```bash
   git clone https://github.com/tacojazz-svg/tree-mapper.git
   cd tree-mapper
   ```

2. **Configurez la clé API** dans `local.properties` (voir étape 2)

3. **Ouvrez dans Android Studio** :
   - File → Open → sélectionnez le dossier `tree-mapper`

4. **Compilez et lancez** :
   - Connectez votre téléphone Android (activez le débogage USB)
   - Cliquez sur ▶️ Run dans Android Studio
   - Ou en ligne de commande :
     ```bash
     ./gradlew assembleDebug
     adb install app/build/outputs/apk/debug/app-debug.apk
     ```

---

## 4. Utilisation de l'application

1. **Lancez l'app** → acceptez les permissions GPS et stockage
2. La carte satellite s'ouvre centrée sur **465 Route des Hubacs**
3. **Naviguez** sur le terrain avec pinch-to-zoom et glisser
4. **Repérez une plante** :
   - Approchez-vous de la plante physiquement
   - Appuyez sur 🌳 **Plante repérée** quand vous êtes à côté
   - Un marqueur vert numéroté apparaît sur la carte
5. **Gérer les marqueurs** :
   - Appuyez sur un marqueur pour voir ses coordonnées GPS ou le supprimer
   - Appuyez sur 📋 **Liste** pour voir tous les arbres repérés
6. **Exporter la carte** :
   - Appuyez sur 📸 **Exporter** pour sauvegarder une capture d'écran dans votre galerie (dossier Pictures/TreeMapper)

---

## Structure du projet

```
tree-mapper/
├── app/
│   ├── src/main/
│   │   ├── java/com/treemapper/
│   │   │   ├── MainActivity.kt       # Activité principale
│   │   │   ├── TreeMarker.kt         # Data class pour les marqueurs
│   │   │   └── MarkerStorage.kt      # Sauvegarde SharedPreferences
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   └── values/
│   │   │       ├── strings.xml
│   │   │       ├── colors.xml
│   │   │       └── themes.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── local.properties                  # Clé API (à configurer localement)
└── README.md
```

---

## Permissions requises

| Permission | Usage |
|-----------|-------|
| `ACCESS_FINE_LOCATION` | GPS précis pour la localisation |
| `ACCESS_COARSE_LOCATION` | Localisation réseau (secours) |
| `INTERNET` | Chargement des tuiles Google Maps |
| `WRITE_EXTERNAL_STORAGE` | Sauvegarde de l'export (Android ≤ 9) |

---

## Coordonnées GPS du terrain

- **Adresse** : 465 Route des Hubacs, 26160 Le Poet Laval, France
- **Latitude** : 44.5283
- **Longitude** : 5.0167